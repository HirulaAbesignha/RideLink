# Contributing to RideLink

This guide is the required working process for all four members. Following it keeps the project stable and leaves clear evidence for individual marks.

## 1. Rules every member must follow

1. Use your own GitHub account. Never share accounts.
2. Configure your own Git name and an email connected to your GitHub account.
3. Work only on a personal feature branch.
4. Commit your own code using small, meaningful commits.
5. Open your own pull requests.
6. Ask another member to review each pull request.
7. Merge only after tests pass and the reviewer approves.
8. Use **Create a merge commit** on GitHub. Do not use squash merge for this assignment because the full sequence of individual commits should remain visible.
9. Never push directly to `main` or `develop`.
10. Never commit passwords, tokens, private keys, `.env` files or real personal/payment information.
11. Never query or modify another microservice's database.
12. Follow `docs/api-contracts/API_CONTRACT.md` exactly. Change the contract through a reviewed pull request before changing connected APIs.

## 2. Branch purpose

| Branch | Purpose | Who changes it |
|---|---|---|
| `main` | Integrated, demonstrable and submission-ready system | Pull requests from `develop` only |
| `develop` | Latest reviewed work from all members | Reviewed feature pull requests |
| `feature/...` | One business feature owned by one member | Feature owner |
| `fix/...` | One focused bug fix | Bug owner |
| `docs/...` | Documentation-only work | Document owner |

Do not keep a feature branch for the whole assignment. Create a new branch for each focused feature.

Recommended names:

```text
feature/account-registration
feature/account-jwt-login
feature/driver-profile-vehicle
feature/driver-availability-location
feature/driver-reservation
feature/ride-request-assignment
feature/ride-lifecycle
feature/fare-estimation
feature/payment-receipt
fix/driver-reservation-race
docs/member-contributions
```

## 3. First-time setup for every member

### 3.1 Accept the invitation

The group leader invites each member as a collaborator. Each member accepts using their own GitHub account.

### 3.2 Clone the repository

Open PowerShell in the folder where you keep university work:

```powershell
git clone https://github.com/HirulaAbesignha/RideLink.git
cd RideLink
```

Do not send project ZIP files between members. Everyone works from the same Git repository.

### 3.3 Configure personal identity in this repository

Run these commands inside the cloned `RideLink` folder:

```powershell
git config user.name "Your full name"
git config user.email "email-linked-to-your-github-account@example.com"
git config user.name
git config user.email
```

The email must be added to the member's GitHub account. A GitHub-provided `noreply` email is also valid when it belongs to that account.

Do not set another member's name or email. Do not make one member commit everybody's code.

### 3.4 Check Java and the starter project

```powershell
java -version
git status
```

Java must report version 17. The working tree should be clean.

Run the assigned service's starter test. Member 2 example:

```powershell
cd driver-vehicle-service
.\mvnw.cmd test
cd ..
```

## 4. Start every new piece of work safely

Always begin from the latest `develop`:

```powershell
git switch develop
git pull --ff-only origin develop
git switch -c feature/your-feature-name
```

Example for Member 2:

```powershell
git switch develop
git pull --ff-only origin develop
git switch -c feature/driver-profile-vehicle
```

Confirm the branch before editing:

```powershell
git branch --show-current
git status
```

If the command shows `main` or `develop`, stop and create a feature branch.

## 5. Make good commits

### 5.1 Work in your assigned area

| Member | Normal working directory |
|---|---|
| Member 1 | `account-service/` |
| Member 2 | `driver-vehicle-service/` |
| Member 3 | `ride-management-service/` |
| Member 4 | `fare-payment-service/` |

Ask the owner before changing another member's service. Shared files such as the API contract, root README, Postman collection and CI should be changed through a focused PR.

### 5.2 Inspect changes before staging

```powershell
git status
git diff
```

Check that no password, token, `.env`, IDE folder, log, `target/` folder or unrelated file appears.

### 5.3 Stage only related files

Prefer specific paths:

```powershell
git add driver-vehicle-service/src
git add driver-vehicle-service/pom.xml
git status
git diff --cached
```

Do not blindly use `git add .` when unrelated files are present.

### 5.4 Commit with a meaningful message

Format:

```text
type(service): short action
```

Examples:

```text
feat(account): add passenger registration
feat(driver): add vehicle persistence
feat(ride): enforce lifecycle transitions
feat(fare): calculate distance-based estimate
test(driver): cover duplicate vehicle registration
fix(ride): release driver after cancellation
docs(api): clarify payment error response
```

Avoid messages such as `update`, `changes`, `work`, `final` or `done`.

Create several meaningful commits as the feature progresses. Do not divide one trivial edit into fake commits, and do not wait until the deadline to make one giant commit.

## 6. Test before pushing

Run tests inside the service you changed:

```powershell
cd driver-vehicle-service
.\mvnw.cmd test
cd ..
```

Check the final difference:

```powershell
git status
git diff develop...HEAD
```

The member who opens the PR is responsible for fixing failing tests.

## 7. Push and open a pull request

Push the feature branch:

```powershell
git push -u origin feature/your-feature-name
```

GitHub normally displays a **Compare & pull request** button.

Set:

```text
Base branch: develop
Compare branch: feature/your-feature-name
```

Complete the pull request template. Include:

