<!--
Sync Impact Report
Version change: template (unratified) → 1.0.0
Rationale: Initial ratification. No prior filled constitution existed; this is the first
codification of principles already observed in the STOP codebase (feature-folder layering,
Kafka-first inter-service communication, per-service JWT validation, DB-per-service isolation,
AWS Secrets Manager secret handling). Treated as MINOR→bootstrapped as MAJOR 1.0.0 per governance
rules for first ratification.

Modified principles: n/a (initial)
Added sections: Core Principles (I-VI), Technology & Deployment Constraints, Development Workflow,
Governance
Removed sections: none (template placeholders replaced)

Templates requiring updates:
✅ .specify/templates/plan-template.md — "Constitution Check" gate is generic, no principle names
   hardcoded; no edit required.
✅ .specify/templates/spec-template.md — no constitution-specific references found.
✅ .specify/templates/tasks-template.md — no constitution-specific references found.
⚠ README.md — does not yet document the module-boundary / Kafka-first / per-service-JWT rules
   codified here; recommend a follow-up doc pass (not done in this change, out of scope for a
   governance-file update).

Follow-up TODOs:
- TODO(RATIFICATION_DATE): Set to the date this file is first merged if different from today's
  drafting date (2026-08-04 used here since no earlier ratified version exists in git history).
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
orchestrated via `docker/docker-compose.yml` on a single EC2 host. Each service has its own
Postgres 15 database container; Redis and Kafka (`apache/kafka:4.2.0`, KRaft mode) are shared
infrastructure on the `backend` network. Container memory limits are explicitly set per service
tier (`x-java-resources`, `x-db-resources`, `x-kafka-resources`, `x-redis-resources` anchors in
`docker-compose.yml`) — new services or infra components MUST define resource limits/reservations
rather than running unbounded. `api-gateway` (Spring Cloud Gateway) is the sole public entry point
(port 8080 exposed); internal services are not directly reachable from outside the `backend`
network.

## Development Workflow

Deployment is triggered by pushes to `main` via `.github/workflows/deploy.yml`, which SSHes into
the EC2 host, pulls, runs `mvn clean package -DskipTests` per service, and runs
`docker compose up -d --build`. Build steps MUST NOT require any secret to be present — secrets
are only needed at container runtime, never at compile/package time, and any change to the build
process MUST preserve this separation. Git commit messages follow conventional-commit-style
prefixes (`chore:`, `fix:`, `feat:`) in imperative mood, matching existing history.

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

**Version**: 1.0.0 | **Ratified**: 2026-08-04 | **Last Amended**: 2026-08-04
