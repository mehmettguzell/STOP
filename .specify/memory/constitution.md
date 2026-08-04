<!--
Sync Impact Report
Version change: 1.1.0 → 1.1.1
Rationale: PATCH — implementation-detail correction, no principle or pipeline stage
added/removed/redefined. Image registry switched from Amazon ECR to the GitHub Container
Registry (`ghcr.io`) after repeated, unresolved IAM permission failures on the EC2 host's
`ecr:GetAuthorizationToken` call blocked every deploy attempt. GHCR removes AWS credentials
from the build stage entirely (push uses the run's own `GITHUB_TOKEN`) and reduces the deploy
stage's AWS dependency to Secrets Manager + SSM only, with a GHCR pull token sourced from
Secrets Manager rather than IAM.

Modified sections: Technology & Deployment Constraints, Development Workflow stages 3 and 5
(ECR → GHCR wording)
Added sections: none
Removed sections: none

Templates requiring updates:
✅ .specify/templates/plan-template.md, spec-template.md, tasks-template.md — no
   constitution-specific references found, no edit required.
✅ .github/workflows/build.yml, deploy.yml — rewritten for GHCR (docker/login-action with
   GITHUB_TOKEN for push; Secrets-Manager-sourced PAT for pull on the host).
   docker/docker-compose.yml image references switched from `${ECR_REGISTRY}` to
   `${GHCR_REGISTRY}` for all five services.
✅ README.md — updated to reference GHCR instead of ECR.

Follow-up TODOs:
- TODO(AWS_SETUP): A new Secrets Manager secret `stop/prod/ghcr-pat` (a GitHub PAT with
  `read:packages` scope, key `token`) must be created — the deploy script now depends on it to
  authenticate `docker login ghcr.io` on the host. The EC2 instance role's now-unused
  `ecr-pull`/`ecr-push`-style policies can be removed once GHCR is confirmed working end-to-end
  (cleanup, not blocking).
- TODO(TESTCONTAINERS): Reverted. A first attempt at adding Testcontainers-backed `*IT.java`
  tests failed CI repeatedly — this Spring Boot version's split `-test` starters (and even the
  umbrella `spring-boot-starter-test`) did not resolve `@DataJpaTest`/`@AutoConfigureTestDatabase`
  on the compile classpath, and the underlying `org.testcontainers:*` module version mismatch
  with `spring-boot-dependencies` was never fully pinned down. All Testcontainers dependencies
  and test classes were removed; `test` stage runs `mvn test` (existing Mockito unit tests only)
  again. Needs a properly verified local Maven resolution (not guessed against CI) before
  retrying.
- DEPLOY_TRANSPORT: deploy.yml now uses `aws ssm send-command` (AWS-RunShellScript), not SSH —
  the `SSH_HOST`/`SSH_USER`/`SSH_KEY` repo secrets are no longer referenced and should be deleted
  from GitHub once the SSM-based deploy is confirmed working end-to-end.
-->

# STOP Constitution

## Core Principles

### I. Service Autonomy & Bounded Layering
Each service (`identity-service`, `match-service`, `communication-service`,
`notification-service`, `api-gateway`) owns its domain exclusively: its own Postgres database, its
own Docker network (`identity-db-net`, `match-db-net`, `communication-db-net`,
`notification-db-net`), and its own package tree under `com.stop.<service>`. No service may read
or write another service's database — the only services on a DB's network are the DB itself and
its owning service; cross-service data needs MUST go through Kafka events or an explicit REST
client (see Principle II).

Within a service, code MUST be organized by feature folder (e.g. `friendship/`, `moderation/`,
`matchParticipation/`), each following `controller/ → service/ → repository/` with `dto/request`
and `dto/response` kept separate from JPA `entity` classes. Controllers MUST NOT call repositories
directly — all persistence access goes through a service class. (A known violation exists in
`communication-service/.../controller/ChatHistoryController.java`, which injects repositories
directly; this MUST be refactored to route through a service class the next time that file is
touched, and MUST NOT be used as a precedent for new code.)

**Rationale**: Independent databases and strict layering are what let five services be deployed,
scaled, and reasoned about independently. Bypassing the service layer or reaching into another
service's schema silently reintroduces the tight coupling a microservice split is meant to remove.

### II. Asynchronous-First Communication, REST Only at the Edge
Kafka is the default and preferred mechanism for inter-service communication. A feature that needs
to react to something happening in another service MUST implement it as a
`kafka/producer`/`kafka/consumer`/`kafka/event` triple, following the existing pattern (e.g.
`identity-service/.../user/kafka/producer/UserCreatedProducer.java`).

