---

description: "Task list for Profil Fotoğrafı Yükleme, Değiştirme ve Silme"
---

# Tasks: Profil Fotoğrafı Yükleme, Değiştirme ve Silme

**Input**: Design documents from `/specs/003-profile-photo-upload/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/avatar-upload.contract.md, contracts/avatar-storage.contract.md, quickstart.md

**Tests**: `identity-service` gets JUnit 5 + Mockito unit tests for the new service-layer logic (Constitution Principle IV, S3 client mocked — no real AWS calls in tests). `frontend/` has no automated test framework (confirmed in prior features); manual verification tasks driven by quickstart.md stand in for it, consistent with features 002/003 so far.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Paths are relative to repository root

## Path Conventions

Two-part layout: `identity-service/src/main/java/com/stop/identity_service/...` (backend) and
`frontend/src/...` (Expo app) — no new top-level directory (see plan.md Structure Decision).

---

## Phase 1: Setup

**Purpose**: Baseline sanity check before touching either codebase

- [X] T001 Confirm both projects currently build/run clean before changes: `cd identity-service && mvn -q compile` and `cd frontend && npm install && npm run web`. Note the current avatar rendering spots as the pre-change baseline: `frontend/src/components/ui/HeaderAvatar.tsx:26-28`, `frontend/src/screens/profile/ProfileViewScreen.tsx:142-150`, `frontend/src/screens/search/PlayerProfileScreen.tsx:171-174`, `frontend/src/screens/search/SearchScreen.tsx` PlayerCard (~lines 217-227), and the free-text "Avatar URL" `Input` in `EditProfileScreen.tsx`/`CreateProfileScreen.tsx`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: New dependencies, config, and error codes that every user story's implementation needs — MUST be in place before any story's tasks start

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T002 [P] Add the AWS SDK for Java v2 S3 module (`software.amazon.awssdk:s3`) as a dependency in `identity-service/pom.xml` (research.md Decision 1 — no other AWS SDK dependency exists yet in this service).
- [X] T003 [P] In `identity-service/src/main/resources/application.yml`, add `spring.servlet.multipart.max-file-size`/`max-request-size` (a few MB, e.g. `5MB`, per research.md Decision 3) and an `aws: { region: ${AWS_REGION}, s3: { bucket: ${AWS_S3_BUCKET} } }`-style properties block reading two new env vars.
- [X] T004 [P] Add `AWS_S3_BUCKET` and `AWS_REGION` to the `identity-service` service's `environment:` block in `docker/docker-compose.yml` (alongside the existing `DB_URL`/`JWT_PUBLIC_KEY`-style vars) — no credentials env var, per research.md Decision 6 / contracts/avatar-storage.contract.md (EC2 instance IAM role only).
- [X] T005 [P] Add three new entries to `identity-service/src/main/java/com/stop/identity_service/common/error/IdentityErrorCode.java`: `INVALID_IMAGE_FILE` (400), `FILE_TOO_LARGE` (400), following the existing enum-entry format (code string, Turkish message, `HttpStatus`).
- [X] T006 Create `identity-service/src/main/java/com/stop/identity_service/config/aws/S3Config.java`: a Spring `@Configuration` exposing an `S3Client` `@Bean`, region from the `AWS_REGION`-backed property (T003), credentials left to the AWS SDK's default provider chain (no explicit credentials — EC2 instance IAM role in prod). (Depends on T002, T003)
- [X] T007 [P] Run `cd frontend && npx expo install expo-image-picker` (no image-picker dependency exists yet), then add an `expo-image-picker` entry to the `plugins` array in `frontend/app.json` with `photosPermission`/`cameraPermission` Turkish descriptions (e.g. "STOP, profil fotoğrafı seçebilmen için galerine erişim istiyor." / "...kamerana erişim istiyor.") — `app.json`'s `plugins` array is currently empty.

**Checkpoint**: Foundation ready — US1, US2, and US3 implementation can now proceed (US2/US3 build on US1's upload plumbing, see Dependencies below)

---

## Phase 3: User Story 1 - Fotoğrafı olmayan bir kullanıcının fotoğraf eklemesi (Priority: P1) 🎯 MVP

**Goal**: A user with no profile photo can tap their avatar, pick/take an image, and have it uploaded, validated, re-encoded, stored in S3, and shown everywhere their avatar appears.

**Independent Test**: Sign in as a user with no photo, tap the avatar, choose a gallery image, confirm the upload progress indicator appears, and confirm the resulting photo shows on the own profile view, in search results, and via `HeaderAvatar` — replacing the initial-letter circle.

### Implementation for User Story 1

- [X] T008 [US1] Create `identity-service/src/main/java/com/stop/identity_service/userProfile/service/avatar/AvatarStorageService.java` with two methods: `uploadAvatar(byte[] originalBytes)` — decodes with `javax.imageio.ImageIO` (throw `BusinessException(IdentityErrorCode.INVALID_IMAGE_FILE)` if decoding fails per research.md Decision 2), downscales to a fixed max dimension (e.g. 1024px) if larger, re-encodes to JPEG, uploads to S3 under a random `avatars/<uuid>.jpg` key (research.md Decision 5) via the `S3Client` from T006, and returns the object's public URL; and `deleteObject(String avatarUrl)` — derives the key from a previously-returned URL and issues a single `DeleteObject` call, per contracts/avatar-storage.contract.md. (Depends on T006)
- [X] T009 [US1] In `identity-service/src/main/java/com/stop/identity_service/userProfile/service/UserProfileService.java`, add `updateAvatar(UUID userId, byte[] fileBytes)`: load the user's `UserProfile`, call `AvatarStorageService.uploadAvatar(...)`, if the profile's current `avatarUrl` was non-null call `AvatarStorageService.deleteObject(oldUrl)` (contracts/avatar-upload.contract.md — same upsert endpoint handles both "add" and "replace"), persist the new `avatarUrl`, and apply the same `@CacheEvict(value = {"user:profile","user:self","user:public"}, key = "#userId")` used by `updateUserProfile` so cached reads don't serve the stale avatar. In `identity-service/src/main/java/com/stop/identity_service/userProfile/controller/UserProfileController.java`, add `POST /api/v1/users/profile/avatar` (`@RequestParam MultipartFile file`) calling it via `SecurityUtils.getCurrentUserId()`. (Depends on T008)
- [X] T010 [P] [US1] Add `uploadAvatar(uri: string): Promise<{ avatarUrl: string }>` to `frontend/src/api/profile.api.ts`: build a `FormData` with the picked image (`file` field, matching the multipart contract) and `POST` to `/users/profile/avatar` with `Content-Type: multipart/form-data`.
- [X] T011 [US1] Create `frontend/src/components/ui/AvatarPicker.tsx`: props `{ avatarUrl: string | null; size?: number; onChange: (avatarUrl: string | null) => void }`. Renders the avatar (an `Image` from `avatarUrl` if present, else the existing initial-letter circle style) as a `TouchableOpacity` that opens a custom `Modal` action sheet (styled like `CityPicker.tsx`/`PositionPicker.tsx`'s modal) with rows "Galeriden Seç" (`ImagePicker.launchImageLibraryAsync`, requesting `requestMediaLibraryPermissionsAsync` first) and "Kamera ile Çek" (`ImagePicker.launchCameraAsync`, requesting `requestCameraPermissionsAsync` first) — label these both under a header that reads "Fotoğraf Ekle" when `avatarUrl` is empty (US2 changes this label for the non-empty case). On image selection, show an inline upload-progress state, call `profileApi.uploadAvatar(uri)` (T010), and call `onChange(result.avatarUrl)` on success or show an `Alert` with the server's error message on failure (FR-005) leaving `avatarUrl` unchanged. (Depends on T007, T010)
- [X] T012 [US1] In `frontend/src/screens/profile/ProfileViewScreen.tsx`, replace the `avatarCircle`/`avatarText` block (lines 142-150) with: `<AvatarPicker avatarUrl={profile?.avatarUrl ?? null} onChange={(url) => storeSetProfile({ ...profile!, avatarUrl: url })} />` when `isOwnProfile` is true; when false, keep a read-only version (existing initial-circle, or an `Image` if `profile?.avatarUrl` is present) with no tap action (spec FR-009/Edge Cases — the picker must not appear on another user's profile). (Depends on T011)
- [X] T013 [P] [US1] In `frontend/src/components/ui/HeaderAvatar.tsx`, import `useProfileStore` and render an `Image` from `useProfileStore((s) => s.profile?.avatarUrl)` when present, falling back to the existing initial-letter `Text` (line 26-28) when there's no photo or the store hasn't loaded a profile yet — do not add a new fetch call to this component; it only reads whatever `useProfileStore` already has cached (populated by `ProfileViewScreen`/`EditProfileScreen`).
- [X] T014 [P] [US1] In `frontend/src/screens/search/PlayerProfileScreen.tsx`, render an `Image` from `profile.avatarUrl` when present instead of the initial-letter `Text` at lines 171-174 (this screen is always someone else's profile — read-only, no `AvatarPicker`).
- [X] T015 [P] [US1] In `frontend/src/screens/search/SearchScreen.tsx`'s `PlayerCard` function, render an `Image` from `profile.avatarUrl` when present instead of the initial-letter `Text` (~lines 218-220) — read-only, matches the search-results requirement in spec FR-010.
- [X] T016 [P] [US1] Remove the free-text "Avatar URL" `Input` (and its `avatarUrl` form state/validation) from `frontend/src/screens/profile/EditProfileScreen.tsx` and `frontend/src/screens/profile/CreateProfileScreen.tsx` — per spec Assumptions, `AvatarPicker` (wired into `ProfileViewScreen` in T012) replaces manual URL entry entirely; profile creation no longer sets an avatar at creation time, it's added afterward from the profile view.
- [X] T017 [US1] Add `identity-service/src/test/java/com/stop/identity_service/service/AvatarStorageServiceTest.java` (JUnit 5 + Mockito, `S3Client` mocked): a valid JPEG/PNG byte array is accepted, re-encoded, and uploaded (mock `putObject` invoked, returned URL is well-formed); a non-image/corrupt byte array throws `BusinessException` with `IdentityErrorCode.INVALID_IMAGE_FILE`; an oversized image is downscaled to the configured max dimension before upload. (Depends on T008; Constitution Principle IV)
- [X] T018 [US1] Manually verify quickstart.md Scenarios 1 and 6 (add a photo end-to-end and confirm it renders everywhere; confirm — via network inspector / backend request logs — that viewing the photo afterward never hits `identity-service`/`api-gateway`, only the S3 URL directly). (Depends on T009, T012-T015)

**Checkpoint**: User Story 1 is fully functional and independently testable — a user with no photo can add one, and it appears consistently.

---

## Phase 4: User Story 2 - Mevcut fotoğrafı olan bir kullanıcının fotoğrafını değiştirmesi (Priority: P1)

**Goal**: A user with an existing photo can replace it; the old photo becomes fully inaccessible afterward.

**Independent Test**: As a user with a photo already set, tap the avatar, confirm the action sheet reads "Fotoğrafı Değiştir" (not "Fotoğraf Ekle"), pick a new image, confirm it replaces the old one everywhere, and confirm (via S3) the old object no longer exists.

**Note**: Because the upload endpoint is an upsert by design (contracts/avatar-upload.contract.md — "add" and "replace" are the same server operation), most of this story's backend behavior was already built as part of US1 (T009's old-object deletion). This story's tasks are correspondingly light: they verify that shared behavior explicitly and adjust the one piece of UI that differs (the action-sheet label).

### Implementation for User Story 2

- [X] T019 [US2] Add a test case (in `AvatarStorageServiceTest.java` or a new `UserProfileServiceTest.java` addition, S3Client mocked) verifying that `UserProfileService.updateAvatar` calls `AvatarStorageService.deleteObject` with the previous `avatarUrl` when the user already had one set, and does NOT call it when they didn't (the "add" case from US1). (Depends on T009)
- [X] T020 [US2] In `frontend/src/components/ui/AvatarPicker.tsx` (T011), change the action-sheet header/label to "Fotoğrafı Değiştir" instead of "Fotoğraf Ekle" whenever `avatarUrl` is non-empty — the underlying "Galeriden Seç"/"Kamera ile Çek" flow and `onChange` wiring are unchanged. (Depends on T011)
- [X] T021 [US2] Manually verify quickstart.md Scenario 2 (replace an existing photo; confirm via AWS console/`aws s3 ls` that the previous object was deleted, not just that the UI moved on). (Depends on T009, T019, T020)

**Checkpoint**: User Stories 1 AND 2 both work — adding and replacing a photo are both fully functional, and storage never accumulates more than one object per user.

---

## Phase 5: User Story 3 - Kullanıcının mevcut fotoğrafını silmesi (Priority: P2)

**Goal**: A user with a photo can delete it (with confirmation) and revert to the initial-letter fallback everywhere.

**Independent Test**: As a user with a photo, tap the avatar, choose "Fotoğrafı Sil", cancel once (photo unchanged), then confirm (photo removed, initial-letter shown everywhere, S3 object gone).

### Implementation for User Story 3

- [X] T022 [US3] In `UserProfileService.java`, add `deleteAvatar(UUID userId)`: load the profile, if `avatarUrl` is set call `AvatarStorageService.deleteObject(avatarUrl)` (T008), set `avatarUrl` to `null`, persist, and apply the same `@CacheEvict` as T009. In `UserProfileController.java`, add `DELETE /api/v1/users/profile/avatar` calling it via `SecurityUtils.getCurrentUserId()`, returning success as a no-op when there was no photo to delete (contracts/avatar-upload.contract.md). (Depends on T008)
- [X] T023 [P] [US3] Add `deleteAvatar(): Promise<void>` to `frontend/src/api/profile.api.ts` (`DELETE /users/profile/avatar`).
- [X] T024 [US3] In `AvatarPicker.tsx`, add a "Fotoğrafı Sil" row to the action sheet, shown only when `avatarUrl` is non-empty. Selecting it closes the action sheet and shows an `Alert.alert` confirmation ("Fotoğrafını silmek istediğine emin misin?", cancel/confirm — matching the existing logout-confirmation pattern in `ProfileViewScreen.tsx:94-97`); on confirm, call `profileApi.deleteAvatar()` (T023) and then `onChange(null)`; on cancel, do nothing (spec US3 AS3). (Depends on T011, T023)
- [X] T025 [US3] Add `identity-service/src/test/java/com/stop/identity_service/service/UserProfileServiceTest.java` (or extend it if T019 already created it) covering `deleteAvatar`: clears `avatarUrl` and calls `AvatarStorageService.deleteObject` when a photo existed; is a no-op (no delete call, no error) when it didn't. (Depends on T022)
- [X] T026 [US3] Manually verify quickstart.md Scenario 3 (delete with cancel-then-confirm) and Scenario 5 (viewing another user's profile shows their photo but no tappable action sheet — confirms the `isOwnProfile` gating from T012 also covers the delete option). (Depends on T024)

**Checkpoint**: All three user stories work independently and together — add, replace, and delete are all fully functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final verification across all three stories

- [X] T027 Manually verify quickstart.md Scenario 4 (upload a renamed non-image file and an oversized file; confirm both are rejected with a clear message and the avatar/state is unchanged in both cases).
- [X] T028 [P] Run `cd frontend && npx tsc --noEmit` to confirm no new type errors in `AvatarPicker.tsx`, `profile.api.ts`, `HeaderAvatar.tsx`, `ProfileViewScreen.tsx`, `PlayerProfileScreen.tsx`, `SearchScreen.tsx`, `EditProfileScreen.tsx`, or `CreateProfileScreen.tsx`.
- [X] T029 [P] Run `cd identity-service && mvn -q test` to confirm the full existing suite plus the new `AvatarStorageServiceTest`/`UserProfileServiceTest` cases pass with no regressions.
- [X] T030 Run the full `specs/003-profile-photo-upload/quickstart.md` validation (all 6 scenarios) end-to-end as a final regression pass.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all three user stories (the S3 client, multipart config, and error codes must exist before any story's backend tasks can compile).
- **User Story 1 (Phase 3)**: Depends on Foundational. This is where the shared upload/validate/re-encode/upsert plumbing is built.
- **User Story 2 (Phase 4)**: Depends on Foundational **and** on US1's T008/T009 (the upsert endpoint and `AvatarStorageService` already implement "replace" as a side effect of "add" — see research.md Decision 5). Not independent of US1's *code*, but independently *testable* as its own acceptance scenario.
- **User Story 3 (Phase 5)**: Depends on Foundational **and** on US1's T008 (`AvatarStorageService.deleteObject` is reused, not reimplemented).
- **Polish (Phase 6)**: Depends on all three user stories being complete.

### Within Each User Story

- US1: T008 (service) → T009 (endpoint) → T010 (frontend API call) → T011 (picker component) → T012 (wire into ProfileViewScreen); T013/T014/T015/T016 (other render spots + cleanup) can run in parallel with each other and with T012 once T011 exists (T013-T015 don't depend on T012, only on `avatarUrl` already being in their existing data). T017 (test) and T018 (manual verify) close out the story.
- US2: T019 (test) can run as soon as T009 exists; T020 (label change) only needs T011; T021 (manual verify) needs both.
- US3: T022 (endpoint) needs T008; T023 (frontend API call) is independent of T022 (different file); T024 (UI) needs T011 and T023; T025/T026 close out the story.

### Parallel Opportunities

- T002, T003, T004, T005 (Foundational) touch four different files and can all run in parallel.
- T007 (frontend dependency) is independent of all backend Foundational tasks and can run in parallel with T002-T006.
- Within US1: T013, T014, T015, T016 touch four different files with no interdependency and can run in parallel once T011 exists (T013/T014/T015 don't even need T011 — they only need `Image` + the existing `profile.avatarUrl` field, so they could in principle start as soon as Foundational is done, in parallel with T008-T012).
- T028 (frontend typecheck) and T029 (backend test suite) touch entirely different codebases and can always run in parallel with each other.

---

## Parallel Example: Foundational Phase

```bash
# These four can run in parallel (different files, no shared dependency):
Task: "Add AWS SDK S3 dependency to identity-service/pom.xml"                          # T002
Task: "Add multipart/AWS config properties to application.yml"                          # T003
Task: "Add AWS_S3_BUCKET/AWS_REGION env vars to docker-compose.yml"                      # T004
Task: "Add INVALID_IMAGE_FILE/FILE_TOO_LARGE error codes to IdentityErrorCode.java"      # T005

