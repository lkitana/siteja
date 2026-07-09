package com.siteja.engine.model.dto;

/**
 * Flat, frontend-friendly view of one candidate's current standing.
 * Designed to be dropped straight into a Chart.js dataset:
 * labels: [names...], data: [percentage...]
 */
public class CandidateDto {

    private Long id;
    private String name;

    /** Raw posterior probability, 0.0 - 1.0 (sums to 1.0 across ALL candidates, not just this page). */
    private double probability;

    /** Same value * 100, rounded to 2 decimals, for direct display ("42.17%"). */
    private double percentage;

    public CandidateDto() {
    }

    public CandidateDto(Long id, String name, double probability) {
        this.id = id;
        this.name = name;
        this.probability = probability;
        this.percentage = Math.round(probability * 10000.0) / 100.0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}
