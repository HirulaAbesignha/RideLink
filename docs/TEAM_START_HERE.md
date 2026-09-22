# Team start here

## Step 1: identify every contributor

Replace the placeholders in the root README with each member's full name and student ID. Every member must use their own GitHub account and Git author identity.

Check your identity:

```powershell
git config user.name
git config user.email
```

Set it for this repository if necessary:

```powershell
git config user.name "Your full name"
git config user.email "your-github-email@example.com"
```

## Step 2: agree on shared contracts

Before implementing business endpoints, complete `docs/api-contracts/API_CONTRACT.md` together. Agree on IDs, roles, endpoint paths, DTOs, errors, ride states and service-to-service authentication.

## Step 3: create the GitHub repository

The group leader creates one private empty GitHub repository. Do not add another README from GitHub because this folder already contains one. Invite all members and the teaching team.

From the `RideLink` folder, run the commands GitHub displays for pushing an existing repository. Use the actual repository URL supplied by GitHub.

## Step 4: protect important branches

After the first push:

1. Create a `develop` branch.
2. Protect `main` and `develop` in GitHub settings.
3. Require a pull request and one approval.
4. Require the service test workflow to pass.
5. Block force pushes and branch deletion.

## Step 5: run every starter test

Each service includes an H2 test profile, so tests do not need PostgreSQL:

```powershell
cd account-service
.\mvnw.cmd test
```

Repeat inside the other three service directories. CI performs the same check automatically.

## Step 6: divide the first pull requests

- Member 1: registration, login and token contract.
- Member 2: driver profile, vehicle model and availability states.
- Member 3: ride model, lifecycle and authorization matrix.
- Member 4: fare formula, quote model and rounding tests.

Each member should open an early, focused pull request. Do not wait until the whole service is finished.

## Group leader checklist

- [ ] Names and student IDs recorded.
- [ ] Repository invitations accepted.
- [ ] Teaching team has access.
- [ ] Contract meeting completed.
- [ ] `main` and `develop` protected.
- [ ] One early pull request per member.
- [ ] Each member reviews at least one peer pull request.
- [ ] Integration starts before all services are complete.
- [ ] Shared Postman collection is updated during development.
- [ ] Final clean-clone rehearsal is scheduled before 1 October.

