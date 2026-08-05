# Phase 0 Research: Profil Fotoğrafı Yükleme, Değiştirme ve Silme

Driven directly by the three concerns raised for this plan: (1) storage/path approach, (2)
file-upload vulnerability protection, (3) never bloating the (single, shared) EC2 host or hurting
its performance.

## Decision 1: Where the photo bytes are stored

- **Decision**: A new AWS S3 bucket (external, AWS-managed object storage). `identity-service`
  uploads the processed image to S3 and stores only the resulting object URL as a string in the
  existing `UserProfile.avatar_url` column (`identity-service/.../entity/profile/UserProfile.java`,
  already `VARCHAR(512)`) — no database migration needed.
- **Rationale**: The entire backend runs as five containers on **one** EC2 host via
  `docker-compose.yml` (constitution, Technology & Deployment Constraints). Any option that writes
  image bytes to that host's local disk (a bind-mounted `uploads/` folder, a database `bytea`
  column, etc.) means storage grows without bound as users add photos, competing with the disk the
  Postgres containers, Kafka, and the JVMs themselves depend on — this is precisely the
  "sunucuyu şişirmemeli" (must not bloat the server) failure mode this plan was asked to avoid, and
  it degrades over time in a way that's invisible until the disk is nearly full. S3 moves that
  growth entirely off the host, onto storage that scales independently and costs nothing in host
  disk/IO/CPU.
- **Alternatives considered**:
  - **Local disk on the EC2 host** (bind mount, served via a static-file route). Rejected — this is
    exactly the growth-on-the-shared-host problem above, plus it would require `identity-service`
    (or `api-gateway`) to serve every single image *read* itself, adding request load/bandwidth to
    the host for what will be its most frequent operation (see Decision 3).
  - **Store the image as bytes in Postgres** (`bytea` column). Rejected — same unbounded-growth
    problem, but worse: it also bloats `identity-db`'s own storage and backup size, and turns cheap
    read-only image serving into expensive database queries.
  - **Self-hosted S3-compatible object storage (e.g. MinIO) as a new Docker Compose service.**
    Rejected — it would still run as a container *on the same EC2 host*, still consuming that
    host's disk as photos accumulate, and per the constitution's Technology & Deployment
    Constraints it would need its own resource-limit anchor (`x-*-resources`) like the other five
    services — more operational surface for no benefit over managed S3, which needs none of that.
  - **A third-party image CDN/hosting service** (e.g. Cloudinary). Rejected — introduces a new
    external vendor and credential set with no clear advantage over S3 given the project's existing
    AWS footprint (Secrets Manager, SSM, EC2, previously ECR); S3 is the option that requires the
    least *new* operational trust.

## Decision 2: Accepted input, validation, and re-encoding approach

- **Decision**: Accept JPEG and PNG uploads only. On receipt, `identity-service` decodes the bytes
  using the JDK's built-in `javax.imageio.ImageIO` (no new library) into an in-memory
  `BufferedImage`. If decoding fails, the upload is rejected (FR-005) — this is the real
  content check, not a trust of the `Content-Type` header or file extension. If decoding succeeds,
  the image is downscaled (if needed) to a fixed maximum dimension and re-encoded to a single fixed
  output format (JPEG) before being uploaded to S3. The original uploaded bytes are never stored or
  forwarded anywhere — only the re-encoded output is.
- **Rationale**: This is the single most effective mitigation against file-upload vulnerabilities
  for an *image* upload feature specifically. A file extension or `Content-Type` header is
  attacker-controlled and trivially spoofed (e.g. a `.exe` or a polyglot file renamed
  `photo.jpg`); actually decoding it as a raster image and re-encoding the decoded pixels discards
  everything that isn't genuine image data — embedded scripts, oversized/malformed metadata,
  polyglot payloads, decompression-bomb structures that don't survive a normal decode/re-encode
  round-trip. It also caps worst-case output size (fixed max dimensions), which directly serves
  the "must not bloat storage" goal regardless of what a user uploads.