Synchronous REST between internal services is the exception, not the default, and MUST be
justified by a genuine need for an immediate response the caller cannot proceed without (the
existing precedent is `notification-service`'s `MatchServiceClient`, which needs participant IDs
synchronously to build a notification). Any new synchronous internal REST client MUST document,
in a comment on the client class, why an async Kafka event is not sufficient.

**Rationale**: Kafka decouples service availability — one service being briefly down does not
cascade into failures elsewhere. Synchronous calls reintroduce that coupling, so they must earn
their place rather than be a default convenience.

### III. Zero-Trust Authentication at Every Service
Every service that exposes an HTTP API MUST independently validate the JWT on incoming requests
using its own `JwtDecoderConfig` + `RsaKeyConfig`, resolving the RSA public key from
`JWT_PUBLIC_KEY`. A service MUST NOT trust an `X-User-Id` / `X-User-Role` header, or any other
gateway-injected value, as a substitute for validating the token itself — `api-gateway`'s
`IdentityHeadersFilter` is a convenience for downstream handlers, not an authentication boundary.
This defense-in-depth stance is deliberate: it must hold even though all services currently sit
behind the same `api-gateway` on the `backend` Docker network.

Role-based authorization (`hasRole("ADMIN")`) MUST guard admin and actuator endpoints
consistently; any new admin-only endpoint MUST be added to the relevant `SecurityConfig`
`authorizeHttpRequests` block rather than relying on business-logic checks alone.

**Rationale**: A single compromised or misconfigured service, or an internal network that turns
out to be less isolated than assumed, must not be enough to impersonate a user across the whole
system. Independent verification at every hop is the cost of that guarantee.

