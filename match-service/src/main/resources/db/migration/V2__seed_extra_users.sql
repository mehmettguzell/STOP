-- ============================================================
-- SEED — Extra users (user2 & user3)
-- ============================================================
-- ADMIN  : 00000000-0000-0000-0000-000000000001  (memozeko@example.com)
-- USER1  : 00000000-0000-0000-0000-000000000002  (user@example.com)
-- USER2  : 00000000-0000-0000-0000-000000000003  (user2@example.com)
-- USER3  : 00000000-0000-0000-0000-000000000004  (user3@example.com)
-- ============================================================

-- Match4: Cuma akşamı maçı — OPEN, gelecekte, USER2 organize ediyor
INSERT INTO matches (id, title, description, location, start_time, visibility, status,
                     organizer_id, capacity, participant_count, version, created_at, updated_at)
VALUES
    ('10000000-0000-0000-0000-000000000004',
     'Cuma Akşamı Maçı', 'Hafta sonu öncesi keyifli maç!', 'Karşıyaka Spor Tesisi',
     NOW() + INTERVAL '5 days', 'PUBLIC', 'OPEN',
     '00000000-0000-0000-0000-000000000003', 10, 3, 0, NOW(), NOW()),

-- Match5: Geçen Perşembe — COMPLETED, tüm 4 kullanıcı katılmış, değerlendirme yapılabilir
    ('10000000-0000-0000-0000-000000000005',
     'Perşembe Rekabeti', 'Hepimiz oradaydık!', 'Nilüfer Haliçspor Sahası',
     NOW() - INTERVAL '3 days', 'PUBLIC', 'COMPLETED',
     '00000000-0000-0000-0000-000000000001', 10, 4, 0,
     NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days');

UPDATE matches
SET home_score = 2, away_score = 3,
    rating_deadline = NOW() + INTERVAL '2 days',
    rating_count = 0
WHERE id = '10000000-0000-0000-0000-000000000005';

-- Match4 participants: USER2 (organizer), ADMIN, USER3
INSERT INTO match_participants (id, match_id, user_id, status, joined_at)
VALUES
    ('20000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000003', 'JOINED', NOW()),
    ('20000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 'JOINED', NOW()),
    ('20000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000004', 'JOINED', NOW());

-- Match5 participants: all 4 users with teams
INSERT INTO match_participants (id, match_id, user_id, status, joined_at, team)
VALUES
    ('20000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001', 'JOINED', NOW() - INTERVAL '3 days', 'HOME'),
    ('20000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000002', 'JOINED', NOW() - INTERVAL '3 days', 'HOME'),
    ('20000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000003', 'JOINED', NOW() - INTERVAL '3 days', 'AWAY'),
    ('20000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000004', 'JOINED', NOW() - INTERVAL '3 days', 'AWAY');
