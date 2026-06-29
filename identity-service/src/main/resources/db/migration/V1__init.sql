-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id             UUID         PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    phone_number   VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    display_name   VARCHAR(255) NOT NULL UNIQUE,
    role           VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    phone_verified BOOLEAN      NOT NULL DEFAULT FALSE,
    trust_score    DECIMAL(3,1) NOT NULL DEFAULT 7.0,
    rank_score     DECIMAL(3,1) NOT NULL DEFAULT 7.0,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL
);

CREATE INDEX idx_users_status      ON users (status);
CREATE INDEX idx_users_role        ON users (role);
CREATE INDEX idx_users_trust_score ON users (trust_score DESC);
CREATE INDEX idx_users_rank_score  ON users (rank_score DESC);

-- ============================================================
-- REFRESH TOKENS
-- ============================================================
CREATE TABLE refresh_token (
    id         UUID         PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP
);

CREATE INDEX idx_refresh_token_hash    ON refresh_token (token_hash);
CREATE INDEX idx_refresh_token_user_id ON refresh_token (user_id);

-- ============================================================
-- USER PROFILES
-- ============================================================
CREATE TABLE user_profiles (
    user_id       UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    first_name    VARCHAR(255),
    last_name     VARCHAR(255),
    birth_date    DATE,
    city          VARCHAR(255),
    position      VARCHAR(255),
    height_cm     INTEGER,
    weight_kg     INTEGER,
    dominant_foot VARCHAR(50),
    bio           TEXT,
    avatar_url    VARCHAR(512),
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL
);

CREATE INDEX idx_user_profiles_city     ON user_profiles (lower(city));
CREATE INDEX idx_user_profiles_position ON user_profiles (lower(position));

-- ============================================================
-- TRUST SCORE EVENTS
-- ============================================================
CREATE TABLE trust_score_events (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL,
    event_type  VARCHAR(50)  NOT NULL,
    delta       DECIMAL(3,1) NOT NULL,
    score_after DECIMAL(3,1) NOT NULL,
    source_id   UUID,
    source_type VARCHAR(50),
    changed_by  UUID,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_trust_user_time ON trust_score_events (user_id, created_at DESC);

-- ============================================================
-- RANK SCORE EVENTS
-- ============================================================
CREATE TABLE rank_score_events (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL,
    event_type  VARCHAR(50)  NOT NULL,
    delta       DECIMAL(3,1) NOT NULL,
    score_after DECIMAL(3,1) NOT NULL,
    source_id   UUID,
    source_type VARCHAR(50),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rank_user_time ON rank_score_events (user_id, created_at DESC);

-- ============================================================
-- FRIENDSHIPS
-- ============================================================
CREATE TABLE friendships (
    id           UUID        PRIMARY KEY,
    requester_id UUID        NOT NULL,
    receiver_id  UUID        NOT NULL,
    status       VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'BLOCKED')),
    created_at   TIMESTAMP   NOT NULL
);

CREATE UNIQUE INDEX uq_friendship_pair
    ON friendships (LEAST(requester_id, receiver_id), GREATEST(requester_id, receiver_id));

CREATE INDEX idx_friendships_requester      ON friendships (requester_id);
CREATE INDEX idx_friendships_receiver       ON friendships (receiver_id);
CREATE INDEX idx_friendships_status         ON friendships (status);
CREATE INDEX idx_friendships_requester_recv ON friendships (requester_id, receiver_id);
CREATE INDEX idx_friendships_receiver_req   ON friendships (receiver_id, requester_id);

-- ============================================================
-- REPORTS
-- ============================================================
CREATE TABLE reports (
    id             UUID        PRIMARY KEY,
    reporter_id    UUID        NOT NULL,
    target_user_id UUID        NOT NULL,
    match_id       UUID,
    reason         TEXT        NOT NULL,
    status         VARCHAR(20) NOT NULL CHECK (status IN ('OPEN', 'REVIEWING', 'RESOLVED', 'REJECTED')),
    resolved_by    UUID,
    resolved_at    TIMESTAMP,
    resolve_note   TEXT,
    created_at     TIMESTAMP   NOT NULL
);

CREATE INDEX idx_reports_target         ON reports (target_user_id);
CREATE INDEX idx_reports_status_created ON reports (status, created_at DESC);

-- ============================================================
-- PASSWORD RESET TOKENS
-- ============================================================
CREATE TABLE password_reset_tokens (
    id         UUID         PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64)  NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_prt_user_id ON password_reset_tokens (user_id);