### IV. Test Coverage for Business Logic
Service classes containing business logic (not simple pass-through CRUD) MUST have unit tests
using JUnit 5 and Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks`),
following the existing `<Class>Test.java` naming convention (e.g. `AuthServiceTest.java`,
`MatchServiceTest.java`). New Kafka consumers that trigger side effects (state changes, further
events) MUST have a corresponding unit test asserting the side effect occurs on a valid event and
does not occur on an invalid/malformed one.

Current coverage is thin (11 test files across ~486 main source files) — this principle sets the
bar for new and changed code going forward; it does not require retroactively back-filling
existing untested classes, though doing so opportunistically is encouraged.

**Rationale**: Five independently-deployed services with Kafka-driven side effects are hard to
debug by reading logs alone after the fact. Unit tests on service-layer logic are the cheapest
guardrail against regressions that would otherwise only surface in production.

### V. No Secrets in Code or Version Control
Database credentials, JWT signing keys, and any other secret value MUST NOT be committed to the
repository in any form — not in `application.yml`, not in Dockerfiles, not in scripts. Local
development secrets live only in `docker/.env` (gitignored via `.env` / `.env.*` in
`.gitignore`), populated with values distinct from production. Production secrets are stored in
AWS Secrets Manager and fetched at deploy time on the EC2 host using its IAM instance role —
credentials are never embedded in GitHub Actions workflows, CI runner environment, or any
committed file. `docker-compose.yml` and Dockerfiles MUST continue to consume secrets only via
environment variable references (`${VAR}`), never hardcoded values.

**Rationale**: A secret that reaches git history is compromised permanently, regardless of later
rotation, because history is hard to fully scrub and may already be cloned elsewhere. Sourcing
secrets from Secrets Manager via IAM role removes the need for any long-lived AWS credential to
exist on disk or in CI configuration at all.

### VI. Consistent API & Error Contracts
Public HTTP endpoints MUST be versioned under `/api/v1/...`. Each service MUST handle errors
through its own `common/exception/GlobalExceptionHandler.java` paired with a service-specific
error-code enum (e.g. `IdentityErrorCode`) rather than letting framework exceptions leak
unstructured stack traces to clients. DTOs MUST stay separate from JPA entities (`dto/request`,
`dto/response` per feature) — entities MUST NOT be returned directly from controllers.

**Rationale**: Consistent versioning and error shapes let API consumers (mobile/web clients,
other services) handle failures predictably across all five services instead of learning a
different contract per service.

## Technology & Deployment Constraints

Backend services are Spring Boot (Java) built with Maven, packaged as Docker images, and
orchestrated via `docker/docker-compose.yml` on a single EC2 host. Images are built once in CI,
pushed to the GitHub Container Registry (`ghcr.io`), and pulled by tag on the EC2 host — the host MUST NOT compile source or
build images itself (see Development Workflow, stage 3). Each service has its own Postgres 15
database container; Redis and Kafka (`apache/kafka:4.2.0`, KRaft mode) are shared infrastructure
on the `backend` network. Container memory limits are explicitly set per service tier
(`x-java-resources`, `x-db-resources`, `x-kafka-resources`, `x-redis-resources` anchors in
`docker-compose.yml`) — new services or infra components MUST define resource limits/reservations
rather than running unbounded. `api-gateway` (Spring Cloud Gateway) is the sole public entry point
(port 8080 exposed); internal services are not directly reachable from outside the `backend`
network.

## Development Workflow

### Pipeline stages

All changes reach production through a GitHub Actions pipeline triggered on push to `main`.
The pipeline is organized into ordered stages; each MUST pass before the next runs.

**1. `changes` — change detection.** A path-filter job determines which services are affected
by the push and emits a matrix consumed by downstream stages. Test, build, and deploy MUST
operate only on affected services, so an unrelated change never rebuilds, retests, or
redeploys the entire system. A service with no source changes MUST NOT be rebuilt or redeployed.

**2. `test` — verification.** For every affected service the pipeline runs unit tests,
integration tests against ephemeral dependencies (e.g. Testcontainers) — never shared or
production infrastructure such as prod RDS — and a container healthcheck that boots the built
image and confirms `/actuator/health` reports `UP`. A failing test or healthcheck MUST halt the
pipeline; no artifact from a failed run may be built, pushed, or deployed.

**3. `build` — package & publish.** For every affected service the pipeline runs
`mvn clean package`, builds the Docker image, tags it with an immutable tag (git SHA; plus the
release version where applicable), and pushes the image to the GitHub Container Registry using
the run's own `GITHUB_TOKEN` (no AWS credentials needed for this stage at all). Build,
package, and image-build steps MUST NOT require any application secret to be present — secrets
are needed only at container runtime, never at compile/package/build time, and any change to the
build process MUST preserve this separation. Images are the sole deployment artifact; the EC2
host MUST NOT compile source or build images itself.

**4. `release-please` — versioning & changelog.** Release automation parses conventional-commit
history on `main` to compute the next semantic version and maintain a release PR with an
auto-generated changelog. Merging the release PR cuts the version tag and GitHub release that
deployment references. Because versioning is derived mechanically from commit messages,
conventional-commit prefixes are load-bearing, not cosmetic.

**5. `deploy` — release to EC2.** Deployment connects to the EC2 host, pulls the pre-built
images from the GitHub Container Registry by tag (the host authenticates with a pull token
sourced from Secrets Manager, the one long-lived credential this pipeline still needs since
GitHub has no IAM-style short-lived token for private package pulls), and runs
`docker compose up -d` (without `--build`, since images are
already published). Runtime secrets are injected into containers at startup from AWS Secrets
Manager; they are never baked into images or committed to the compose configuration. Deploys
MUST be serialized (no overlapping runs against the same host), a post-deploy healthcheck MUST
confirm each service reports `UP`, and a failed healthcheck MUST roll back to the previously
deployed image tag.

### Commit conventions

Git commit messages MUST follow conventional-commit prefixes (`chore:`, `fix:`, `feat:`, and
breaking-change markers) in imperative mood, matching existing history. This is a hard
requirement, not a style preference: `release-please` derives version bumps and changelog
entries directly from these prefixes, so a mislabeled commit produces an incorrect release.

## Governance

This constitution supersedes ad hoc conventions where they conflict. Amendments are made by
editing `.specify/memory/constitution.md` directly, updating the Sync Impact Report at the top of
the file, and bumping `CONSTITUTION_VERSION` per semantic versioning: MAJOR for backward-
incompatible principle removals/redefinitions, MINOR for new principles or materially expanded
guidance, PATCH for wording/clarification fixes. Every amendment MUST re-check
`.specify/templates/plan-template.md`, `spec-template.md`, and `tasks-template.md` for references
that would go stale, per the propagation checklist in the `speckit-constitution` skill.

Pull requests and code reviews MUST verify compliance with the Core Principles above; any
deviation (e.g. a new synchronous REST call, a controller bypassing its service layer) MUST be
called out explicitly in the PR description with a rationale, not merged silently.

**Version**: 1.1.1 | **Ratified**: 2026-08-04 | **Last Amended**: 2026-08-05
