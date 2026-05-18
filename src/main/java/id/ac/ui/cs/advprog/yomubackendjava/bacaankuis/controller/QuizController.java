package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizQuestionResponse;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Quiz;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.QuizRepository;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service.QuizService;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.UnauthorizedException;
import id.ac.ui.cs.advprog.yomubackendjava.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
        UUID userId = CurrentUser.userId()
                .orElseThrow(() -> new UnauthorizedException("Login diperlukan"));
        quizService.submitAndSync(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Jawaban berhasil dikirim", null));
    }
}
