package com.siteja.engine.model.dto;

import java.util.List;

/**
 * Response body for POST /api/teja/answer.
 * Bundles the updated Top-5 leaderboard plus a simple "should we guess now?"
 * flag the frontend can use to trigger a final-answer screen.
 */
public class AnswerResponse {

    private String sessionId;
    private List<CandidateDto> topCandidates;

    /** True once the leading candidate's probability crosses a confidence threshold. */
    private boolean readyToGuess;

    /** Number of questions asked so far in this session. */
    private int questionsAsked;

    public AnswerResponse() {
    }

    public AnswerResponse(String sessionId, List<CandidateDto> topCandidates, boolean readyToGuess, int questionsAsked) {
        this.sessionId = sessionId;
        this.topCandidates = topCandidates;
        this.readyToGuess = readyToGuess;
        this.questionsAsked = questionsAsked;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<CandidateDto> getTopCandidates() {
        return topCandidates;
    }

    public void setTopCandidates(List<CandidateDto> topCandidates) {
        this.topCandidates = topCandidates;
    }

    public boolean isReadyToGuess() {
        return readyToGuess;
    }

    public void setReadyToGuess(boolean readyToGuess) {
        this.readyToGuess = readyToGuess;
    }

    public int getQuestionsAsked() {
        return questionsAsked;
    }

    public void setQuestionsAsked(int questionsAsked) {
        this.questionsAsked = questionsAsked;
    }
}
