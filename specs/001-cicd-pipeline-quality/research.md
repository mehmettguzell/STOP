# Phase 0 Research: CI/CD Pipeline Quality & Performance

All decisions below were fully specified by the user's technical brief; this document records
rationale and rejected alternatives so the choices are auditable later, and resolves the one
genuinely open point (rollback notification channel) that the spec left as "must notify" without
naming a mechanism.

## 1. Deploy transport: AWS SSM vs. SSH

**Decision**: `aws ssm send-command` (`AWS-RunShellScript` document) against the EC2 instance ID.

**Rationale**: Removes the last static credential in the pipeline (`SSH_KEY`) and the need for
port 22 to be open at all. Auth becomes IAM-only, consistent with how the pipeline already
authenticates to ECR (OIDC) and how the host already authenticates to Secrets Manager (instance
role) — one consistent trust model instead of a mix of key-based and IAM-based access.

**Alternatives considered**: SSH via `appleboy/ssh-action` (the pre-existing approach) — rejected
because it requires a long-lived private key stored as a GitHub secret and an open inbound port,
both of which the constitution's Principle V ("No Secrets in Code or Version Control") argues
against in spirit even when technically "just a deploy key."

## 2. CI → AWS authentication: OIDC vs. static IAM user keys

**Decision**: GitHub OIDC, `aws-actions/configure-aws-credentials` assuming a scoped IAM role
(`id-token: write` permission), no `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` secrets anywhere.

**Rationale**: Directly required by constitution Principle V and spec FR-010. Credentials are
short-lived and scoped per-run; a leaked GitHub secret can't be replayed outside a workflow run
the way a static key could.

**Alternatives considered**: IAM user with long-lived access keys stored as repo secrets —
rejected as the exact pattern Principle V exists to prevent.

## 3. Integration testing: Testcontainers vs. shared test database

**Decision**: Testcontainers-managed ephemeral Postgres and Kafka per test run, never a shared or
production instance.

**Rationale**: Constitution stage 2 explicitly requires this ("never shared or production
infrastructure such as prod RDS"). This also resolves the constitution's own
`TODO(TESTCONTAINERS)` marker, which flagged that no service had this yet.

**Alternatives considered**: Pointing integration tests at a long-lived shared "test" RDS
instance — rejected: flaky under concurrent CI runs, and a config mistake could point tests at
prod, which the constitution explicitly forbids.

## 4. Change detection: path-filter matrix vs. always build everything

**Decision**: `dorny/paths-filter` computes a per-service-directory change matrix; every
downstream job (test/build/deploy) is parameterized by it.

**Rationale**: Directly required by spec FR-001/SC-001 (10-minute budget only holds if unaffected
services are skipped entirely, not just deprioritized) and constitution stage 1.

**Alternatives considered**: Always test/build/deploy all 5 services on every push — rejected,
fails the 10-minute-per-affected-service target as soon as more than one or two services exist
in a monorepo of this size, and violates "a service with no source changes MUST NOT be rebuilt."

## 5. Image tagging & rollback target

**Decision**: Every build is tagged with the immutable git SHA and pushed; the mutable `latest`
tag is only advanced after that image passes its post-build healthcheck. The deploy stage tracks
the currently-deployed SHA in a single file (`.last-deployed-tag`) on the host as the rollback
target.

**Rationale**: Satisfies FR-005 (traceability) and FR-008/FR-011 (a concrete, known-good rollback
target must always be identifiable, including "no prior version exists" per FR-011).

**Alternatives considered**: Deploying only ever from `latest` with no SHA tag — rejected, gives
no way to identify or roll back to "the previous version" once `latest` has moved.

## 6. Release versioning: release-please vs. manual tagging

**Decision**: `googleapis/release-please-action`, driven by conventional-commit history.

**Rationale**: Required by FR-006 and constitution stage 4; commit-message discipline is already
established practice (per `git log` history reviewed earlier in this project).

**Alternatives considered**: Manual `git tag` + hand-written changelog — rejected as the exact
manual, error-prone process FR-006 exists to remove.

## 7. Rollback notification channel

**Decision**: Rely on GitHub Actions' built-in workflow-failure notifications. `deploy.yml`
already `exit 1`s on the rollback path (see existing implementation), which triggers GitHub's
default notification to the workflow-triggering actor and repo watchers with no additional
integration required.

**Rationale**: Satisfies FR-013 ("actively notified... not discoverable only by checking logs")
today, with zero new secrets, webhooks, or third-party services — consistent with FR-010's
minimal-credential-surface goal. This was the one point spec clarification left as "must notify"
without naming a channel (see spec.md Clarifications, Q3); this research resolves it by choosing
the lowest-friction option that already meets the requirement.

**Alternatives considered**: A dedicated Slack webhook — noted as a reasonable future
enhancement (louder/more immediate signal than email) but not required to meet FR-013 now, and
would add a new secret (`SLACK_WEBHOOK_URL`) and dependency the feature doesn't otherwise need.
Deferred, not rejected — worth revisiting if GitHub's default notification proves too easy to
miss in practice.

## Outcome

No `NEEDS CLARIFICATION` markers remain in the Technical Context. Proceeding to Phase 1.
