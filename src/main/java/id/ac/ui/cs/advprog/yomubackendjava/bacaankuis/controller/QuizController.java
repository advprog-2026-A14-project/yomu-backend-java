package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizQuestionResponse;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSubmitRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service.QuizService;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSubmitResult;
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

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/{article_id}")
    public ResponseEntity<ApiResponse<List<QuizQuestionResponse>>> getQuizzes(
            @PathVariable("article_id") String articleId) {
        UUID userId = CurrentUser.userId()
                .orElseThrow(() -> new UnauthorizedException("Login diperlukan"));

        List<QuizQuestionResponse> quizzes = quizService.getAvailableQuizzes(userId, articleId);
        return ResponseEntity.ok(ApiResponse.success("Soal kuis ditemukan", quizzes));
    }

    @PostMapping("/{article_id}/submit")
    public ResponseEntity<ApiResponse<QuizSubmitResult>> submitQuiz(
            @PathVariable("article_id") String articleId,
            @RequestBody QuizSubmitRequest request) {
        UUID userId = CurrentUser.userId()
                .orElseThrow(() -> new UnauthorizedException("Login diperlukan"));

        QuizSubmitResult result = quizService.submitAndSync(userId, articleId, request);
        return ResponseEntity.ok(ApiResponse.success("Jawaban berhasil dikirim", result));
    }
}