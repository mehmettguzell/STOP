# Quickstart: Validating Profile Photo Upload/Change/Delete

No automated frontend test suite exists (consistent with prior features in this repo). Backend
service-layer logic gets JUnit 5/Mockito unit tests (Constitution Principle IV) with S3 mocked —
this quickstart is the **end-to-end** validation against a real (or dev) S3 bucket, per
[spec.md](./spec.md)'s acceptance scenarios.

## Prerequisites

- An S3 bucket provisioned per [contracts/avatar-storage.contract.md](./contracts/avatar-storage.contract.md)
  (name/region set via `AWS_S3_BUCKET`/`AWS_REGION`), reachable with credentials available to
  wherever `identity-service` runs locally (a local AWS profile / IAM role, or — for pure local dev
  without touching real AWS — point at a local S3-compatible test double; either way, this
  provisioning step is a deploy/dev-environment prerequisite outside `/speckit-tasks`' automatable
  scope, per plan.md Constraints).
- `identity-service` and `api-gateway` running with the new env vars set.
- At least one seeded user account to sign in as.

## Setup

```bash
cd identity-service && mvn spring-boot:run   # or however the service is normally run locally
cd frontend && npm run web                    # or npm run android / npm run ios
```

## Scenario 1 — Add a photo when none exists (spec User Story 1)

1. Sign in as a user with no profile photo (avatar shows their initial letter).
2. Go to your own profile and tap the avatar.
3. **Expected**: An action sheet opens with a single "Fotoğraf Ekle" option (no "Değiştir"/"Sil" —
   spec AS1 for US1 vs the "has photo" menu in US2 AS1).
4. Choose "Fotoğraf Ekle", pick an image from the gallery (or take one with the camera).
5. **Expected**: An upload progress indicator appears (FR-004), then the avatar updates to show
   the uploaded photo — check this reflects on `ProfileViewScreen`, and anywhere else your own
   avatar renders (`HeaderAvatar`) without needing to restart the app.
6. Have a second account search for/view this user (`SearchScreen` results, `PlayerProfileScreen`).
   **Expected**: The photo appears there too (spec SC-002) — confirming reads are served correctly
   from the stored `avatarUrl`, not just cached client-side.

## Scenario 2 — Replace an existing photo (spec User Story 2)

1. As the same user from Scenario 1 (now has a photo), tap the avatar again.
2. **Expected**: The action sheet now shows "Fotoğrafı Değiştir" and "Fotoğrafı Sil" (not
   "Fotoğraf Ekle").
3. Choose "Fotoğrafı Değiştir", pick a different image.
4. **Expected**: The new photo replaces the old one everywhere it's shown. Separately, verify (via
   AWS console or `aws s3 ls`) that the *previous* object was deleted from the bucket — confirms
   research.md Decision 5's storage-bound guarantee, not just that the UI moved on.

## Scenario 3 — Delete a photo (spec User Story 3)

1. Tap the avatar, choose "Fotoğrafı Sil".
2. **Expected**: A confirmation prompt appears (FR-007) — cancel it once and confirm the photo is
   unchanged (AS3).
3. Reopen and confirm deletion this time.
4. **Expected**: The avatar reverts to the initial-letter fallback everywhere (own profile, other
   users' view of this profile, search results). Verify the S3 object no longer exists.

## Scenario 4 — Rejected uploads don't corrupt state (spec FR-005, Edge Cases)

1. Attempt to upload a non-image file (e.g. rename a `.txt` or `.exe` to `.jpg` and try to select
   it, or use a corrupt/truncated image file).
2. **Expected**: The upload is rejected with a clear error message; the avatar (whatever it was
   before the attempt) is unchanged; no object was created in S3.
3. Attempt to upload a file larger than the configured max size.
4. **Expected**: Rejected before the upload appears to "hang" or partially complete; avatar
   unchanged.

## Scenario 5 — Ownership boundary (spec FR-009, Edge Cases)

1. As User A, view User B's profile (`PlayerProfileScreen`).
2. **Expected**: User B's avatar is visible but not tappable-to-manage — no action sheet opens (the
   add/change/delete affordance only appears on your own profile).

## Scenario 6 — Server load sanity check (plan.md Performance Goals)

1. With a photo set, open the user's profile/search-card view repeatedly (or via several accounts
   simultaneously).
2. **Expected**: These requests hit S3 directly (observable via the image URL's host in browser
   devtools/network inspector being the S3 bucket domain, not the app's own API host) —
   `identity-service`/`api-gateway` logs show no corresponding request traffic for these views,
   confirming research.md Decision 4 (reads never touch the backend).
