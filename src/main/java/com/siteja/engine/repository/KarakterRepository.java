package com.siteja.engine.repository;

import com.siteja.engine.model.Karakter;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data access for {@link Karakter}. All candidate data comes from the
 * `karakter` table via this repository — nothing is hardcoded in Java.
 */
public interface KarakterRepository extends JpaRepository<Karakter, Long> {

    /**
     * Loads every character together with its traits in a single query
     * (via an entity graph), avoiding the N+1 query problem that would
     * otherwise occur when the engine reads every character's traits
     * for every Entropy/Bayes calculation.
     */
    @EntityGraph(attributePaths = {"traits", "traits.question"})
    List<Karakter> findAll();
}
