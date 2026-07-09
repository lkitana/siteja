package com.siteja.engine.controller;

import com.siteja.engine.model.Question;
import com.siteja.engine.model.dto.AnswerRequest;
import com.siteja.engine.model.dto.AnswerResponse;
import com.siteja.engine.model.dto.CandidateDto;
import com.siteja.engine.service.TejaEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin HTTP layer for Si-Teja. Contains NO statistical logic itself — it
 * only translates HTTP requests into calls on TejaEngineService and shapes
 * the JSON responses. All Entropy/Bayes math lives exclusively in the
 * service layer.
 */
@RestController
@RequestMapping("/api/teja")
@CrossOrigin(origins = "*") // Allows any frontend (browser/Postman) to call this API during development.
public class TejaController {

    private final TejaEngineService tejaEngineService;

    @Autowired
    public TejaController(TejaEngineService tejaEngineService) {
        this.tejaEngineService = tejaEngineService;
    }

    /**
     * POST /api/teja/start
     * Starts a brand new game session with a uniform prior distribution.
     * Response: { "sessionId": "..." }
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startGame() {
        String sessionId = tejaEngineService.startNewGame();
        Map<String, String> response = new LinkedHashMap<>();
        response.put("sessionId", sessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/teja/next-question?sessionId=...
     * Returns the next best Question chosen via Shannon Entropy maximization,
     * or 204 No Content if the question bank has been exhausted for this session.
     */
    @GetMapping("/next-question")
    public ResponseEntity<?> getNextQuestion(@RequestParam String sessionId) {
        try {
            Question next = tejaEngineService.getNextBestQuestion(sessionId);
            if (next == null) {
                return ResponseEntity.noContent().build(); // No questions left; frontend should show final guess.
            }
            return ResponseEntity.ok(next);
        } catch (IllegalArgumentException ex) {
            return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    /**
     * POST /api/teja/answer
     * Body: { "sessionId": "...", "questionId": 3, "answer": "YES|NO|DONT_KNOW|PROBABLY|PROBABLY_NOT" }
     * Applies the Bayesian update for this answer, then returns the updated
     * Top-5 candidates (ready for a Chart.js bar/pie dataset) plus a
     * readyToGuess flag once the leader is confident enough.
     */
    @PostMapping("/answer")
    public ResponseEntity<?> submitAnswer(@RequestBody AnswerRequest request) {
        try {
            tejaEngineService.processAnswer(request.getSessionId(), request.getQuestionId(), request.getAnswer());

            List<CandidateDto> topFive = tejaEngineService.getTopCandidateDtos(request.getSessionId(), 5);
            boolean readyToGuess = tejaEngineService.isReadyToGuess(request.getSessionId());
            int questionsAsked = tejaEngineService.getQuestionsAskedCount(request.getSessionId());

            AnswerResponse response = new AnswerResponse(
                    request.getSessionId(), topFive, readyToGuess, questionsAsked);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    /**
     * GET /api/teja/top-candidates?sessionId=...&limit=5
     * Standalone dashboard endpoint — returns current standings without
     * requiring a new answer to be submitted. Useful for polling/refresh.
     */
    @GetMapping("/top-candidates")
    public ResponseEntity<?> getTopCandidates(@RequestParam String sessionId,
                                               @RequestParam(defaultValue = "5") int limit) {
        try {
            List<CandidateDto> topCandidates = tejaEngineService.getTopCandidateDtos(sessionId, limit);
            return ResponseEntity.ok(topCandidates);
        } catch (IllegalArgumentException ex) {
            return errorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    private ResponseEntity<Map<String, String>> errorResponse(HttpStatus status, String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