- member name and student ID;
- assigned service;
- what changed;
- how it was tested;
- API contract impact;
- Swagger/Postman changes;
- known limitations.

Assign a teammate as reviewer. The author cannot approve their own pull request.

## 8. Peer review process

The reviewer checks:

1. Code belongs to the correct service.
2. API paths and DTOs match the contract.
3. Another service's database is not accessed.
4. Role and ownership checks exist.
5. Validation and consistent errors exist.
6. Tests cover success and failure behaviour.
7. No secrets or generated files are committed.
8. Swagger and Postman are updated when required.

The reviewer should leave at least one useful review comment or an explicit approval explaining what was checked. Review activity is also contribution evidence.

When changes are requested, the original author makes and pushes the corrections to the same feature branch. Do not open a replacement pull request.

## 9. Merge correctly

After approval and green CI:

1. Select **Create a merge commit**.
2. Merge into `develop`.
3. Delete the remote feature branch after the merge.
4. Keep the pull request and its comments as evidence.

Do not select **Squash and merge** for this assignment. Squashing hides the original sequence of commits from the target branch history.

After merge, update the local repository:

```powershell
git switch develop
git pull --ff-only origin develop
git branch -d feature/your-feature-name
```

## 10. Keep a branch current and avoid conflicts

Before opening a PR, update the feature branch:

```powershell
git switch develop
git pull --ff-only origin develop
git switch feature/your-feature-name
git merge develop
```

If there are no conflicts, run the tests and push normally.

To reduce conflicts:

- each member normally edits only their service;
- announce shared-file changes in the team chat;
- keep PRs small;
- merge completed PRs regularly;
- pull `develop` before creating every branch;
- never edit the same shared document in two branches without coordination.

## 11. Resolve a merge conflict safely

When Git reports a conflict:

1. Do not delete the repository or create a new copy.
2. Run `git status` to see conflicted files.
3. Open each file and find `<<<<<<<`, `=======` and `>>>>>>>`.
4. Speak with the other file owner when both changes are meaningful.
5. Keep the correct combined content and remove all conflict markers.
6. Run tests.
7. Stage the resolved files and commit the merge.

```powershell
git status
git add path/to/resolved-file
git commit -m "merge: resolve develop conflicts"
git push
```

If unsure, ask the group leader before committing. Never use `git push --force` to solve a conflict.

## 12. Recover from common mistakes

### Edited on `develop` but have not committed

```powershell
git switch -c feature/correct-feature-name
```

Your uncommitted edits move to the new branch.

### Staged the wrong file

```powershell
git restore --staged path/to/file
```

This removes it from staging without deleting the edit.

### Need to discard one uncommitted file

```powershell
git restore path/to/file
```

This permanently discards that file's uncommitted changes. Check the path carefully.

### Committed locally on the wrong branch and have not pushed

Stop and ask the group leader to help move the commit. Do not reset or force push without understanding the result.

### Accidentally exposed a secret

Immediately tell the group leader, rotate the secret, remove it from the code and document the incident. Deleting it in a later commit does not remove it from earlier history.

## 13. Required contribution evidence per member

Every member should finish with all of the following:

- their own GitHub account and correct commit email;
- at least three focused feature/test/documentation PRs for their service;
- a sustained sequence of meaningful commits;
- meaningful unit tests committed by that member;
- Swagger documentation for their endpoints;
- Postman requests for their part of the workflow;
- at least one review of another member's PR;
- contribution entry in `docs/CONTRIBUTION_LOG.md`;
- ability to explain their code, tests and integration during the viva.

The number of commits alone does not earn marks. Quality, continuity, authorship, tests, reviews and understanding matter.

## 14. Recommended PR plan for each owner

### Member 1 - Account Service

1. Account schema, migration and registration.
2. Login, password hashing and JWT issuance.
3. Profile/status APIs, authorization, tests and Swagger.

### Member 2 - Driver & Vehicle Service

1. Driver profile, vehicle schema and persistence.
2. Location and availability business rules.
3. Eligibility, atomic reservation/release, Account integration, tests and Swagger.

### Member 3 - Ride Management Service

1. Ride schema, creation and fare-estimate validation.
2. Driver assignment, reservation and compensation.
3. Lifecycle, authorization, completion/cancellation, tests and Swagger.

### Member 4 - Fare & Payment Service

1. Fare formula, estimate persistence and tests.
2. Final fare and Ride integration.
3. Simulated payment, receipt, authorization, tests and Swagger.

## 15. Group leader weekly checks

Check repository authors:

```powershell
git fetch --all --prune
git shortlog -sne --all
```

Inspect one member's commits:

```powershell
git log --all --author="Member Name" --oneline
```

Also check on GitHub:

- every member appears as a collaborator;
- each member opens PRs under their account;
- CI passes;
- reviewers are not always the group leader;
- contribution is spread across the development period;
- completed work is eventually merged into `develop` and then `main`;
- `docs/CONTRIBUTION_LOG.md` agrees with Git history.

## 16. Integration and release flow

Feature PRs target `develop`. When the integrated system is tested:

1. Open a PR from `develop` to `main`.
2. Run the complete Postman success and negative workflows.
3. Confirm CI passes for all four services.
4. Confirm README, Swagger and the report match the code.
5. Merge using a merge commit.
6. Create the assessed release tag on `main`.

Do not add new features after the release freeze unless fixing a verified problem through another reviewed PR.

