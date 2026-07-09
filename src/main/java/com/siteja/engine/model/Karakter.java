package com.siteja.engine.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity for a candidate character (a public figure / lecturer / persona)
 * that the game is trying to guess. Persisted in the `karakter` table.
 * <p>
 * Trait probabilities — previously a hardcoded {@code Map<String, Double>}
 * built in a @PostConstruct initializer — now live in the separate
 * {@code karakter_trait} join table (see {@link KarakterTrait}), populated
 * entirely from the database. No character or trait data is compiled into
 * the JAR; everything is queried through {@code KarakterRepository}.
 */
@Entity
@Table(name = "karakter")
public class Karakter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    /**
     * Inverse side of the Karakter <-> Question many-to-many relationship,
     * mediated by KarakterTrait so each pairing can carry its own curated
     * probability. mappedBy points at the "karakter" field on KarakterTrait.
     */
    @OneToMany(mappedBy = "karakter", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<KarakterTrait> traits = new HashSet<>();

    protected Karakter() {
        // JPA requires a no-arg constructor.
    }

    public Karakter(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<KarakterTrait> getTraits() {
        return traits;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Karakter other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Karakter{id=" + id + ", name='" + name + "'}";
    }
}
