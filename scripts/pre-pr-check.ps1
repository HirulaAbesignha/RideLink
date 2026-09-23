[CmdletBinding()]
param(
    [switch]$Ci,
    [switch]$SkipTests,
    [string]$BaseRef = "origin/develop"
)

$ErrorActionPreference = "Stop"
$script:HasFailure = $false

function Write-Pass([string]$Message) {
    Write-Host "[PASS] $Message" -ForegroundColor Green
}

function Write-Fail([string]$Message) {
    Write-Host "[FAIL] $Message" -ForegroundColor Red
    $script:HasFailure = $true
}

function Write-Info([string]$Message) {
    Write-Host "[INFO] $Message" -ForegroundColor Cyan
}

$repoRoot = (& git rev-parse --show-toplevel 2>$null).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($repoRoot)) {
    throw "Run this checker from inside the RideLink Git repository."
}

Push-Location $repoRoot
try {
    Write-Host "RideLink pre-PR checker" -ForegroundColor White
    Write-Host "Repository: $repoRoot"
    Write-Host "Base: $BaseRef"

    if (-not $Ci) {
        $branch = (& git branch --show-current).Trim()
        if ($branch -in @("main", "develop") -or [string]::IsNullOrWhiteSpace($branch)) {
            Write-Fail "Create a feature/fix/docs branch before requesting a PR. Current branch: '$branch'."
        }
        else {
            Write-Pass "Current branch is '$branch'."
        }

        $workingTree = @(& git status --porcelain)
        if ($workingTree.Count -gt 0) {
            Write-Fail "Working tree is not clean. Commit or intentionally restore all changes first."
            $workingTree | ForEach-Object { Write-Host "       $_" }
        }
        else {
            Write-Pass "Working tree is clean."
        }

        Write-Info "Fetching the latest develop branch."
        & git fetch origin develop --quiet
        if ($LASTEXITCODE -ne 0) {
            Write-Fail "Could not fetch origin/develop. Check internet access and repository permissions."
        }
    }

    & git rev-parse --verify $BaseRef *> $null
    if ($LASTEXITCODE -ne 0) {
        Write-Fail "Base reference '$BaseRef' does not exist locally."
        $changedFiles = @()
    }
    else {
        if (-not $Ci) {
            & git merge-base --is-ancestor $BaseRef HEAD
            if ($LASTEXITCODE -ne 0) {
                Write-Fail "Your branch is behind $BaseRef. Merge the latest develop branch before opening the PR."
            }
            else {
                Write-Pass "Branch contains the latest $BaseRef."
            }
        }

        $changedFiles = @(& git diff --name-only --diff-filter=ACMR "$BaseRef...HEAD")
        if ($changedFiles.Count -eq 0) {
            Write-Fail "No committed changes were found compared with $BaseRef."
        }
        else {
            Write-Pass "Found $($changedFiles.Count) changed file(s) for this PR."
        }

        $whitespaceProblems = @(& git diff --check "$BaseRef...HEAD")
        if ($whitespaceProblems.Count -gt 0) {
            Write-Fail "Git found whitespace errors."
            $whitespaceProblems | ForEach-Object { Write-Host "       $_" }
        }
        else {
            Write-Pass "No whitespace errors found."
        }
    }

    $trackedFiles = @(& git ls-files)
    $forbiddenFiles = @()
    foreach ($file in $trackedFiles) {
        $normalized = $file.Replace("\", "/")
        $leaf = [System.IO.Path]::GetFileName($normalized)
        if (
            $normalized -match '(^|/)(target|node_modules|\.idea|\.vscode)/' -or
            $normalized -match '\.(class|jar|war|log)$' -or
            (($leaf -eq '.env' -or $leaf -like '.env.*') -and $leaf -ne '.env.example')
        ) {
            $forbiddenFiles += $file
        }
    }

    if ($forbiddenFiles.Count -gt 0) {
        Write-Fail "Generated, local or secret file names are tracked."
        $forbiddenFiles | ForEach-Object { Write-Host "       $_" }
    }
    else {
        Write-Pass "No forbidden generated/local files are tracked."
    }

    $largeFiles = @()
    foreach ($file in $trackedFiles) {
        $path = Join-Path $repoRoot $file
        if (Test-Path -LiteralPath $path -PathType Leaf) {
            $item = Get-Item -LiteralPath $path
            if ($item.Length -gt 5MB) {
                $largeFiles += "$file ($([math]::Round($item.Length / 1MB, 2)) MB)"
            }
        }
    }

    if ($largeFiles.Count -gt 0) {
        Write-Fail "Tracked files larger than 5 MB require group-leader review."
        $largeFiles | ForEach-Object { Write-Host "       $_" }
    }
    else {
        Write-Pass "No unexpectedly large tracked files found."
    }

    $conflictMarkers = @(& git grep -n -E '^(<<<<<<< |=======|>>>>>>> )' -- . 2>$null)
    if ($conflictMarkers.Count -gt 0) {
        Write-Fail "Unresolved merge-conflict markers found."
        $conflictMarkers | ForEach-Object { Write-Host "       $_" }
    }
    else {
        Write-Pass "No merge-conflict markers found."
    }

    $secretPatterns = @(
        '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----',
        '\bghp_[A-Za-z0-9]{20,}\b',
        '\bgithub_pat_[A-Za-z0-9_]{20,}\b',
        '\bsk-[A-Za-z0-9]{20,}\b',
        '(?im)^\s*(JWT_SECRET|SERVICE_TOKEN)\s*=\s*(?!change-|example|<|\$\{)[^\s#]{16,}'
    )

    $secretFindings = @()
    foreach ($file in $changedFiles) {
        $path = Join-Path $repoRoot $file
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { continue }
        $item = Get-Item -LiteralPath $path
        if ($item.Length -gt 5MB) { continue }
        try {
            $content = Get-Content -LiteralPath $path -Raw -ErrorAction Stop
            foreach ($pattern in $secretPatterns) {
                if ($content -match $pattern) {
                    $secretFindings += "$file matched secret pattern"
                    break
                }
            }
        }
        catch {
            Write-Info "Skipped text scanning for $file."
        }
    }

    if ($secretFindings.Count -gt 0) {
        Write-Fail "Possible secret material found. Inspect and remove it before pushing."
        $secretFindings | ForEach-Object { Write-Host "       $_" }
    }
    else {
        Write-Pass "No obvious secret patterns found in changed files."
    }

    $contractPath = Join-Path $repoRoot "docs/api-contracts/API_CONTRACT.md"
    if (Test-Path -LiteralPath $contractPath) {
        $contract = Get-Content -LiteralPath $contractPath -Raw
        $jsonBlocks = [regex]::Matches($contract, '(?s)```json\r?\n(.*?)\r?\n```')
        $invalidJson = @()
        for ($index = 0; $index -lt $jsonBlocks.Count; $index++) {
            try {
                $null = $jsonBlocks[$index].Groups[1].Value | ConvertFrom-Json
            }
            catch {
                $invalidJson += "JSON example $($index + 1): $($_.Exception.Message)"
            }
        }
        if ($invalidJson.Count -gt 0) {
            Write-Fail "The API contract contains invalid JSON examples."
            $invalidJson | ForEach-Object { Write-Host "       $_" }
        }
        else {
            Write-Pass "Validated $($jsonBlocks.Count) API-contract JSON examples."
        }
    }
    else {
        Write-Fail "Required API contract is missing."
    }

    if (-not $SkipTests) {
        $services = @(
            "account-service",
            "driver-vehicle-service",
            "ride-management-service",
            "fare-payment-service"
        )
        $changedServices = @($services | Where-Object { $service = $_; $changedFiles | Where-Object { $_ -like "$service/*" } })

        if ($changedServices.Count -eq 0) {
            Write-Info "No service code changed; Maven tests are not needed for this documentation-only PR."
        }
        else {
            foreach ($service in $changedServices) {
                Write-Info "Running tests for $service."
                Push-Location (Join-Path $repoRoot $service)
                try {
                    if ($env:OS -eq "Windows_NT") {
                        & .\mvnw.cmd --batch-mode test
                    }
                    else {
                        & chmod +x ./mvnw
                        & ./mvnw --batch-mode test
                    }
                    if ($LASTEXITCODE -ne 0) {
                        Write-Fail "$service tests failed."
                    }
                    else {
                        Write-Pass "$service tests passed."
                    }
                }
                finally {
                    Pop-Location
                }
            }
        }
    }
    else {
        Write-Info "Maven tests skipped here; the separate service-test workflow runs all four services in CI."
    }

    if ($script:HasFailure) {
        Write-Host "`nPre-PR check FAILED. Fix the items above and run it again." -ForegroundColor Red
        exit 1
    }

    Write-Host "`nPre-PR check PASSED. You can push and request a pull request." -ForegroundColor Green
}
finally {
    Pop-Location
}
