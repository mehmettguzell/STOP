-- ============================================================
-- MATCHES
-- ============================================================
CREATE TABLE matches (
    id                UUID         PRIMARY KEY,
    title             VARCHAR(150) NOT NULL,
    description       TEXT,
    location          VARCHAR(255) NOT NULL,
    start_time        TIMESTAMP    NOT NULL,
    visibility        VARCHAR(20)  NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    organizer_id      UUID         NOT NULL,
    capacity          INTEGER      NOT NULL,
    participant_count INTEGER      NOT NULL DEFAULT 0,
    rating_count      INTEGER      NOT NULL DEFAULT 0,
    home_score        INTEGER,
    away_score        INTEGER,
    rating_deadline   TIMESTAMP,
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL
);

CREATE INDEX idx_matches_start_time ON matches (start_time);
CREATE INDEX idx_matches_status     ON matches (status);
CREATE INDEX idx_matches_location   ON matches (location);
CREATE INDEX idx_matches_visibility ON matches (visibility);

-- ============================================================
-- MATCH PARTICIPANTS
-- ============================================================
CREATE TABLE match_participants (
    id          UUID        PRIMARY KEY,
    match_id    UUID        NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL,
    status      VARCHAR(20) NOT NULL,
    team        VARCHAR(5)  CHECK (team IN ('HOME', 'AWAY')),
    joined_at   TIMESTAMP   NOT NULL,
    position_x  FLOAT,
    position_y  FLOAT
);

CREATE UNIQUE INDEX uq_match_participant   ON match_participants (match_id, user_id);
CREATE INDEX idx_participants_match        ON match_participants (match_id);
CREATE INDEX idx_participants_user         ON match_participants (user_id);
CREATE INDEX idx_participants_match_status ON match_participants (match_id, status);

-- ============================================================
-- PARTICIPATION REQUESTS
-- ============================================================
CREATE TABLE participation_requests (
    id         UUID        PRIMARY KEY,
    match_id   UUID        NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL,
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMP   NOT NULL
);

CREATE UNIQUE INDEX uq_pending_request
    ON participation_requests (match_id, user_id)
    WHERE status = 'PENDING';

CREATE INDEX idx_requests_match_status ON participation_requests (match_id, status);

-- ============================================================
-- INVITATIONS
-- ============================================================
CREATE TABLE invitations (
    id          UUID        PRIMARY KEY,
    match_id    UUID        NOT NULL,
    sender_id   UUID        NOT NULL,
    receiver_id UUID        NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_pending_invitation
    ON invitations (match_id, receiver_id)
    WHERE status = 'PENDING';

CREATE INDEX idx_invitations_receiver ON invitations (receiver_id);
CREATE INDEX idx_invitations_match    ON invitations (match_id);

-- ============================================================
-- MATCH RATINGS
-- ============================================================
CREATE TABLE match_ratings (
    id            UUID      PRIMARY KEY,
    match_id      UUID      NOT NULL,
    rater_id      UUID      NOT NULL,
    rated_id      UUID      NOT NULL,
    skill         INTEGER   NOT NULL CHECK (skill BETWEEN 1 AND 5),
    teamwork      INTEGER   NOT NULL CHECK (teamwork BETWEEN 1 AND 5),
    sportsmanship INTEGER   NOT NULL CHECK (sportsmanship BETWEEN 1 AND 5),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_not_self_rate CHECK (rater_id <> rated_id),
    CONSTRAINT uq_match_rating   UNIQUE (match_id, rater_id, rated_id)
);

CREATE INDEX idx_ratings_match       ON match_ratings (match_id);
CREATE INDEX idx_ratings_rated       ON match_ratings (rated_id);
CREATE INDEX idx_ratings_match_rater ON match_ratings (match_id, rater_id);

-- ============================================================
-- SEED
-- ============================================================
-- ADMIN : 00000000-0000-0000-0000-000000000001  (memozeko@example.com)
-- USER1 : 00000000-0000-0000-0000-000000000002  (user@example.com)
-- ============================================================

INSERT INTO matches (id, title, description, location, start_time, visibility, status,
                     organizer_id, capacity, participant_count, version, created_at, updated_at)
VALUES
    ('10000000-0000-0000-0000-000000000001',
     'Salı Akşamı Maçı', 'Herkes davetli!', 'Bostancı Spor Tesisi',
     NOW() + INTERVAL '3 days', 'PUBLIC', 'OPEN',
     '00000000-0000-0000-0000-000000000002', 10, 2, 0, NOW(), NOW()),

    ('10000000-0000-0000-0000-000000000002',
     'Çarşamba Rekabeti', 'Ciddi maç!', 'Kadıköy Sahası',
     NOW() - INTERVAL '1 hour', 'PUBLIC', 'STARTED',
     '00000000-0000-0000-0000-000000000001', 8, 2, 0, NOW(), NOW()),

    ('10000000-0000-0000-0000-000000000003',
     'Geçen Haftanın Maçı', NULL, 'Beşiktaş Sahası',
     NOW() - INTERVAL '8 days', 'PUBLIC', 'RATINGS_CLOSED',
     '00000000-0000-0000-0000-000000000001', 6, 2, 0,
     NOW() - INTERVAL '8 days', NOW() - INTERVAL '7 days');

UPDATE matches
SET home_score = 3, away_score = 1, rating_deadline = NOW() - INTERVAL '1 day'
WHERE id = '10000000-0000-0000-0000-000000000003';

INSERT INTO match_participants (id, match_id, user_id, status, joined_at)
VALUES
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', 'JOINED', NOW()),
    ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'JOINED', NOW());

INSERT INTO match_participants (id, match_id, user_id, status, joined_at, team)
VALUES
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'JOINED', NOW(), 'HOME'),
    ('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002', 'JOINED', NOW(), 'AWAY'),

    ('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'JOINED', NOW() - INTERVAL '8 days', 'HOME'),
    ('20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002', 'JOINED', NOW() - INTERVAL '8 days', 'AWAY');

INSERT INTO match_ratings (id, match_id, rater_id, rated_id, skill, teamwork, sportsmanship, created_at)
VALUES
    ('40000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', 4, 4, 5, NOW() - INTERVAL '7 days'),
    ('40000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 5, 5, 5, NOW() - INTERVAL '7 days');
