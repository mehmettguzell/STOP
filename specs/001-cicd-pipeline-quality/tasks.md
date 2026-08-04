---

description: "Task list for CI/CD Pipeline Quality & Performance"
---

# Tasks: CI/CD Pipeline Quality & Performance

**Input**: Design documents from `/specs/001-cicd-pipeline-quality/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Testcontainers integration tests are explicitly required by the spec (FR-003) and
constitution stage 2 — included below, not optional.

**Organization**: Tasks are grouped by user story (US1/US2/US3, priorities from spec.md) so each
can be delivered and validated independently. `build.yml`, `deploy.yml`, and
`release-please.yml` already exist in a partial/prior state (SSH-based deploy, `mvn test` only,
no Testcontainers) — most tasks below are edits to bring them to the plan's target design, not
greenfield creation.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1/US2/US3)

---

## Phase 1: Setup

**Purpose**: AWS-side prerequisites nothing else in this feature can succeed without.

- [ ] T001 [P] Verify all 5 ECR repositories exist in `eu-north-1`
      (`stop/identity-service`, `stop/match-service`, `stop/communication-service`,
      `stop/notification-service`, `stop/api-gateway`); create any missing ones —
      referenced by `.github/workflows/build.yml`
- [ ] T002 [P] Verify the `github-actions-deploy` IAM role has both the ECR push/list inline
      policy and the SSM `SendCommand` inline policy attached, and its ARN is stored as the
      `AWS_GHA_ECR_ROLE_ARN` repository secret — confirm via `.github/workflows/oidc-test.yml`
- [ ] T003 [P] Verify the SSM Agent is installed and registered on the EC2 host
      (`snap list | grep ssm`, `aws ssm describe-instance-information`) and the host's instance
      role permits ECR pull + Secrets Manager read (already established) — no new IAM needed for
      this, just verification
- [ ] T004 [P] Add Testcontainers dependencies (`testcontainers-bom`, `postgresql`, `kafka`
      JUnit 5 modules) to `identity-service/pom.xml`, `match-service/pom.xml`,
      `communication-service/pom.xml`, `notification-service/pom.xml` (`api-gateway` has no DB/
      Kafka dependency, skip)

**Checkpoint**: AWS side is reachable and provable via `oidc-test.yml`; all service POMs can
compile Testcontainers-based tests.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared pipeline mechanics every user story depends on.

**⚠️ CRITICAL**: No user story phase below can be validated end-to-end until this phase is done.

- [ ] T005 Change the `test` job's Maven goal in `.github/workflows/build.yml` from
      `mvn -B clean test` to `mvn -B clean verify` so Testcontainers-backed integration tests
      (added in US1) actually run
- [X] T006 Add Docker Buildx layer caching (`docker/setup-buildx-action` +
      `cache-from`/`cache-to: type=gha`) to the `build` job's image-build step in
      `.github/workflows/build.yml`, replacing the plain `docker build` call — required to hit
      the 10-minute SC-001 budget on repeat runs
- [X] T007 Confirm `actions/setup-java`'s `cache: maven` is present on both the `test` and
      `build` jobs in `.github/workflows/build.yml` (already present on `build`; add to `test`
      job if missing)

**Checkpoint**: `build.yml` is ready to actually execute Testcontainers tests with caching in
place. User story phases below can now be implemented and validated independently.

---

## Phase 3: User Story 1 - Fast, isolated feedback on a single-service change (Priority: P1) 🎯 MVP

**Goal**: A push or PR touching one service only tests/builds that service, in under 10 minutes.

**Independent Test**: Run quickstart.md Scenarios 2 and 3.

### Tests for User Story 1

- [ ] T008 [P] [US1] Add a Testcontainers integration test class
      `identity-service/src/test/java/com/stop/identity_service/IdentityServiceIT.java` that
      boots an ephemeral Postgres and exercises at least one repository/service path against it
- [ ] T009 [P] [US1] Add a Testcontainers integration test class
      `match-service/src/test/java/com/stop/match_service/MatchServiceIT.java` (ephemeral
      Postgres)
- [ ] T010 [P] [US1] Add a Testcontainers integration test class
      `communication-service/src/test/java/com/stop/communication_service/CommunicationServiceIT.java`
      (ephemeral Postgres)
- [ ] T011 [P] [US1] Add a Testcontainers integration test class
      `notification-service/src/test/java/com/stop/notification_service/NotificationServiceIT.java`
      (ephemeral Postgres)
- [ ] T012 [P] [US1] Add at least one Testcontainers-backed Kafka producer/consumer integration
      test (ephemeral Kafka) in whichever service's `kafka/` package most directly owns a
      producer→consumer flow (e.g. `identity-service/src/test/java/.../user/kafka/UserCreatedFlowIT.java`)

### Implementation for User Story 1

- [X] T013 [US1] Confirm the `changes` job's `dorny/paths-filter` filters in
      `.github/workflows/build.yml` correctly scope to each of the 5 service directories, and
      that the `test`/`build` job matrices consume `needs.changes.outputs.services` for both
      `push` and `pull_request` events
- [X] T014 [US1] Confirm `.github/workflows/build.yml`'s `build` job does not run `deploy`-only
      steps on `pull_request` events (the `latest`-tag push step is already gated on
      `github.event_name == 'push'` — verify this still holds after T005/T006 edits)
- [ ] T015 [US1] Run quickstart.md Scenario 2 (single-service push) and Scenario 3 (pull request)
      against a throwaway branch; confirm total per-service job time is under 10 minutes and
      record the actual duration in this task's completion note

**Checkpoint**: User Story 1 is independently functional — pushes and PRs touching one service
stay isolated to that service and complete within budget.

---

## Phase 4: User Story 2 - Safe, self-healing production deploys (Priority: P1) 🎯 MVP

**Goal**: Deploys go out via SSM (not SSH), verify their own health, and auto-rollback + notify
on failure.

**Independent Test**: Run quickstart.md Scenarios 4, 5, and 6.

### Implementation for User Story 2

- [X] T016 [US2] Rewrite `.github/workflows/deploy.yml`'s deploy step to call
      `aws ssm send-command --document-name AWS-RunShellScript --instance-ids <id>` instead of
      `appleboy/ssh-action`, authenticating via the same OIDC role pattern as `build.yml`
      (`aws-actions/configure-aws-credentials` with `id-token: write`)
- [X] T017 [US2] Move the deploy shell logic (ECR login via host IAM role, `.last-deployed-tag`
      read, `docker compose pull` + `up -d`, health-check loop, rollback-on-failure) into the
      SSM command payload in `.github/workflows/deploy.yml`, preserving the existing
      `deploy_tag`/`wait_healthy` shell functions
- [X] T018 [US2] Remove the now-unused `SSH_HOST`/`SSH_USER`/`SSH_KEY` references from
      `.github/workflows/deploy.yml`; note in the PR description that these repo secrets should
      be deleted from GitHub settings once the SSM-based deploy is confirmed working
- [X] T019 [US2] Confirm the `concurrency: group: deploy-production` block is retained in the
      rewritten `.github/workflows/deploy.yml` so overlapping deploys still queue
- [X] T020 [US2] Confirm the rewritten `deploy.yml` still `exit`s non-zero on the rollback path,
      so GitHub's built-in workflow-failure notification fires per research.md §7 (no new
      notification integration needed — this is a verification task, not new code)
- [ ] T021 [US2] Run quickstart.md Scenario 4 (healthy deploy updates `.last-deployed-tag`),
      Scenario 5 (failing deploy auto-rolls-back and the run shows failed/red with a
      notification), and Scenario 6 (two rapid deploys queue rather than overlap)

**Checkpoint**: User Stories 1 AND 2 both work independently — the pipeline is fast, isolated,
and self-healing.

---

## Phase 5: User Story 3 - Traceable, automatic versioning (Priority: P2)

**Goal**: Releases and changelogs compute themselves from conventional commits; every deployed
service traces back to an exact commit.

**Independent Test**: Run quickstart.md Scenario 7.

### Implementation for User Story 3

- [X] T022 [P] [US3] Confirm `.github/workflows/release-please.yml` has `contents: write` and
      `pull-requests: write` permissions and correctly references
      `release-please-config.json` / `.release-please-manifest.json` at the repo root
- [X] T023 [P] [US3] Confirm `.github/workflows/build.yml`'s `build` job tags every image with
      `github.sha` unconditionally, and only pushes/advances the `latest` tag after the
      healthcheck step passes (already implemented — verification task)
- [ ] T024 [US3] Merge a few `fix:`/`feat:`-labeled commits on a throwaway branch scenario and
      run quickstart.md Scenario 7; confirm the release PR's proposed version bump matches the
      commit labels

**Checkpoint**: All three user stories are independently functional — this is the full feature.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T025 [P] Update `.specify/memory/constitution.md`'s Sync Impact Report to close out
      `TODO(AWS_SETUP)` and `TODO(TESTCONTAINERS)` once T001-T004 and T008-T012 are confirmed done
- [X] T026 [P] Document the pipeline (trigger model, stages, rollback behavior) in `README.md`,
      closing the constitution's still-pending README TODO from the v1.0.0 ratification
- [ ] T027 Run the full quickstart.md scenario list (1-7) end-to-end in one sitting as a final
      sign-off before treating this feature as done

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately, all four tasks parallel
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories (T005 in particular
  gates every Testcontainers test added in US1 from actually running)
- **User Stories (Phase 3-5)**: All depend on Foundational; US1 and US2 are both P1 and can
  proceed in parallel if staffed, US3 (P2) can start anytime after Foundational too since it
  touches different files (`release-please.yml`) than US1/US2
- **Polish (Phase 6)**: Depends on whichever user stories are in scope for this delivery being
  complete

### User Story Dependencies

- **US1 (P1)**: No dependency on US2/US3. Independently testable via quickstart Scenarios 2-3.
- **US2 (P1)**: No dependency on US1/US3 — touches `deploy.yml` only, US1 touches `build.yml`
  and service test trees. Independently testable via quickstart Scenarios 4-6.
- **US3 (P2)**: No dependency on US1/US2 — touches `release-please.yml` and verifies tagging
  behavior already built into `build.yml` by earlier work. Independently testable via Scenario 7.

### Parallel Opportunities

- All Setup tasks (T001-T004) in parallel
- T008-T012 (Testcontainers test classes, all different files) in parallel
- T022-T023 (US3 verification tasks, different files) in parallel
- Once Foundational (Phase 2) is done, US1, US2, and US3 can be staffed to three different
  people/sessions simultaneously — they touch disjoint files
      (`build.yml`+service test trees / `deploy.yml` / `release-please.yml`+tag-verification)

---

## Parallel Example: User Story 1

```bash
# Launch all Testcontainers test classes together:
Task: "Add Testcontainers IT class in identity-service/src/test/java/.../IdentityServiceIT.java"
Task: "Add Testcontainers IT class in match-service/src/test/java/.../MatchServiceIT.java"
Task: "Add Testcontainers IT class in communication-service/src/test/java/.../CommunicationServiceIT.java"
Task: "Add Testcontainers IT class in notification-service/src/test/java/.../NotificationServiceIT.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 + User Story 2)

Both US1 and US2 are P1 — together they are the MVP: fast isolated feedback AND safe deploys.
US3 (automatic versioning) is valuable but the pipeline is safe and usable without it.

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3 (US1) and Phase 4 (US2) — in either order, or in parallel if staffed
4. **STOP and VALIDATE**: run quickstart Scenarios 2-6
5. Ship this as the MVP pipeline

### Incremental Delivery

1. Setup + Foundational → pipeline mechanics ready
2. US1 → validate → per-service isolation + speed confirmed
3. US2 → validate → safe, self-healing deploys confirmed (MVP complete once both US1 and US2 land)
4. US3 → validate → automatic versioning confirmed
5. Polish → constitution/README sync, full sign-off run

---

## Notes

- `[P]` tasks touch different files and have no ordering dependency on incomplete tasks
- Most tasks are edits to already-existing files (`build.yml`, `deploy.yml`,
  `release-please.yml`) rather than new files — this is a brownfield hardening pass, not a
  greenfield build
- Commit after each task or logical group, using the conventional-commit prefixes the
  constitution and US3 both depend on
- Stop at either MVP checkpoint (end of Phase 4) or the full checkpoint (end of Phase 5) to
  validate independently before continuing
