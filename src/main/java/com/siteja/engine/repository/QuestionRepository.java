package com.siteja.engine.repository;

import com.siteja.engine.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Question}. The question bank is whatever rows
 * exist in the `question` table — no hardcoded question list in Java.
 */
public interface QuestionRepository extends JpaRepository<Question, Long> {
}
