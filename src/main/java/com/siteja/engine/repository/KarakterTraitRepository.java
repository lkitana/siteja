package com.siteja.engine.repository;

import com.siteja.engine.model.KarakterTrait;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link KarakterTrait}, the curated (Karakter, Question)
 * probability pairs. Exposed mainly for future admin/CRUD tooling; the
 * game engine itself reads traits via Karakter.getTraits() through the
 * entity graph on KarakterRepository.findAll().
 */
public interface KarakterTraitRepository extends JpaRepository<KarakterTrait, Long> {
}
