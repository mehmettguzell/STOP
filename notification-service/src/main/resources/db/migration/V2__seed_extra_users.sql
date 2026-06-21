-- ============================================================
-- SEED — Notifications for user2 & user3
-- ============================================================
-- USER2 : 00000000-0000-0000-0000-000000000003  (user2@example.com)
-- USER3 : 00000000-0000-0000-0000-000000000004  (user3@example.com)
-- ============================================================

INSERT INTO notifications (id, user_id, type, title, message, reference_id, is_read, created_at)
VALUES
    -- USER2 notifications
    ('50000000-0000-0000-0000-000000000005',
     '00000000-0000-0000-0000-000000000003',
     'FRIEND_REQUEST_ACCEPTED', 'Arkadaşlık Kabul Edildi', 'Memozeko arkadaşlık isteğini kabul etti.',
     '00000000-0000-0000-0000-000000000001', true, NOW() - INTERVAL '7 days'),

    ('50000000-0000-0000-0000-000000000006',
     '00000000-0000-0000-0000-000000000003',
     'PARTICIPANT_JOINED', 'Yeni Katılımcı', 'Cuma Akşamı Maçı maçına yeni bir oyuncu katıldı.',
     '10000000-0000-0000-0000-000000000004', false, NOW() - INTERVAL '1 hour'),

    ('50000000-0000-0000-0000-000000000007',
     '00000000-0000-0000-0000-000000000003',
     'MATCH_COMPLETED', 'Maç Tamamlandı', 'Perşembe Rekabeti maçı sona erdi! Takım arkadaşlarını değerlendirmeyi unutma.',
     '10000000-0000-0000-0000-000000000005', false, NOW() - INTERVAL '3 days'),

    -- USER3 notifications
    ('50000000-0000-0000-0000-000000000008',
     '00000000-0000-0000-0000-000000000004',
     'FRIEND_REQUEST_RECEIVED', 'Arkadaşlık İsteği', 'Memozeko sana arkadaşlık isteği gönderdi.',
     '00000000-0000-0000-0000-000000000001', true, NOW() - INTERVAL '3 days'),

    ('50000000-0000-0000-0000-000000000009',
     '00000000-0000-0000-0000-000000000004',
     'MATCH_COMPLETED', 'Maç Tamamlandı', 'Perşembe Rekabeti maçı sona erdi! Takım arkadaşlarını değerlendirmeyi unutma.',
     '10000000-0000-0000-0000-000000000005', false, NOW() - INTERVAL '3 days'),

    ('50000000-0000-0000-0000-000000000010',
     '00000000-0000-0000-0000-000000000004',
     'PARTICIPANT_JOINED', 'Maça Katıldın', 'Cuma Akşamı Maçı maçına başarıyla katıldın.',
     '10000000-0000-0000-0000-000000000004', false, NOW() - INTERVAL '2 hours'),

    -- ADMIN için de ek bildirimler
    ('50000000-0000-0000-0000-000000000011',
     '00000000-0000-0000-0000-000000000001',
     'MATCH_COMPLETED', 'Maç Tamamlandı', 'Perşembe Rekabeti maçı sona erdi! Takım arkadaşlarını değerlendirmeyi unutma.',
     '10000000-0000-0000-0000-000000000005', false, NOW() - INTERVAL '3 days'),

    ('50000000-0000-0000-0000-000000000012',
     '00000000-0000-0000-0000-000000000001',
     'PARTICIPANT_JOINED', 'Yeni Katılımcı', 'Cuma Akşamı Maçı maçına yeni bir oyuncu katıldı.',
     '10000000-0000-0000-0000-000000000004', false, NOW() - INTERVAL '1 hour');