# Independent of the above, also parallel:
Task: "Install expo-image-picker and configure app.json permissions"                    # T007
```

## Parallel Example: User Story 1 render spots

```bash
# Once AvatarPicker.tsx (T011) exists, these four touch different files:
Task: "Wire AvatarPicker into ProfileViewScreen.tsx"                # T012
Task: "Render photo in HeaderAvatar.tsx"                            # T013
Task: "Render photo in PlayerProfileScreen.tsx"                     # T014
Task: "Render photo in SearchScreen.tsx's PlayerCard"               # T015
Task: "Remove Avatar URL text input from Edit/CreateProfileScreen"  # T016
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001)
2. Complete Phase 2: Foundational (T002-T007) — CRITICAL, blocks everything
3. Complete Phase 3: User Story 1 (T008-T018)
4. **STOP and VALIDATE**: A user with no photo can add one, and it shows up everywhere — this alone delivers the feature's core value
5. User Story 2 (replace) is nearly free at this point since the upload endpoint is already an upsert — worth doing immediately after as it's mostly verification + one label change

### Incremental Delivery

1. Setup + Foundational → S3/image infrastructure ready
2. Add User Story 1 → validate → users can add a photo (MVP)
3. Add User Story 2 → validate → users can replace a photo (mostly already works, confirm + relabel)
4. Add User Story 3 → validate → users can delete a photo
5. Polish → typecheck, backend test suite, full quickstart regression

