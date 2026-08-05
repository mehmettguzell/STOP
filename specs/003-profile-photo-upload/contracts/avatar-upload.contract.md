# Contract: Avatar Upload/Delete API

Base path: existing `/api/v1/users/profile` (`UserProfileController`). Two new endpoints, both
authenticated via the existing JWT bearer flow (`SecurityUtils.getCurrentUserId()` — no
client-supplied user id is ever trusted).

## `POST /api/v1/users/profile/avatar`

Uploads a new photo — used for both "add" (spec US1) and "replace" (spec US2); the server behaves
identically either way (upsert), since research.md Decision 5 always deletes whatever the previous
object was (none, in the "add" case).

**Request**: `multipart/form-data`, one part named `file`, containing a JPEG or PNG image.

- Max size: enforced server-side via Spring multipart config (research.md Decision 3) — requests
  exceeding it are rejected with `413`-equivalent behavior before the body is fully read.

**Response `200 OK`**:

```json
{
  "avatarUrl": "https://<bucket>.s3.<region>.amazonaws.com/avatars/<random-key>.jpg"
}
```

(Or the full `UserProfileResponse` if simpler to return the whole updated profile — either shape
is acceptable at implementation time; the contract requirement is that the client receives the new
`avatarUrl` to update its UI immediately.)

**Error responses** (via existing `GlobalExceptionHandler` + `IdentityErrorCode`, spec FR-005):

| Condition | Behavior |
|---|---|
| File is not a decodable JPEG/PNG (per research.md Decision 2) | `400`-equivalent, a new `IdentityErrorCode` (e.g. `INVALID_IMAGE_FILE`); `avatarUrl` unchanged |
| File exceeds max size | `400`/`413`-equivalent, a new `IdentityErrorCode` (e.g. `FILE_TOO_LARGE`); `avatarUrl` unchanged |
| Caller not authenticated | `401`, existing behavior |
| Cooldown not elapsed since last upload (research.md Decision 6) | `429`-equivalent, a new `IdentityErrorCode` (e.g. `UPLOAD_TOO_FREQUENT`) |

**Side effects**: The previous S3 object (if any) for this user is deleted as part of the same
operation (spec FR-006). No local disk write occurs on `identity-service` at any point (research.md
Decision 3).

## `DELETE /api/v1/users/profile/avatar`

Removes the current photo (spec US3) — requires the client to have already shown a confirmation
prompt (FR-007); the server does not re-confirm, it executes the deletion when called.

**Request**: No body.

**Response `200 OK`**: Updated profile (or `{ "avatarUrl": null }`) reflecting `avatarUrl` cleared.

**Error responses**:

| Condition | Behavior |
|---|---|
| Caller not authenticated | `401` |
| No photo currently set | `200` no-op, or `404`-equivalent — implementation's choice; either is acceptable as long as it doesn't error the client's UI (deleting an already-absent photo should not surface as a hard failure) |

**Side effects**: The current S3 object is deleted; `UserProfile.avatarUrl` set to `null`
(spec FR-008).

## Non-goals

- No endpoint to list or fetch photo *history* — only the current photo exists (research.md
  Decision 5: old objects are deleted, not archived).
- No separate "get avatar" endpoint — reads happen directly against the S3 URL already present in
  `UserProfileResponse.avatarUrl` (research.md Decision 4); these two endpoints only ever handle
  writes.
- No endpoint lets a caller act on another user's photo — both endpoints operate exclusively on
  `SecurityUtils.getCurrentUserId()`'s own profile (spec FR-009).
