-- =====================================================================
-- V2: Seed data — 5 candidate characters x 5 trait-questions
-- =====================================================================
-- This replaces the old @PostConstruct KarakterDataInitializer. All
-- domain content (question text, character names, curated trait
-- probabilities) now lives here as data, not as compiled Java.
--
-- Probabilities are deliberately kept strictly between 0.05 and 0.95
-- (see the chk_probability_range constraint in V1) so the Bayesian
-- engine can never assign a hard 0% posterior to any character —
-- this is what keeps "TIDAK" answers tolerant of noisy input.
-- =====================================================================

INSERT INTO question (id, text) VALUES
    (1, 'Apakah tokoh ini berprofesi sebagai dosen/lecturer?'),
    (2, 'Apakah tokoh ini biasa terlihat memakai kacamata?'),
    (3, 'Apakah tokoh ini berkecimpung di bidang teknologi/IT?'),
    (4, 'Apakah tokoh ini sering menjadi pembicara publik/seminar?'),
    (5, 'Apakah usia tokoh ini di bawah 45 tahun?');

INSERT INTO karakter (id, name) VALUES
    (1, 'Prof. Budi Santoso (Dosen Ilmu Komputer)'),
    (2, 'Dr. Siti Rahayu (Dosen & Peneliti AI)'),
    (3, 'Andi Wijaya (Founder Startup Teknologi)'),
    (4, 'Dewi Lestari (Dosen Sastra & Penulis)'),
    (5, 'Rian Pratama (Content Creator & Public Speaker Muda)');

-- karakter_id, question_id, probability = P(YES | karakter is the true answer)

-- Prof. Budi Santoso — senior CS lecturer archetype
INSERT INTO karakter_trait (karakter_id, question_id, probability) VALUES
    (1, 1, 0.95),
    (1, 2, 0.85),
    (1, 3, 0.90),
    (1, 4, 0.70),
    (1, 5, 0.10);

-- Dr. Siti Rahayu — lecturer & AI researcher
INSERT INTO karakter_trait (karakter_id, question_id, probability) VALUES
    (2, 1, 0.90),
    (2, 2, 0.60),
    (2, 3, 0.95),
    (2, 4, 0.80),
    (2, 5, 0.55);

-- Andi Wijaya — young tech founder, not a lecturer
INSERT INTO karakter_trait (karakter_id, question_id, probability) VALUES
    (3, 1, 0.05),
    (3, 2, 0.30),
    (3, 3, 0.95),
    (3, 4, 0.85),
    (3, 5, 0.90);

-- Dewi Lestari — literature lecturer & writer, not a tech field
INSERT INTO karakter_trait (karakter_id, question_id, probability) VALUES
    (4, 1, 0.92),
    (4, 2, 0.55),
    (4, 3, 0.05),
    (4, 4, 0.75),
    (4, 5, 0.35);

-- Rian Pratama — young public-speaking content creator
INSERT INTO karakter_trait (karakter_id, question_id, probability) VALUES
    (5, 1, 0.05),
    (5, 2, 0.15),
    (5, 3, 0.60),
    (5, 4, 0.95),
    (5, 5, 0.95);
