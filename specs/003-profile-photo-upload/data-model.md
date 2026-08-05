# Phase 1 Data Model: Profil Fotoğrafı Yükleme, Değiştirme ve Silme

## Entity: Profile Photo (Profil Fotoğrafı)

Not a new database table — an attribute of the existing `UserProfile` entity, backed by external
object storage.

| Field | Type | Notes |
|---|---|---|
| `avatarUrl` | `string \| null` | Existing column, `identity-service/.../entity/profile/UserProfile.java` (`avatar_url VARCHAR(512)`). Now always either `null` (no photo — client renders the initial-letter fallback) or a full S3 object URL pointing at the current, re-encoded JPEG. No longer a user-typed arbitrary URL (see plan.md — the free-text "Avatar URL" input is removed). |
| `objectKey` (implicit) | `string` | Not a separate persisted field — the S3 object key is embedded in `avatarUrl` itself (or derivable from it). Server-generated (random/UUID-based), never derived from a client-supplied filename (research.md Decision 5). |

**Validation rules**:
- An uploaded file MUST decode as a genuine JPEG or PNG image (`ImageIO.read()` succeeds) — see
  research.md Decision 2. Anything that fails to decode is rejected before ever reaching S3.
- An uploaded file MUST NOT exceed the configured max size (a few MB, enforced by Spring's
  multipart config before the request body is even fully read) — see research.md Decision 3.
- The persisted `avatarUrl`, when non-null, always points at an object that was produced by this
  feature's own re-encode step (fixed format, max dimensions) — never at an arbitrary
  externally-hosted URL, closing off the old "type any http(s) URL" input.

**State transitions**:

```text
[no photo]  --(FR-001..004: add)-->        [has photo A]
[has photo A] --(FR-006: replace)-->        [has photo B]   (photo A deleted from S3)
[has photo A] --(FR-007/008: delete)-->     [no photo]      (photo A deleted from S3)
```

There is no intermediate/partial state exposed to other parts of the system: a given upload either
fully succeeds (new `avatarUrl` persisted, old object removed) or fully fails (rejected before any
S3 write, `avatarUrl` unchanged) — see spec Edge Cases (interrupted upload, connection loss).

## Relationships

- **UserProfile.avatarUrl** (existing field) — the only persisted linkage between a user and their
  photo; 1:1, at most one active photo per profile at any time (spec Key Entities: "bir kullanıcının
  aynı anda en fazla bir aktif profil fotoğrafı olabilir").
- **S3 object** (external, not in Postgres) — the actual image bytes, keyed by the random
  server-generated key embedded in `avatarUrl`. Lifecycle is tied 1:1 to `UserProfile.avatarUrl`:
  created on add/replace, deleted on replace (old object)/delete. No other entity references an S3
  object for this feature.

No new entities, no new tables, no new foreign keys. The only schema-adjacent change is a
*semantic* one: `avatar_url` now exclusively holds system-generated S3 URLs rather than
user-supplied arbitrary URLs.
