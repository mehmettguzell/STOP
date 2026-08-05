# Contract: Avatar Object Storage (S3)

Internal contract between `identity-service` and the new S3 bucket — not client-facing, but
documented because it encodes the security/performance decisions from research.md and must be
honored by whichever task implements the `AvatarStorageService`.

## Bucket configuration

- One new S3 bucket, region/name provided via new environment variables (e.g. `AWS_S3_BUCKET`,
  `AWS_REGION`) added to `identity-service`'s `docker-compose.yml` environment block, alongside its
  existing `DB_URL`/`JWT_PUBLIC_KEY`-style env vars.
- Credentials: **none configured explicitly** — the AWS SDK's default credential provider chain
  resolves credentials from the EC2 instance's IAM role at runtime (research.md Decision 6 /
  Constitution Principle V). No access key/secret appears in `docker-compose.yml`, `.env`, or any
  committed file.
- Object ACL: public-read per object (research.md Decision 4 — reads are served directly from S3,
  no proxy). Bucket-level public *listing* MUST remain disabled — only individual object GETs are
  public, and object keys are random/non-enumerable (research.md Decision 5), so this does not
  expose a browsable photo directory.
- Write access: PUT/DELETE restricted to the IAM role/policy attached to the EC2 instance running
  `identity-service` — no other principal can write to the bucket.

## Object key scheme

```text
avatars/<random-uuid>.jpg
```

- `<random-uuid>` is generated server-side per upload (research.md Decision 5) — never derived
  from the client-supplied filename, the user's id, or any other predictable value (predictable
  keys would let one user guess/overwrite another's object).
- Fixed `.jpg` extension — the re-encode step (research.md Decision 2) always outputs JPEG
  regardless of input format, so the key's extension is never attacker-influenced either.

## Upload contract (`AvatarStorageService` → S3)

- Input: an in-memory re-encoded JPEG byte array (already validated/re-encoded per research.md
  Decision 2) plus the target user's id (used only for the delete-old-object step, not for key
  generation).
- Operation: a single S3 `PutObject` call with a freshly generated key; on success, delete the
  previous object for this user (if `UserProfile.avatarUrl` was previously non-null) via a single
  S3 `DeleteObject` call.
- Failure handling: if the `PutObject` call fails, the operation is aborted and `UserProfile` is
  **not** updated (no partial state — spec Edge Cases). If the subsequent old-object `DeleteObject`
  call fails, this is logged but does not fail the overall request — an orphaned old object is a
  minor storage-cost concern, not a correctness or security one, and must not block the user's
  successful upload of their new photo.

## Delete contract (`AvatarStorageService` → S3)

- Input: the target user's current `avatarUrl` (used to derive the object key to delete).
- Operation: a single S3 `DeleteObject` call, then `UserProfile.avatarUrl` set to `null`.
- Failure handling: if `DeleteObject` fails, the request fails and `avatarUrl` remains unchanged
  (do not clear the database record for an object that might still exist and still be publicly
  reachable — that would be a dangling-reference/inconsistency bug, not just a storage-cost one).

## Non-goals

- No lifecycle/expiration policy is required (research.md Decision 5 — at most one object per user
  at any time, actively deleted on replace/delete rather than left to expire).
- No CDN/CloudFront layer in front of the bucket for this feature — direct S3 URLs are sufficient
  given the scale (one small image per user); revisit only if read latency/cost ever becomes a
  measured problem.
