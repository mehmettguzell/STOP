-- ============================================================
-- CHAT MESSAGES
-- ============================================================
CREATE TABLE chat_messages (
    id         UUID                     PRIMARY KEY,
    match_id   UUID                     NOT NULL,
    sender_id  UUID                     NOT NULL,
    type       VARCHAR(15)              NOT NULL DEFAULT 'TEXT',
    content    VARCHAR(1000),
    poll_id    UUID,
    sent_at    TIMESTAMPTZ              NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_chat_match_id ON chat_messages (match_id, sent_at DESC);

-- ============================================================
-- DIRECT CHATS
-- ============================================================
CREATE TABLE direct_chats (
    id         UUID        PRIMARY KEY,
    user1_id   UUID        NOT NULL,
    user2_id   UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_direct_chat_pair UNIQUE (user1_id, user2_id),
    CONSTRAINT chk_no_self_chat    CHECK  (user1_id <> user2_id)
);

-- ============================================================
-- MATCH CHAT PARTICIPANTS
-- ============================================================
CREATE TABLE match_chat_participants (
    match_id  UUID        NOT NULL,
    user_id   UUID        NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (match_id, user_id)
);

CREATE INDEX idx_mcp_user ON match_chat_participants (user_id);

-- ============================================================
-- CHAT READS
-- ============================================================
CREATE TABLE chat_reads (
    user_id      UUID        NOT NULL,
    chat_id      UUID        NOT NULL,
    last_read_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, chat_id)
);

-- ============================================================
-- MATCH CHAT LOCKS
-- ============================================================
CREATE TABLE match_chat_locks (
    match_id     UUID                     NOT NULL PRIMARY KEY,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- ============================================================
-- HIDDEN CONVERSATIONS
-- ============================================================
CREATE TABLE hidden_conversations (
    id      UUID NOT NULL PRIMARY KEY,
    user_id UUID NOT NULL,
    chat_id UUID NOT NULL,
    CONSTRAINT uq_hidden_user_chat UNIQUE (user_id, chat_id)
);

-- ============================================================
-- POLLS
-- ============================================================
CREATE TABLE polls (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id    UUID         NOT NULL,
    creator_id UUID         NOT NULL,
    question   VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    closes_at  TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE poll_options (
    id       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id  UUID         NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    text     VARCHAR(200) NOT NULL,
    position INTEGER      NOT NULL
);

CREATE TABLE poll_votes (
    id        UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id   UUID    NOT NULL,
    option_id UUID    NOT NULL REFERENCES poll_options(id) ON DELETE CASCADE,
    user_id   UUID    NOT NULL,
    voted_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_poll_user UNIQUE (poll_id, user_id)
);

CREATE INDEX idx_polls_chat        ON polls (chat_id);
CREATE INDEX idx_poll_options_poll ON poll_options (poll_id);
CREATE INDEX idx_poll_votes_poll   ON poll_votes (poll_id);
CREATE INDEX idx_poll_votes_option ON poll_votes (option_id);
