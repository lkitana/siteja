package com.siteja.engine.service;

import com.siteja.engine.model.GameState;
import com.siteja.engine.model.Karakter;
import com.siteja.engine.model.KarakterTrait;
import com.siteja.engine.model.Question;
import com.siteja.engine.model.dto.CandidateDto;
import com.siteja.engine.repository.KarakterRepository;
import com.siteja.engine.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * =====================================================================
 * TejaEngineService — the statistical core of Si-Teja.
 * =====================================================================
 * This service contains ZERO hardcoded "if this character then ask that"
 * branching, and ZERO hardcoded character/question/trait data — every
 * Karakter, Question, and trait probability is queried from MySQL via
 * KarakterRepository / QuestionRepository (JPA/Hibernate). Every decision
 * (which question to ask, how much to trust an answer) is derived purely
 * from two formulas:
 * <p>
 * 1) SHANNON ENTROPY — decides WHICH question to ask next.
 * H(X) = - Σ P(x_i) * log2(P(x_i))
 * We don't literally compute the entropy of the candidate distribution
 * itself; we compute, for each unasked question, the entropy of the
 * *binary YES/NO split* that question would induce over the current
 * probability mass. The question whose split is closest to 50/50 has
 * the HIGHEST entropy (max information gain) and is chosen. This is
 * the standard binary-decision form of Shannon Entropy used by
 * Akinator-style engines.
 * <p>
 * 2) BAYES' THEOREM — decides HOW MUCH to trust each answer and updates
 * every character's posterior probability accordingly.
 * P(T_k | J) = [ P(J | T_k) * P(T_k) ] / Σ_i [ P(J | T_i) * P(T_i) ]
 * T_k = "the true answer is character k"
 * J   = "the observed answer/evidence just given by the user"
 * No character is ever hard-deleted on a NO answer — its probability is
 * only ever multiplied down, never zeroed, which is what makes the
 * engine tolerant of accidental wrong clicks.
 */
@Service
public class TejaEngineService {

    /**
     * In-memory multi-user session store: sessionId -> that user's GameState.
     * Intentionally NOT persisted to MySQL — a game session is transient
     * runtime state (like an HTTP session), not durable domain data.
     * Karakter/Question/KarakterTrait are the durable data and DO live in
     * the database.
     */
    private final ConcurrentHashMap<String, GameState> sessions = new ConcurrentHashMap<>();

    private final KarakterRepository karakterRepository;
    private final QuestionRepository questionRepository;

    /**
     * Once the leading candidate's posterior crosses this probability,
     * the frontend is told readyToGuess = true. Purely a UX threshold,
     * not part of the Bayes/Entropy math itself.
     */
    private static final double CONFIDENCE_THRESHOLD = 0.75;

    /** P(YES) assumed for a (karakter, question) pair with no curated row in the DB. */
    private static final double UNKNOWN_TRAIT_PROBABILITY = 0.5;

    @Autowired
    public TejaEngineService(KarakterRepository karakterRepository, QuestionRepository questionRepository) {
        this.karakterRepository = karakterRepository;
        this.questionRepository = questionRepository;
    }

    // =================================================================
    // SESSION LIFECYCLE
    // =================================================================

    /**
     * Starts a brand-new game session with a UNIFORM prior distribution:
     * every candidate starts equally likely, P(T_k) = 1 / N, where N is
     * however many Karakter rows currently exist in the database.
     */
    @Transactional(readOnly = true)
    public String startNewGame() {
        String sessionId = UUID.randomUUID().toString();
        GameState state = new GameState(sessionId);

        List<Karakter> allKarakters = karakterRepository.findAll();
        if (allKarakters.isEmpty()) {
            throw new IllegalStateException("No Karakter rows found in the database — check the Flyway seed migration.");
        }
        double uniformPrior = 1.0 / allKarakters.size();

        Map<Long, Double> initialProbabilities = new LinkedHashMap<>();
        for (Karakter k : allKarakters) {
            initialProbabilities.put(k.getId(), uniformPrior);
        }

        state.setCurrentProbabilities(initialProbabilities);
        sessions.put(sessionId, state);
        return sessionId;
    }

