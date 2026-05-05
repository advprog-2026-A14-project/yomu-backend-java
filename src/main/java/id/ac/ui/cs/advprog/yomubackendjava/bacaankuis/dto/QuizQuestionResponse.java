package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Quiz;

public class QuizQuestionResponse {
    private final String id;
    private final String articleId;
    private final String question;
    private final String options;

    public QuizQuestionResponse(Quiz quiz) {
        this.id = quiz.getId();
        this.articleId = quiz.getArticleId();
        this.question = quiz.getQuestion();
        this.options = quiz.getOptions();
    }

    public String getId() {
        return id;
    }

    public String getArticleId() {
        return articleId;
    }

    public String getQuestion() {
        return question;
    }

    public String getOptions() {
        return options;
    }
}
