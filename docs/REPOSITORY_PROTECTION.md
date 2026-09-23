# Repository protection setup

Repository: `HirulaAbesignha/RideLink`

Goal:

- Team members can create branches, push their work, open PRs, review and merge approved feature PRs into `develop`.
- Only the repository owner can merge a PR into `main`.
- Direct pushes, force pushes and deletion of `main` and `develop` are blocked.
- Automated quality and service tests must pass.

The repository is owned by a personal GitHub account. Personal-repository collaborators have write access and can normally merge pull requests. The `main` ruleset therefore uses **Restrict updates** and gives only the **Repository admin** role bypass access **for pull requests only**.

## 1. Configure allowed merge methods

1. Open the repository on GitHub.
2. Select **Settings**.
3. Select **General**.
4. Scroll to **Pull Requests**.
5. Enable **Allow merge commits**.
6. Disable **Allow squash merging**.
7. Disable **Allow rebase merging**.
8. Enable **Automatically delete head branches** if the team wants merged feature branches cleaned up.

Merge commits preserve each member's original commits in the shared history.

## 2. Create the owner-only `main` ruleset

1. Open **Settings**.
2. In the left sidebar, open **Rules**, then **Rulesets**.
3. Select **New ruleset**, then **New branch ruleset**.
4. Name it `Protect main - owner merge only`.
5. Set **Enforcement status** to **Active**.

### Bypass list

1. Select **Add bypass**.
2. Add **Repository admin**.
3. Change its mode to **For pull requests only**.
4. Do not add the Write role, collaborators or individual teammates.

In a personal repository, the owner is the repository administrator. Teammates are collaborators with write access, so this bypass selection is what reserves `main` updates for the owner.

### Target branch

1. Under **Target branches**, select **Add target**.
2. Choose **Include by pattern**.
3. Enter `main`.

### Enable these rules

- **Restrict updates**
- **Restrict deletions**
- **Require a pull request before merging**
- Required approvals: `1`
- **Dismiss stale pull request approvals when new commits are pushed**
- **Require approval of the most recent reviewable push**
- **Require conversation resolution before merging**
- **Require status checks to pass**
- **Require branches to be up to date before merging**
- **Block force pushes**

Do not enable **Require linear history**, because this project intentionally uses merge commits.

### Required status checks

After the workflows have run at least once, add:

```text
Repository quality
Test account-service
Test driver-vehicle-service
Test ride-management-service
Test fare-payment-service
```

Save the ruleset.

Expected result: teammates can open and review a PR targeting `main`, but only the repository owner can perform the merge after approval and all checks pass.

## 3. Create the team `develop` ruleset

1. Create another branch ruleset.
2. Name it `Protect develop - reviewed team merges`.
3. Set enforcement to **Active**.
4. Do not add a broad bypass role.
5. Target branch pattern: `develop`.

Enable:

- **Restrict deletions**
- **Require a pull request before merging**
- Required approvals: `1`
- **Dismiss stale pull request approvals when new commits are pushed**
- **Require approval of the most recent reviewable push**
- **Require conversation resolution before merging**
- **Require status checks to pass**
- **Require branches to be up to date before merging**
- **Block force pushes**

Do not enable **Restrict updates** for `develop`. Approved collaborators need to merge feature PRs into it.

Add the same five required status checks and save.

## 4. Check Actions permissions

1. Open **Settings**.
2. Open **Actions**, then **General**.
3. Under **Actions permissions**, allow GitHub actions required by the repository.
4. Under **Workflow permissions**, select **Read repository contents and packages permissions**.
5. Do not enable permission for Actions to create and approve pull requests; these workflows do not need it.
6. Save.

## 5. Test the protection with a teammate

Use a harmless documentation change:

1. Teammate branches from `develop`.
2. Teammate changes one documentation line.
3. Teammate runs `check-before-pr.cmd`.
4. Teammate pushes and opens a PR into `develop`.
5. Confirm tests/checker run and one reviewer is required.
6. After approval, confirm the teammate can merge into `develop`.
7. Open a PR from `develop` to `main`.
8. Confirm the teammate cannot merge it.
9. Confirm the repository owner can merge only after approval and checks pass.

Do not test protection using real unfinished service code.

## 6. If `Restrict updates` or rulesets are unavailable

Rulesets are supported for public repositories on GitHub Free. If the repository later becomes private, private-repository rulesets require a plan that supports them.

If GitHub does not show the required control:

1. Keep the repository public without secrets, or use a plan that supports private rulesets.
2. Alternatively transfer the repository to a GitHub organization.
3. In an organization, give members **Write** access and use a ruleset whose bypass list contains only the owner/admin role.

A standard approval rule by itself does not guarantee that only the owner clicks merge. The key rule is **Restrict updates** with an owner-only bypass entry.

## 7. Settings to avoid

- Do not add the Write role to the `main` bypass list.
- Do not allow force pushes.
- Do not allow branch deletion.
- Do not enable squash-only or linear history.
- Do not let the PR author approve their own PR.
- Do not make status checks optional on `main`.
- Do not give teammates repository-admin access.