---

## Notes

- [P] tasks touch different files with no shared incomplete dependency.
- [US1]/[US2]/[US3] labels map tasks to the user stories in spec.md for traceability.
- **Deferred from research.md Decision 6**: the per-user upload cooldown was flagged in planning as "nearly free," but `UserProfile.updatedAt` is shared across *all* profile fields (city, position, bio, etc.), not avatar-specific — using it for an avatar-only cooldown would incorrectly reset on unrelated edits, and a correct implementation would need a new dedicated timestamp column (a schema migration). That's disproportionate for this abuse-guard given the strict per-upload size cap (T003) and owner-only JWT auth already bound the realistic damage; this task list intentionally does **not** implement the cooldown. Revisit only if upload abuse is actually observed.
- **Deliberately out of scope**: `frontend/src/screens/profile/FriendsScreen.tsx`'s friend-list rows do not render the photo — its `FriendEntry` type only carries `displayName` (from `userApi`, not `profileApi`), so adding avatars there would require a backend DTO change to the friendship/user-listing endpoint (a separate concern from avatar storage itself), plus N+1-fetch avoidance. Spec FR-010 names "arkadaş listeleri" but this task list bounds it to the four spots that already have `avatarUrl` in their existing data (HeaderAvatar, ProfileViewScreen, PlayerProfileScreen, SearchScreen) without a new cross-service data change.
- No automated frontend tests exist; T018, T021, T026, T027, T030 are manual verification checkpoints driven by quickstart.md.
- Commit after each task or logical group (e.g., after Foundational; after T008+T009; after T011+T012).
