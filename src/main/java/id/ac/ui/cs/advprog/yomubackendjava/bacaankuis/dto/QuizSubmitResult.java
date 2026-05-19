package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QuizSubmitResult {
    private final double score;
    private final double accuracy;

    @JsonProperty("correct_count")
    private final long correctCount;

    @JsonProperty("total_questions")
    private final int totalQuestions;

    public QuizSubmitResult(double score, double accuracy, long correctCount, int totalQuestions) {
        this.score = score;
        this.accuracy = accuracy;
        this.correctCount = correctCount;
        this.totalQuestions = totalQuestions;
    }

    public double getScore() {
        return score;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public long getCorrectCount() {
        return correctCount;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }
}