    /** Fetches a session's state, or throws if the sessionId is unknown/expired. */
    private GameState getStateOrThrow(String sessionId) {
        GameState state = sessions.get(sessionId);
        if (state == null) {
            throw new IllegalArgumentException("No active game session found for sessionId: " + sessionId);
        }
        return state;
    }

    // =================================================================
    // STEP 1 — SHANNON ENTROPY: choosing the next best question
    // =================================================================

    /**
     * Scans every unasked question (queried from the database) and picks
     * the one with maximum Information Gain (i.e. maximum entropy of its
     * induced YES/NO split over the CURRENT probability distribution).
     */
    @Transactional(readOnly = true)
    public Question getNextBestQuestion(String sessionId) {
        GameState state = getStateOrThrow(sessionId);
        List<Karakter> allKarakters = karakterRepository.findAll();
        List<Question> allQuestions = questionRepository.findAll();

        List<Question> unasked = allQuestions.stream()
                .filter(q -> !state.getAskedQuestionIds().contains(q.getId()))
                .collect(Collectors.toList());

        if (unasked.isEmpty()) {
            return null; // No more questions left — controller/frontend should move to final guess.
        }

        Question bestQuestion = null;
        double bestEntropy = -1.0; // Entropy is always >= 0, so -1 guarantees the first candidate wins initially.

        for (Question candidateQuestion : unasked) {
            double entropy = calculateEntropyForQuestion(candidateQuestion, allKarakters, state.getCurrentProbabilities());
            if (entropy > bestEntropy) {
                bestEntropy = entropy;
                bestQuestion = candidateQuestion;
            }
        }

        return bestQuestion;
    }

    /**
     * --- SHANNON ENTROPY CALCULATION (core formula) ---------------------
     * For ONE candidate question, this computes the entropy of the binary
     * split it would create over the current probability mass:
     * <p>
     *   pYes = Σ_k [ P(T_k) * P(J=YES | T_k) ]   <- expected probability mass
     *                                                that would answer YES
     *   pNo  = 1 - pYes
     * <p>
     *   H(question) = - ( pYes * log2(pYes) + pNo * log2(pNo) )
     * <p>
     * This is EXACTLY the Shannon Entropy formula H(X) = -Σ P(x_i)*log2(P(x_i))
     * applied to the two-outcome distribution X = {YES, NO}.
     * H is maximal (H = 1.0 bit) when pYes = pNo = 0.5 — i.e. the question
     * splits the remaining candidates as close to 50/50 as possible, which
     * is precisely the "best" question to ask to minimize expected future
     * questions needed.
     */
    private double calculateEntropyForQuestion(Question question, List<Karakter> allKarakters,
                                                Map<Long, Double> currentProbabilities) {

        double pYes = 0.0;
        for (Karakter k : allKarakters) {
            double posterior = currentProbabilities.getOrDefault(k.getId(), 0.0);
            double likelihoodYesGivenK = traitProbability(k, question);
            // Expected contribution of this character to the overall "YES mass".
            pYes += posterior * likelihoodYesGivenK;
        }

        double pNo = 1.0 - pYes;

        return shannonBinaryEntropy(pYes, pNo);
    }

    /**
     * Pure implementation of H(X) = - Σ P(x_i) * log2(P(x_i)) for the
     * two-outcome case X = {YES, NO}. Guards against log2(0), which is
     * mathematically undefined (-Infinity) but conventionally treated
     * as contributing 0 to the entropy sum (standard information-theory
     * convention: 0 * log2(0) := 0).
     */
    private double shannonBinaryEntropy(double pYes, double pNo) {
        double entropy = 0.0;
        entropy += entropyTerm(pYes);
        entropy += entropyTerm(pNo);
        return entropy;
    }

    /** Single term of the Shannon sum: -P(x_i) * log2(P(x_i)), safe for P=0. */
    private double entropyTerm(double p) {
        if (p <= 0.0) {
            return 0.0; // 0 * log2(0) is conventionally defined as 0.
        }
        return -p * (Math.log(p) / Math.log(2)); // log2(p) = ln(p) / ln(2)
    }

