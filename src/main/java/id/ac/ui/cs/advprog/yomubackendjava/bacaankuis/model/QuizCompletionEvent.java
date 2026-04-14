package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model;

import lombok.Getter;

@Getter
public class QuizCompletionEvent {
    private final String userId;
    private final String articleId;
    private final double score;

    public QuizCompletionEvent(String userId, String articleId, double score) {
        this.userId = userId;
        this.articleId = articleId;
        this.score = score;
    }
}