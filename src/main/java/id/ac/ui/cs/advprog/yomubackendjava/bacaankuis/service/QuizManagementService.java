package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizCreateRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizUpdateRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Quiz;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.ArticleRepository;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.QuizRepository;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.BadRequestException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ConflictException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizManagementService {
    private final QuizRepository quizRepository;
    private final ArticleRepository articleRepository;

    public QuizManagementService(QuizRepository quizRepository, ArticleRepository articleRepository) {
        this.quizRepository = quizRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional
    public Quiz createQuiz(String articleId, QuizCreateRequest request) {
        validateArticleId(articleId);
        validateCreateRequest(request);

        if (!articleRepository.existsById(articleId)) {
            throw new NotFoundException("Artikel tidak ditemukan");
        }
        if (quizRepository.existsById(request.getId())) {
            throw new ConflictException("Kuis dengan id tersebut sudah ada");
        }

        Quiz quiz = new Quiz(
                request.getId(),
                articleId,
                request.getQuestion(),
                request.getOptions(),
                request.getAnswer()
        );

        return quizRepository.save(quiz);
    }

    @Transactional
    public Quiz updateQuiz(String id, QuizUpdateRequest request) {
        validateQuizId(id);
        validateUpdateRequest(request);

        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kuis tidak ditemukan"));

        quiz.setQuestion(request.getQuestion());
        quiz.setOptions(request.getOptions());
        quiz.setAnswer(request.getAnswer());

        return quizRepository.save(quiz);
    }

    @Transactional
    public void deleteQuiz(String id) {
        validateQuizId(id);

        if (!quizRepository.existsById(id)) {
            throw new NotFoundException("Kuis tidak ditemukan");
        }

        quizRepository.deleteById(id);
    }

    private void validateArticleId(String articleId) {
        if (articleId == null || articleId.isBlank()) {
            throw new BadRequestException("article_id wajib diisi");
        }
    }

    private void validateQuizId(String id) {
        if (id == null || id.isBlank()) {
            throw new BadRequestException("id kuis wajib diisi");
        }
    }

    private void validateCreateRequest(QuizCreateRequest request) {
        if (request == null) {
            throw new BadRequestException("Request kuis tidak boleh kosong");
        }
        if (request.getId() == null || request.getId().isBlank()) {
            throw new BadRequestException("id wajib diisi");
        }
        validateQuestionFields(request.getQuestion(), request.getOptions(), request.getAnswer());
    }

    private void validateUpdateRequest(QuizUpdateRequest request) {
        if (request == null) {
            throw new BadRequestException("Request kuis tidak boleh kosong");
        }
        validateQuestionFields(request.getQuestion(), request.getOptions(), request.getAnswer());
    }

    private void validateQuestionFields(String question, String options, String answer) {
        if (question == null || question.isBlank()) {
            throw new BadRequestException("question wajib diisi");
        }
        if (options == null || options.isBlank()) {
            throw new BadRequestException("options wajib diisi");
        }
        if (answer == null || answer.isBlank()) {
            throw new BadRequestException("answer wajib diisi");
        }
    }
}