package com.siteja.engine.model.dto;

/**
 * Request body for POST /api/teja/answer
 * Example JSON:
 * {
 *   "sessionId": "a1b2c3d4-...",
 *   "questionId": 3,
 *   "answer": "YES"
 * }
 * <p>
 * questionId refers to the real Question.id primary key from the database
 * (see the `question` table seeded by V2__seed_karakter_data.sql).
 * "answer" is validated/parsed against the Answer enum inside TejaEngineService.
 * Accepted values (case-insensitive): YES, NO, DONT_KNOW, PROBABLY, PROBABLY_NOT.
 */
public class AnswerRequest {

    private String sessionId;
    private Long questionId;
    private String answer;

    public AnswerRequest() {
    }

    public AnswerRequest(String sessionId, Long questionId, String answer) {
        this.sessionId = sessionId;
        this.questionId = questionId;
        this.answer = answer;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
