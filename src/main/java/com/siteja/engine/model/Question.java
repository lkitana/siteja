package com.siteja.engine.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity for a single yes/no trait-question the engine can ask the
 * visitor. Persisted in the `question` table — the question bank is no
 * longer a hardcoded list, it is whatever rows exist in this table,
 * queried through {@code QuestionRepository}.
 */
@Entity
@Table(name = "question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String text;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<KarakterTrait> traits = new HashSet<>();

    protected Question() {
        // JPA requires a no-arg constructor.
    }

    public Question(String text) {
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Set<KarakterTrait> getTraits() {
        return traits;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Question other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Question{id=" + id + ", text='" + text + "'}";
    }
}
