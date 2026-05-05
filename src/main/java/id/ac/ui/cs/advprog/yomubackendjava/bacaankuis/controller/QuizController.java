package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizQuestionResponse;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Quiz;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.QuizRepository;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service.QuizService;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quizzes")
public class QuizController {

    private final QuizService quizService;
    private final QuizRepository quizRepository;

    public QuizController(QuizService quizService, QuizRepository quizRepository) {
        this.quizService = quizService;
        this.quizRepository = quizRepository;
    }

    @GetMapping("/{article_id}")
    public ResponseEntity<ApiResponse<List<QuizQuestionResponse>>> getQuizzes(
            @PathVariable("article_id") String articleId) {
        List<QuizQuestionResponse> quizzes = quizRepository.findByArticleId(articleId)
                .stream()
                .map(QuizQuestionResponse::new)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Soal kuis ditemukan", quizzes));
    }

    @PostMapping("/{article_id}/submit")
    public ResponseEntity<ApiResponse<Void>> submitQuiz(
            @PathVariable("article_id") String articleId,
            @RequestBody QuizSyncRequest request) {
        request.setArticleId(articleId);
        quizService.submitAndSync(request);
        return ResponseEntity.ok(ApiResponse.success("Jawaban berhasil dikirim", null));
    }
}