- **Alternatives considered**:
  - **Trust `Content-Type`/file extension only.** Rejected — this is the exact vulnerability class
    being guarded against; both are attacker-controlled and prove nothing about actual file
    content.
  - **Store the raw uploaded bytes as-is (no re-encode).** Rejected — even after verifying it
    decodes as an image, the original file may still carry crafted metadata/structure the decoder
    tolerates but that a different downstream consumer (browser, other library) might not; re-saving
    the decoded pixels is what actually normalizes the file to something known-safe.
  - **Add a dedicated image-processing library** (e.g. Thumbnailator, imgscalr) instead of raw
    `ImageIO`. Rejected for now — `ImageIO`'s built-in JPEG/PNG codecs cover exactly the two
    formats this feature accepts; adding a library for resize convenience isn't justified when the
    JDK API already does decode + scale (`Graphics2D`) + encode without one.
  - **Full malware/antivirus scanning of uploads.** Rejected as disproportionate for this feature's
    threat model and this project's scale — there's no existing scanning infrastructure anywhere in
    the repo, and the decode/re-encode step already defeats the realistic "disguised executable"
    threat for an image-only upload surface. Noted as a possible future hardening step, not a
    requirement here.

## Decision 3: Upload path and size limits

- **Decision**: The client uploads the original image to `identity-service` via a single
  multipart `POST`, capped by a strict server-side max request/file size (a few megabytes — the
  exact number is a config value, not a product decision, and is set in `application.yml`'s
  Spring multipart config so oversized requests are rejected before being fully buffered).
  `identity-service` validates + re-encodes (Decision 2) + uploads to S3 + discards its own copy of
  the bytes, all within that one request — nothing is written to local disk at any point (in
  memory only, using JDK APIs against byte arrays/streams).
- **Rationale**: This keeps the "must not bloat the server" property true not just for storage but
  for the *upload* request path too: a hard, small cap bounds the CPU/memory/network cost of every
  upload regardless of what a client sends, and "no local disk write" means there's no
  temp-file cleanup burden or leftover-file risk on the host either.
- **Alternatives considered**:
  - **Client uploads directly to S3 via a presigned URL that `identity-service` generates**
    (backend never sees the bytes). This is more network/CPU-efficient for the *upload* step (the
    EC2 host handles zero bytes of the file), but it means the backend can never perform the
    decode/re-encode validation from Decision 2 — the exact mitigation this plan is built around
    would either have to be dropped (unacceptable given the explicit anti-file-upload-vulnerability
    requirement) or done asynchronously after the fact (via an S3 event trigger + a re-processing
    step), which needs infrastructure (event notifications, a consumer) this project doesn't have
    and that is disproportionate to add for a single-avatar-per-user feature. Rejected for this
    feature's scope; revisit only if upload volume ever makes the current path measurably expensive.
  - **Store intermediate uploads on local disk before shipping to S3.** Rejected — reintroduces the
    exact "disk accumulation on the shared host" risk this plan avoids, even if meant to be
    "temporary" (crashes/restarts can leave orphaned temp files).

## Decision 4: Serving (read) path

- **Decision**: Photos are served directly from S3 object URLs — every screen that shows a photo
  (`ProfileViewScreen`, `PlayerProfileScreen`, `HeaderAvatar`, search result cards) uses the stored
  `avatarUrl` directly as the `Image` source. `identity-service`/`api-gateway` are never in the
  request path for viewing a photo, only for the (rare, write-only) upload/delete actions.
- **Rationale**: Reads vastly outnumber writes for this feature — a photo is uploaded/changed
  occasionally but *viewed* on every profile visit, every search result, every friend list render.
  Proxying reads through the single EC2 host would mean the feature's most frequent operation adds
  request load and bandwidth cost to the host doing everything else (Kafka, four other services,
  Postgres) — the opposite of the "sunucunun performansı için en doğru yol" (the right choice for
  server performance) this plan asked for. Serving directly from S3 keeps that load at zero.
