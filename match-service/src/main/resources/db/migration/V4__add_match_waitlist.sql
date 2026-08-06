-- ============================================================
-- MAÇ BEKLEME LİSTESİ (WAITLIST)
-- ============================================================
CREATE TABLE match_waitlist (
    id         UUID        PRIMARY KEY,
    match_id   UUID        NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL,
    status     VARCHAR(20) NOT NULL,
    sort_order INTEGER     NOT NULL,
    created_at TIMESTAMP   NOT NULL
);

-- Bir kullanıcının bir maç için aynı anda yalnızca tek bir aktif (WAITING) kaydı olabilir.
CREATE UNIQUE INDEX uq_waitlist_waiting
    ON match_waitlist (match_id, user_id)
    WHERE status = 'WAITING';

-- FIFO sıralama (organizatör listesi, promosyon, pozisyon hesaplama) için.
CREATE INDEX idx_waitlist_match_status_order ON match_waitlist (match_id, status, sort_order);