    // =================================================================
    // TRAIT LOOKUP — reads the curated probability from the loaded
    // Karakter.traits collection (populated straight from MySQL via the
    // KarakterRepository entity graph). Replaces the old
    // Map<String,Double> lookup that used to live directly on the POJO.
    // =================================================================

    /**
     * Returns P(YES | this karakter is the true answer) for the given
     * question, as curated in the karakter_trait table. Falls back to
     * 0.5 (maximum uncertainty) if no row exists for this pairing —
     * an un-curated trait should neither help nor hurt a character's odds.
     */
    private double traitProbability(Karakter karakter, Question question) {
        return karakter.getTraits().stream()
                .filter(t -> t.getQuestion().getId().equals(question.getId()))
                .map(KarakterTrait::getProbability)
                .findFirst()
                .orElse(UNKNOWN_TRAIT_PROBABILITY);
    }

    // =================================================================
    // STEP 2 — BAYES' THEOREM: updating probabilities from an answer
    // =================================================================

    /** Supported answer evidence types, each with its own likelihood weighting below. */
    public enum Answer {
        YES, NO, DONT_KNOW, PROBABLY, PROBABLY_NOT
    }

    /**
     * Applies Bayesian updating to EVERY character based on the user's
     * answer to `questionId`, then re-normalizes so probabilities sum
     * back to 1.0. This is called once per turn and is the ONLY place
     * probabilities change.
     */
    @Transactional(readOnly = true)
    public void processAnswer(String sessionId, Long questionId, String rawAnswer) {
        GameState state = getStateOrThrow(sessionId);
        Answer answer = parseAnswer(rawAnswer);
        List<Karakter> allKarakters = karakterRepository.findAll();

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("No question found for questionId: " + questionId));

        Map<Long, Double> priors = state.getCurrentProbabilities();
        Map<Long, Double> unnormalizedPosteriors = new LinkedHashMap<>();

        // --- BAYES NUMERATOR STEP: P(J | T_k) * P(T_k) for every character k ---
        // This loop computes the numerator of Bayes' Theorem for each candidate,
        // and simultaneously accumulates the denominator Σ [ P(J | T_i) * P(T_i) ].
        double evidenceNormalizer = 0.0; // This is the Σ term — the "total evidence" denominator.

        for (Karakter k : allKarakters) {
            double prior = priors.getOrDefault(k.getId(), 0.0);
            double likelihood = likelihoodOfAnswerGivenKarakter(answer, k, question);
            double numerator = likelihood * prior;

            unnormalizedPosteriors.put(k.getId(), numerator);
            evidenceNormalizer += numerator;
        }

        // --- BAYES DIVISION STEP: divide every numerator by Σ to get true P(T_k | J) ---
        Map<Long, Double> normalizedPosteriors = new LinkedHashMap<>();
        for (Map.Entry<Long, Double> entry : unnormalizedPosteriors.entrySet()) {
            double posterior;
            if (evidenceNormalizer > 0.0) {
                posterior = entry.getValue() / evidenceNormalizer; // exact Bayes' Theorem division
            } else {
                // Degenerate safety-net: if every likelihood*prior underflowed to 0
                // (should not happen given our 0.05-0.95 curated traits), fall back
                // to uniform rather than dividing by zero / producing NaN.
                posterior = 1.0 / allKarakters.size();
            }
            normalizedPosteriors.put(entry.getKey(), posterior);
        }

        state.setCurrentProbabilities(normalizedPosteriors);