- **Alternatives considered**:
  - **Proxy image reads through `identity-service`/`api-gateway`** (e.g. to hide the S3 URL or add
    access control). Rejected — a profile photo, once set, is intended to be visible to anyone who
    can already view that profile (there is no additional privacy tier beyond "own profile can
    edit, everyone can view", per spec FR-009/FR-010); proxying buys no real access-control benefit
    here while directly reintroducing the host-load problem Decision 1/3 were designed to avoid.
    Object keys are server-generated random values (Decision 5), so the bucket cannot be
    meaningfully enumerated even though objects are public-read.

## Decision 5: Object key generation and old-photo cleanup

- **Decision**: `identity-service` generates a random, opaque object key (e.g. a UUID-based path)
  server-side for every upload — the client-supplied filename is never used for anything beyond
  informational purposes and never becomes part of the storage key. On replace or delete, the
  previous S3 object (if any) is deleted as part of the same operation, so a user has at most one
  stored object at a time — not an accumulating history.
- **Rationale**: A server-generated key is standard, low-cost protection against path-traversal and
  overwrite attacks that trusting a client-supplied filename would otherwise invite (FR-006/FR-008
  also require the old photo to become fully inaccessible on replace/delete, which a stable
  server-generated-and-then-deleted key satisfies directly). Deleting the old object on
  replace/delete keeps storage bounded to "one object per user with a photo," reinforcing Decision
  1's goal — no unbounded growth even over many replace cycles for the same user.
- **Alternatives considered**:
  - **Use the client-supplied filename directly.** Rejected — classic path-traversal/overwrite
    vulnerability class, and it also does nothing to guarantee uniqueness across users.
  - **Keep old objects around (versioned/history) instead of deleting.** Rejected — spec explicitly
    requires the old photo become fully inaccessible (FR-006), and keeping history reintroduces
    unbounded storage growth per user over time, which is the opposite of Decision 1's goal.

## Decision 6: Authorization and abuse limits

- **Decision**: Both the upload and delete endpoints resolve the acting user from
  `SecurityUtils.getCurrentUserId()` (the validated JWT), identical to every other
  `UserProfileController` endpoint — never a client-supplied user id. A lightweight per-user cooldown
  (e.g. reject a new upload within N seconds of the previous one, checked against the existing
  `UserProfile.updatedAt`/similar timestamp already available — no new infrastructure) is applied to
  blunt rapid repeated-upload abuse.
- **Rationale**: Owner-only enforcement is a direct, no-new-risk application of the constitution's
  existing zero-trust JWT principle (Principle III) and the existing controller pattern — nothing
  new to justify. The cooldown is a cheap, no-new-dependency guard against a user (or a compromised
  client) hammering the upload endpoint to inflate S3 request volume/cost — a lighter-weight
  concern than the host-CPU/disk risks Decisions 1-3 address, but worth a minimal guard given it's
  nearly free to add.
- **Alternatives considered**:
  - **A dedicated rate-limiting library/infrastructure (e.g. Bucket4j, Redis-backed token
    buckets).** Rejected as disproportionate — no rate-limiting infrastructure exists anywhere in
    this codebase today, and a simple timestamp-based cooldown check achieves the practical goal
    (bound upload frequency) without adding a new dependency or Redis usage pattern for a single
    endpoint.

## Decision 7: Cross-service impact

- **Decision**: No new Kafka event is introduced for avatar changes.
- **Rationale**: Confirmed by direct inspection that `UserProfileService` (the `userProfile`
  domain) does not publish any Kafka event for its existing field updates (city, position, etc.)
  today — only the separate `user` domain's `UserService` publishes `UserUpdatedEvent`, and no
  other service reads or caches `avatarUrl`. Adding a new event here would be new complexity with
  no consumer, contradicting Constitution Principle II's requirement that synchronous/async
  integration be justified by genuine need.
- **Alternatives considered**: Emitting a Kafka event so other services could react to avatar
  changes in the future. Rejected as speculative/unjustified — no current requirement or consumer
  exists; can be added later if a real need arises, per the constitution's own framing of Kafka as
  justified by genuine need rather than default.
