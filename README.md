# STOP

## Architecture

Five independently deployable Spring Boot services, each with its own Postgres database and
Docker network: `identity-service`, `match-service`, `communication-service`,
`notification-service`, and `api-gateway` (Spring Cloud Gateway, the sole public entry point).

- **Inter-service communication** is Kafka-first (`kafka/producer`/`kafka/consumer` per feature).
  Synchronous REST between services is the exception, used only where an immediate response is
  required (e.g. `notification-service`'s `MatchServiceClient`).
- **Authentication** is zero-trust: every service independently validates the JWT on incoming
  requests using its own RSA public key — no service trusts `api-gateway`'s `X-User-Id`/
  `X-User-Role` headers as a substitute for validating the token itself.
- **Secrets** (DB credentials, JWT keys) live only in AWS Secrets Manager in production, fetched
  via the EC2 host's IAM instance role — never committed, never passed as static CI credentials.

See `.specify/memory/constitution.md` for the full set of governing principles.

## CI/CD Pipeline

All changes reach production through a GitHub Actions pipeline (`.github/workflows/`), triggered
on push to `main`:

1. **`changes`** — detects which of the five services were touched and scopes every downstream
   step to just those services.
2. **`test`** — runs unit tests and Testcontainers-backed integration tests (ephemeral Postgres/
   Kafka, never prod infrastructure) for each affected service; also runs on pull requests for
   pre-merge feedback (no deploy on PRs).
3. **`build`** — packages, builds a Docker image per affected service, tags it with the
   immutable git SHA, and pushes to Amazon ECR (`stop/<service>`). Authentication to AWS is via
   GitHub OIDC — no long-lived AWS credentials are stored anywhere.
4. **`release-please`** — computes the next semantic version and changelog from
   conventional-commit history (`fix:`, `feat:`, etc.).
5. **`deploy`** — connects to the EC2 host via AWS SSM (`aws ssm send-command`, not SSH), pulls
   the new images, and runs `docker compose up -d`. Deploys are serialized; a failed post-deploy
   health check automatically rolls back to the previous image tag and the failed run notifies
   the team via GitHub's built-in workflow-failure notification.

Full design rationale: `specs/001-cicd-pipeline-quality/`.

## Local Development

```bash
cd docker
docker compose up -d
```

`docker-compose.override.yml` builds each service locally with hot-reload
(`Dockerfile_dev`); production instead pulls pre-built images from ECR
(`docker-compose.yml`'s `image:` references). Copy `docker/.env.example` to `docker/.env` and
fill in local-only values (dev DB passwords, a dev-only JWT key pair) before starting.
