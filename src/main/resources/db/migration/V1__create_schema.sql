-- =====================================================================
-- V1: Base schema for Si-Teja
-- =====================================================================
-- karakter        : candidate characters the game can guess
-- question        : yes/no trait-questions the engine can ask
-- karakter_trait   : join table, one row per (karakter, question) pair,
--                    carrying the curated P(YES | karakter) probability
--                    that used to be hardcoded as Java Map literals.
-- =====================================================================

CREATE TABLE karakter (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE question (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    text VARCHAR(500) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE karakter_trait (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    karakter_id   BIGINT NOT NULL,
    question_id   BIGINT NOT NULL,
    probability   DOUBLE NOT NULL,
    CONSTRAINT fk_trait_karakter FOREIGN KEY (karakter_id) REFERENCES karakter(id) ON DELETE CASCADE,
    CONSTRAINT fk_trait_question FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE,
    CONSTRAINT uq_karakter_question UNIQUE (karakter_id, question_id),
    -- Enforced on MySQL 8.0.16+ (earlier versions parse but silently ignore
    -- CHECK constraints). The engine's own Bayes math is written to tolerate
    -- 0.0/1.0 edge cases gracefully regardless, but keeping curated data
    -- inside (0, 1) is what preserves the "no hard elimination" guarantee.
    CONSTRAINT chk_probability_range CHECK (probability > 0.0 AND probability < 1.0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_trait_karakter ON karakter_trait(karakter_id);
CREATE INDEX idx_trait_question ON karakter_trait(question_id);
