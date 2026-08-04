# Implementation Plan: CI/CD Pipeline Quality & Performance

**Branch**: `001-cicd-pipeline-quality` | **Date**: 2026-08-04 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-cicd-pipeline-quality/spec.md`

## Summary

Replace host-side compilation with a GitHub Actions pipeline that only touches affected
services (`dorny/paths-filter` matrix), verifies each with real unit + Testcontainers
integration tests and a boot/health check, builds and SHA-tags Docker images pushed to
Amazon ECR (OIDC auth, no stored AWS keys), computes releases automatically
(`release-please`), and deploys via AWS SSM `SendCommand` (not SSH) to pull images and run
`docker compose up -d`, with a serialized, self-verifying, auto-rollback deploy step. This
executes the pipeline already specified in `.specify/memory/constitution.md`'s Development
Workflow section and resolves that document's two open TODOs (ECR/OIDC setup, Testcontainers).

## Technical Context

**Language/Version**: Java 21 (Spring Boot / Maven) for the five services; pipeline logic is
GitHub Actions YAML + POSIX shell (no new application language introduced).

**Primary Dependencies**: `dorny/paths-filter`, `actions/setup-java` (Temurin 21, Maven cache),
`aws-actions/configure-aws-credentials` (OIDC), `aws-actions/amazon-ecr-login`,
`googleapis/release-please-action`, Testcontainers (JUnit 5 extension) for integration tests,
AWS CLI (`aws ssm send-command`) on the deploy runner.

**Storage**: N/A for the pipeline itself. Images persist in Amazon ECR (`stop/<service>`,
`eu-north-1`); runtime config/secrets persist in AWS Secrets Manager; the last-deployed image
tag persists as a single file (`.last-deployed-tag`) on the EC2 host for rollback purposes.

**Testing**: JUnit 5 + Mockito (existing unit tests, per constitution Principle IV) plus new
Testcontainers-based integration tests (ephemeral Postgres per service, ephemeral Kafka) that
MUST NOT touch prod RDS or the shared prod Kafka broker; a post-build container healthcheck
polls `/actuator/health` until `UP`.

**Target Platform**: GitHub Actions `ubuntu-latest` runners (CI); single EC2 host (Ubuntu, Docker
Compose) as the sole deploy target, reached via AWS SSM `AWS-RunShellScript` — no SSH.

**Project Type**: CI/CD pipeline for an existing 5-service Spring Boot monorepo (infrastructure
change; no new application module).

**Performance Goals**: End-to-end pipeline (test → build → deploy) under 10 minutes per affected
service (spec SC-001); matrix jobs for independently-affected services run in parallel; Maven
dependency cache (`actions/setup-java` cache) and Docker layer caching avoid rebuilding unchanged
layers/dependencies.

**Constraints**: No application secret present during test/build (FR-004); all AWS access via
short-lived OIDC-assumed roles, no static access keys stored anywhere (FR-010); at most one
deploy running against the EC2 host at a time (FR-009); host never compiles source or builds
images (constitution, Technology & Deployment Constraints).

**Scale/Scope**: 5 services (identity, match, communication, notification, api-gateway), single
AWS region (`eu-north-1`), single EC2 deploy target — matches existing project scope, no
multi-region/multi-host work in scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|---|---|---|
| I. Service Autonomy & Bounded Layering | N/A | Pipeline change only; no service package structure touched. |
| II. Async-First Communication | N/A | No inter-service communication code changed. |
| III. Zero-Trust Authentication | PASS | Deploy stage only delivers `JWT_PUBLIC_KEY`/`JWT_PRIVATE_KEY` via Secrets Manager at container startup, unchanged from existing per-service independent validation. |
| IV. Test Coverage for Business Logic | PASS (resolves TODO) | Adds Testcontainers integration tests per service, fulfilling the constitution's `test` stage and closing `TODO(TESTCONTAINERS)`. |
| V. No Secrets in Code or Version Control | PASS (resolves TODO) | OIDC-only AWS auth (no stored keys), closing `TODO(AWS_SETUP)` once the IAM role/ECR repos exist; secrets remain Secrets-Manager-sourced at runtime only. |
| VI. Consistent API & Error Contracts | N/A | No HTTP contract changes. |
| Development Workflow (Pipeline stages) | PASS | This plan *is* the implementation of the constitution's already-specified 5-stage pipeline; no deviation. |

No violations requiring justification — Complexity Tracking table is empty/omitted.

## Project Structure

### Documentation (this feature)

```text
specs/001-cicd-pipeline-quality/
├── plan.md              # This file
├── research.md           # Phase 0 output
├── data-model.md          # Phase 1 output
├── quickstart.md          # Phase 1 output
└── tasks.md               # Phase 2 output (/speckit-tasks — not created here)
```

No `contracts/` directory: this feature has no public API, library interface, or UI surface — it
is CI/CD orchestration internal to the project. Skipped per plan-template guidance for purely
internal changes.

### Source Code (repository root)

```text
.github/workflows/
├── build.yml               # changes (paths-filter) → test (mvn verify + Testcontainers +
│                            #   container healthcheck) → build (package, Docker build,
│                            #   OIDC → ECR push, SHA tag)
├── deploy.yml                # workflow_run trigger after build.yml succeeds; SSM SendCommand
│                            #   deploy + healthcheck + auto-rollback; concurrency-serialized
├── release-please.yml       # conventional-commit driven semver + changelog
└── oidc-test.yml             # manual smoke test for the OIDC role (already added; kept)

release-please-config.json
.release-please-manifest.json

docker/
├── docker-compose.yml        # image: <ECR_REGISTRY>/stop/<service>:${IMAGE_TAG} (no build:)
├── docker-compose.override.yml  # local dev only — still uses build: + Dockerfile_dev, untouched
└── .env.example

<service>/                    # identity-service, match-service, communication-service,
├── Dockerfile                #   notification-service, api-gateway — Dockerfile unchanged
└── src/test/java/.../*IT.java  # NEW: Testcontainers integration test classes, alongside
                                 #   existing *Test.java unit tests
```

**Structure Decision**: No new top-level directories. This feature only adds/modifies files
under `.github/workflows/`, `docker/docker-compose.yml`, two root-level `release-please` config
files, and new `*IT.java` Testcontainers test classes inside each service's existing
`src/test/java` tree — consistent with the constitution's existing feature-folder/layering rules
(Principle I), since these are test classes, not new production packages.

## Complexity Tracking

> No Constitution Check violations — table intentionally omitted.
