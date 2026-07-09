package com.siteja.engine.model;

import jakarta.persistence.*;

/**
 * Join entity linking a {@link Karakter} to a {@link Question} with a
 * curated probability value — this table IS the trait data that used to
 * be hardcoded as {@code Map<String, Double>} literals in Java.
 * <p>
 * probability = P(YES | this Karakter is the true answer to this Question).
 * Curated in the seed migration strictly inside (0.05, 0.95), never at the
 * hard extremes, so Bayesian updates in TejaEngineService can never
 * produce an absolute-zero posterior for any character (the "tolerance
 * rule" depends on this).
 */
@Entity
@Table(
    name = "karakter_trait",
    uniqueConstraints = @UniqueConstraint(columnNames = {"karakter_id", "question_id"})
)
public class KarakterTrait {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "karakter_id", nullable = false)
    private Karakter karakter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private Double probability;

    protected KarakterTrait() {
        // JPA requires a no-arg constructor.
    }

    public KarakterTrait(Karakter karakter, Question question, Double probability) {
        this.karakter = karakter;
        this.question = question;
        this.probability = probability;
    }

    public Long getId() {
        return id;
    }

    public Karakter getKarakter() {
        return karakter;
    }

    public Question getQuestion() {
        return question;
    }

    public Double getProbability() {
        return probability;
    }

    public void setProbability(Double probability) {
        this.probability = probability;
    }
}