        if (!state.getAskedQuestionIds().contains(questionId)) {
            state.getAskedQuestionIds().add(questionId);
        }
    }

    /**
     * --- LIKELIHOOD FUNCTION: P(J | T_k) --------------------------------
     * This is the "likelihood" term of Bayes' Theorem: given that character
     * k IS the true answer, how probable is it that the observed answer J
     * would occur?
     * <p>
     * TOLERANCE RULE implementation:
     * - YES  -> use the trait probability directly. A character strongly
     *           matching the trait (high P) gets rewarded; a mismatching
     *           character gets a LOW likelihood (not zero) so it is only
     *           ever pushed down proportionally, never hard-eliminated.
     * - NO   -> use the COMPLEMENT (1 - traitProbability). Symmetric logic:
     *           mismatching characters (low original trait P) now score
     *           HIGH likelihood for "NO", correctly boosting them.
     * - DONT_KNOW -> likelihood is flat 0.5 for every character. This is
     *           mathematically inert: since it's the SAME constant for all
     *           k, it cancels out in the normalization step, meaning
     *           "don't know" leaves the distribution's SHAPE unchanged
     *           (it doesn't help or hurt anyone) but the question is still
     *           marked asked so it isn't repeated.
     * - PROBABLY / PROBABLY_NOT -> softened versions of YES/NO. Instead of
     *           trusting the trait fully, we blend it 70% toward the
     *           trait's signal and 30% toward neutral (0.5), which produces
     *           a gentler Bayesian nudge than a full YES/NO — directly
     *           satisfying the "resilient to soft/uncertain input" goal.
     */
    private double likelihoodOfAnswerGivenKarakter(Answer answer, Karakter k, Question question) {
        double traitProbability = traitProbability(k, question); // P(YES | T_k) as curated in the database

        switch (answer) {
            case YES:
                return traitProbability;
            case NO:
                return 1.0 - traitProbability;
            case PROBABLY:
                return blendTowardNeutral(traitProbability, 0.7);
            case PROBABLY_NOT:
                return blendTowardNeutral(1.0 - traitProbability, 0.7);
            case DONT_KNOW:
            default:
                return 0.5; // Neutral evidence: cancels out during normalization.
        }
    }

    /** Blends a probability toward 0.5 (neutral) by `weight`, softening confidence. */
    private double blendTowardNeutral(double p, double weight) {
        return (p * weight) + (0.5 * (1.0 - weight));
    }

    /** Parses free-text answer strings leniently (case-insensitive, trims whitespace). */
    private Answer parseAnswer(String rawAnswer) {
        if (rawAnswer == null) {
            throw new IllegalArgumentException("Answer must not be null");
        }
        try {
            return Answer.valueOf(rawAnswer.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid answer '" + rawAnswer + "'. Expected one of: YES, NO, DONT_KNOW, PROBABLY, PROBABLY_NOT");
        }
    }

    // =================================================================
    // LEADERBOARD / DASHBOARD OUTPUT
    // =================================================================

    /**
     * Returns the top-N candidates for this session sorted by descending
     * posterior probability, packaged as (Karakter, probability) entries.
     */
    @Transactional(readOnly = true)
    public List<Map.Entry<Karakter, Double>> getTopCandidates(String sessionId, int limit) {
        GameState state = getStateOrThrow(sessionId);
        Map<Long, Karakter> karakterById = karakterRepository.findAll().stream()
                .collect(Collectors.toMap(Karakter::getId, k -> k));

        return state.getCurrentProbabilities().entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue())) // descending probability
                .limit(limit)
                .map(e -> (Map.Entry<Karakter, Double>) new AbstractMap.SimpleEntry<>(
                        karakterById.get(e.getKey()), e.getValue()))
                .collect(Collectors.toList());
    }

    /** Convenience wrapper producing frontend-ready DTOs (used by the controller). */
    @Transactional(readOnly = true)
    public List<CandidateDto> getTopCandidateDtos(String sessionId, int limit) {
        return getTopCandidates(sessionId, limit).stream()
                .map(e -> new CandidateDto(e.getKey().getId(), e.getKey().getName(), e.getValue()))
                .collect(Collectors.toList());
    }

    /** True once the single leading candidate's probability clears CONFIDENCE_THRESHOLD. */
    public boolean isReadyToGuess(String sessionId) {
        GameState state = getStateOrThrow(sessionId);
        return state.getCurrentProbabilities().values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0) >= CONFIDENCE_THRESHOLD;
    }

    public int getQuestionsAskedCount(String sessionId) {
        return getStateOrThrow(sessionId).getAskedQuestionIds().size();
    }
}
