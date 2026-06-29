-- ============================================================
-- NOTIFICATIONS
-- ============================================================
CREATE TABLE notifications (
    id           UUID         PRIMARY KEY,
    user_id      UUID         NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    title        VARCHAR(200) NOT NULL,
    message      VARCHAR(500) NOT NULL,
    reference_id UUID,
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_notifications_user_id    ON notifications (user_id);
CREATE INDEX idx_notifications_is_read    ON notifications (is_read);
CREATE INDEX idx_notifications_created_at ON notifications (created_at);

-- ============================================================
-- SEED
-- ============================================================
-- ADMIN : 00000000-0000-0000-0000-000000000001  (memozeko@example.com)
-- USER1 : 00000000-0000-0000-0000-000000000002  (user@example.com)
-- ============================================================

INSERT INTO notifications (id, user_id, type, title, message, reference_id, is_read, created_at)
VALUES
    ('50000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000002',
     'MATCH_STARTED', 'Maç Başladı!', 'Katıldığın maç başladı.',
     '10000000-0000-0000-0000-000000000002', false, NOW() - INTERVAL '1 hour'),

    ('50000000-0000-0000-0000-000000000002',
     '00000000-0000-0000-0000-000000000002',
     'MATCH_COMPLETED', 'Maç Tamamlandı', 'Maç sona erdi! Takım arkadaşlarını değerlendirmeyi unutma.',
     '10000000-0000-0000-0000-000000000003', true, NOW() - INTERVAL '7 days'),

    ('50000000-0000-0000-0000-000000000003',
     '00000000-0000-0000-0000-000000000002',
     'MATCH_UPDATED', 'Maç Güncellendi', 'Katıldığın maçın detayları değişti: Salı Akşamı Maçı',
     '10000000-0000-0000-0000-000000000001', false, NOW() - INTERVAL '2 hours'),

    ('50000000-0000-0000-0000-000000000004',
     '00000000-0000-0000-0000-000000000001',
     'PARTICIPANT_JOINED', 'Yeni Katılımcı', 'Maçına yeni bir oyuncu katıldı.',
     '10000000-0000-0000-0000-000000000001', false, NOW() - INTERVAL '30 minutes');
