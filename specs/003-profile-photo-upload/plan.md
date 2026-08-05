# Implementation Plan: Profil Fotoğrafı Yükleme, Değiştirme ve Silme

**Branch**: `003-profile-photo-upload` | **Date**: 2026-08-05 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-profile-photo-upload/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Add a tap-to-manage profile photo to the user's own profile: add (if none), replace, or delete
(with confirmation) an avatar image, shown consistently everywhere an avatar currently renders as
an initial-letter circle. Per explicit direction for this plan, three concerns drove every
technical decision below: (1) where/how the photo is persisted, (2) hardening against file-upload
vulnerabilities, and (3) never letting this feature grow unbounded load or disk usage on the
single EC2 host the whole backend already runs on.

The resulting approach: the browser/app uploads the raw image to `identity-service` (capped at a
small size limit), which decodes it with the JDK's built-in `ImageIO` to confirm it is a genuine
image (not a disguised/polyglot file), re-encodes it to a fixed format and max dimension (which
also strips any embedded payload that survived decoding), uploads the processed bytes to a new AWS
S3 bucket under a random server-generated key, deletes the previous object (if any) so storage
never accumulates beyond one object per user, and persists only the resulting object URL in the
existing `avatar_url` column — no schema change needed. Critically, the raw/processed bytes never
touch local disk on the EC2 host (validated and re-encoded in memory, shipped to S3, discarded),
and every subsequent *read* of a photo (profile views, search cards, friend lists) is served
directly from S3 — never proxied back through `identity-service`/`api-gateway` — so the read-heavy
side of this feature (which vastly outnumbers uploads) adds zero load to the host at all.

## Technical Context

**Language/Version**: TypeScript (React Native/Expo) for `frontend/`; Java 21 / Spring Boot (Maven) for `identity-service` — both match the existing stacks, no version changes

**Primary Dependencies**:
- `frontend/`: NEW `expo-image-picker` (gallery selection + camera capture — nothing like it exists today). No new dependency for *rendering* the photo — the core React Native `Image` component (already available, no package needed) is sufficient for a single small avatar image per screen.
- `identity-service/`: NEW AWS SDK for Java v2, S3 module only (`software.amazon.awssdk:s3`) — nothing like it exists today. Image decode/validate/re-encode uses the JDK's built-in `javax.imageio.ImageIO` (already available, no new library) — sufficient for the JPEG/PNG inputs this feature accepts (see research.md Decision 2), avoiding an unjustified extra dependency for that part.

**Storage**: PostgreSQL (existing `identity-db`) — no schema change; the existing `avatar_url VARCHAR(512)` column on `UserProfile` now holds a full S3 object URL instead of a user-typed arbitrary URL. Binary image bytes live in a **new AWS S3 bucket** (external, AWS-managed object storage) — explicitly not on the EC2 host's local disk and not a new self-hosted container (see research.md Decision 1 for why).

**Testing**: `identity-service` — JUnit 5 + Mockito unit tests for the new service-layer logic (image validation/re-encode, S3 key generation, old-object cleanup on replace/delete), per Constitution Principle IV, with the S3 client mocked (no real AWS calls in tests). `frontend/` — no automated test framework exists in this repo (confirmed in prior features); validated manually via `quickstart.md`, consistent with how feature 002 was handled.

**Target Platform**: Same EC2 host / Docker Compose deployment for the backend (`identity-service` gets one new outbound dependency — the AWS S3 API — no new container); iOS/Android/Web via Expo for the frontend, unchanged.

**Project Type**: Web/mobile app with a backend service — this is the first feature in this session that touches `identity-service`, not just `frontend/`.

**Performance Goals**: Photo *reads* (by far the more frequent operation — every profile view, search result card, friend list entry) must add **zero** request load to `identity-service`/`api-gateway`/the EC2 host, because they're served directly from S3 URLs. Photo *uploads* are capped at a small max original-file size (research.md Decision 3) so each upload's CPU/memory/network cost on the single host is small and bounded, regardless of what a user tries to submit.

