package com.siteja.engine.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the full statistical state of ONE ongoing game session.
 * <p>
 * This is a plain in-memory object, intentionally NOT a JPA entity — a
 * game session is transient runtime state, not data the application
 * needs to persist. It is held in TejaEngineService's
 * ConcurrentHashMap&lt;sessionId, GameState&gt; for the lifetime of the JVM.
 * <p>
 * currentProbabilities: Map of Karakter.id -> P(T_k | all Jawaban so far).
 *   This is the running Bayesian posterior distribution over all candidate
 *   characters, keyed by their real database primary key. It starts
 *   uniform (1 / N) in startNewGame() and is updated turn-by-turn in
 *   processAnswer().
 * <p>
 * askedQuestionIds: Question.id values already used in this session, so
 *   getNextBestQuestion() never repeats a question and the Shannon Entropy
 *   scan only considers the remaining, unasked question pool.
 */
public class GameState {

    private String sessionId;

    /** Karakter.id -> current posterior probability P(T_k | J_1...J_n). */
    private Map<Long, Double> currentProbabilities;

    /** Question.id values already asked in this session (never re-asked). */
    private List<Long> askedQuestionIds;

    public GameState() {
        this.currentProbabilities = new LinkedHashMap<>();
        this.askedQuestionIds = new ArrayList<>();
    }

    public GameState(String sessionId) {
        this();
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Map<Long, Double> getCurrentProbabilities() {
        return currentProbabilities;
    }

    public void setCurrentProbabilities(Map<Long, Double> currentProbabilities) {
        this.currentProbabilities = currentProbabilities;
    }

    public List<Long> getAskedQuestionIds() {
        return askedQuestionIds;
    }

    public void setAskedQuestionIds(List<Long> askedQuestionIds) {
        this.askedQuestionIds = askedQuestionIds;
    }
}
