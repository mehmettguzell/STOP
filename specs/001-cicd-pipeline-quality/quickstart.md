# Quickstart: Validating the CI/CD Pipeline

Prerequisites this plan assumes are already in place (see spec.md Assumptions / constitution
TODOs) before any scenario below will fully pass:

- Five ECR repositories exist: `stop/identity-service`, `stop/match-service`,
  `stop/communication-service`, `stop/notification-service`, `stop/api-gateway`.
- An OIDC-trusted IAM role (`github-actions-deploy` or equivalent) exists with the ECR push
  policy and the SSM `SendCommand` policy attached, and its ARN is stored as the
  `AWS_GHA_ECR_ROLE_ARN` repo secret.
- The EC2 host has the SSM Agent installed and running, and its instance role permits it to pull
  from ECR and read the relevant Secrets Manager secrets.

## Scenario 1 — OIDC trust works at all

1. GitHub → Actions → **OIDC Test** → **Run workflow**.
2. Expected: "Confirm identity" prints an `assumed-role/...` ARN; "Confirm ECR access" returns
   without an `AccessDenied` error.
3. If this fails, nothing else in this plan can work — resolve first (see spec.md Assumptions
   and the IAM policy discussion already captured in this project's history).

## Scenario 2 — Single-service change stays isolated (User Story 1 / FR-001 / SC-001)

1. Push a commit that only changes a file under `match-service/`.
2. Watch the **Build** workflow run.
3. Expected: the `changes` job's matrix contains only `match-service`; `test` and `build` jobs
   run only for `match-service`; total run time for `match-service`'s jobs is under 10 minutes;
   no job runs for the other four services.

## Scenario 3 — Pull requests get feedback without deploying (FR-012)

1. Open a pull request that changes one service.
2. Expected: `test` and `build` jobs run and report status on the PR; no `deploy.yml` run is
   triggered (it only fires on `push` to `main` via `workflow_run`).

## Scenario 4 — A healthy deploy updates the rollback target (User Story 2, FR-008)

1. Merge a small, safe change to `main` for one service.
2. Watch **Build** succeed, then **Deploy** trigger via `workflow_run`.
3. On the EC2 host, confirm `.last-deployed-tag` now contains the new commit SHA and the service
   container is running that image (`docker inspect --format '{{.Config.Image}}' <service>`).

## Scenario 5 — A failing deploy rolls back automatically and notifies (User Story 2, FR-008/FR-011/FR-013)

1. Merge a change to one service that is known to fail its health check (e.g. a deliberately
   broken `/actuator/health` dependency, in a throwaway test branch — do not do this against a
   service anyone depends on).
2. Expected: `deploy.yml`'s health-check loop times out, it redeploys the tag recorded in
   `.last-deployed-tag`, the workflow run ends with a failed (red) status, and GitHub's default
   run-failure notification reaches the triggering actor/watchers (see research.md §7).
3. Confirm the service is back on the previous, working image afterward.

## Scenario 6 — Two deploys never overlap (FR-009)

1. Trigger two `push`-to-`main` deploys in quick succession (e.g. two small commits a few
   seconds apart).
2. Expected: the `deploy-production` concurrency group queues the second run rather than running
   it in parallel with the first — check the Actions run list for a "queued"/waiting state, not
   two simultaneously "in progress" deploy jobs.

## Scenario 7 — Releases compute themselves (User Story 3, FR-006)

1. Merge a few conventionally-labeled commits (`fix:`, `feat:`) to `main`.
2. Expected: `release-please` opens or updates a release PR with the correct proposed version
   bump and an auto-generated changelog entry, without anyone specifying the version number.