**Constraints**: (1) Must not grow unbounded storage on the single EC2 host — satisfied by never persisting image bytes to local disk at all, only ever to S3. (2) Must resist file-upload attacks (disguised executables, oversized payloads, path traversal via filename, unauthorized access to another user's photo) — see research.md Decision 4 for the specific mitigations. (3) Provisioning the S3 bucket + IAM policy is an AWS infrastructure change outside `docker-compose.yml` and outside what `/speckit-tasks` can fully automate — flagged as a deploy-time prerequisite, mirroring the existing `TODO(AWS_SETUP)` pattern already in the constitution's Sync Impact Report.

**Scale/Scope**: One optional photo per user profile (no photo history/gallery). Touches: `identity-service` (1 new controller endpoint pair, S3 upload/delete service logic, 2 new error codes), `frontend/` (1 new picker dependency, 1 new action-sheet component, `Image` rendering added to 3 existing avatar spots), and deployment config (1 new S3 bucket, IAM permissions, 2 new env vars).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Service Autonomy & Bounded Layering**: New logic stays entirely inside `identity-service`'s existing `userProfile` feature folder (`controller/ → service/ → repository/`); the controller will not call S3 or the repository directly — a service-layer component handles validation, re-encoding, and S3 upload/delete, then updates `UserProfile.avatarUrl` through the existing repository. ✅ PASS.
- **II. Asynchronous-First Communication**: Confirmed via codebase research that `UserProfileService` field updates (city, position, and now avatarUrl) do not emit any Kafka event today, and no other service reads or caches `avatarUrl` — `UserUpdatedEvent`/`UserUpdatedProducer` is published only from the separate `user` domain's `UserService`, not `userProfile`. No other service needs to react to an avatar change, so no new event is introduced — this follows existing precedent rather than deviating from it. ✅ PASS.
- **III. Zero-Trust Authentication**: The new upload/delete endpoints resolve the acting user via `SecurityUtils.getCurrentUserId()` from the validated JWT — identical to every existing `UserProfileController` endpoint — never from a client-supplied user id or gateway header. ✅ PASS.
- **IV. Test Coverage for Business Logic**: The new service-layer logic (content validation, re-encoding, S3 key lifecycle) is non-trivial business logic and will get JUnit 5 + Mockito unit tests (S3 client mocked), per existing `<Class>Test.java` convention. ✅ PASS (to be delivered in tasks).
- **V. No Secrets in Code**: S3 access uses the EC2 instance's IAM role via the AWS SDK's default credential provider chain — no access key/secret is embedded in code, Docker images, or `docker-compose.yml`. **Note**: this is the first time *application code* (not just deploy scripts) uses AWS credentials at runtime — a natural, constitution-consistent extension of the existing IAM-role pattern, not a new exception to it. ✅ PASS.
- **VI. Consistent API & Error Contracts**: New endpoints are added under the existing `/api/v1/users/profile` base path; failures (invalid file type, oversized file, corrupt/undecodable image) are surfaced through the existing `GlobalExceptionHandler` + a service-specific `IdentityErrorCode`, not raw framework exceptions; request/response DTOs stay separate from the `UserProfile` entity. ✅ PASS.
- **Technology & Deployment Constraints**: The chosen storage (AWS S3) is deliberately **not** a new Docker container on the shared EC2 host, so it does not need (and does not receive) a `docker-compose.yml` resource-limit anchor like the five app services do — see research.md Decision 1 for why a self-hosted alternative (e.g. MinIO) was rejected specifically because it would reintroduce the "bloats the single host" problem this plan exists to avoid.

**Result**: No gates fail. No violations to justify in Complexity Tracking. One item is flagged above (V) as a new-but-consistent extension of an existing pattern, not a deviation. ✅ PASS (both pre- and post-design).

## Project Structure

### Documentation (this feature)

```text
specs/003-profile-photo-upload/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md         # Phase 1 output (/speckit-plan command)
├── quickstart.md         # Phase 1 output (/speckit-plan command)
├── contracts/            # Phase 1 output (/speckit-plan command)
│   ├── avatar-upload.contract.md
│   └── avatar-storage.contract.md
└── tasks.md              # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
identity-service/
└── src/main/java/com/stop/identity_service/userProfile/
    ├── controller/
    │   └── UserProfileController.java        # MODIFIED — add POST/DELETE .../profile/avatar
    ├── service/
    │   ├── UserProfileService.java           # MODIFIED — call avatar service, update avatarUrl
    │   └── avatar/
    │       ├── AvatarStorageService.java      # NEW — validate, re-encode, upload/delete on S3
    │       └── AvatarValidationException.java # NEW (or reuse existing error-code mechanism)
    ├── dto/response/
    │   └── UserProfileResponse.java           # UNCHANGED shape (avatarUrl already exists)
    └── entity/profile/
        └── UserProfile.java                   # UNCHANGED (avatar_url column already exists)

identity-service/src/main/resources/
└── application.yml                            # MODIFIED — multipart max-file-size, S3 bucket/region env vars

frontend/
└── src/
    ├── components/ui/
    │   └── AvatarPicker.tsx                   # NEW — tappable avatar + action sheet (add/change/delete) + image rendering
    ├── screens/profile/
    │   ├── ProfileViewScreen.tsx              # MODIFIED — own-profile avatar uses AvatarPicker instead of initial-circle-only
    │   └── EditProfileScreen.tsx              # MODIFIED — remove free-text "Avatar URL" Input, use AvatarPicker
    ├── components/ui/
    │   └── HeaderAvatar.tsx                   # MODIFIED — render photo if present, else existing initial circle
    ├── screens/search/
    │   └── PlayerProfileScreen.tsx            # MODIFIED — render photo if present (read-only, no picker — not own profile)
    └── api/
        └── profile.api.ts                     # MODIFIED — add uploadAvatar/deleteAvatar calls (multipart POST / DELETE)
```

**Structure Decision**: Existing two-part layout (`frontend/` Expo app, `identity-service/` Spring
Boot service) — no new top-level directory or service. All new backend code lives inside
`identity-service`'s existing `userProfile` feature folder, following the established
`controller/ → service/ → repository/` layering; a new `service/avatar/` sub-package isolates the
S3-specific logic without it leaking into the controller or repository. All new frontend code
follows the existing `components/ui/` (reusable pickers) + `screens/` (feature screens) split
already used for `CityPicker.tsx`/`PositionPicker.tsx`.

## Complexity Tracking

*No violations — table intentionally omitted (Constitution Check passed with no gates failing).*
