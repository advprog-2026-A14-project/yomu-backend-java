package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizCreateRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizUpdateRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Quiz;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service.QuizManagementService;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminQuizController {
    private final QuizManagementService quizManagementService;

    public AdminQuizController(QuizManagementService quizManagementService) {
        this.quizManagementService = quizManagementService;
    }

    @PostMapping("/articles/{article_id}/quizzes")
    public ResponseEntity<ApiResponse<Quiz>> create(
            @PathVariable("article_id") String articleId,
            @RequestBody QuizCreateRequest request) {
        Quiz quiz = quizManagementService.createQuiz(articleId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Kuis berhasil dibuat", quiz));
    }

    @PatchMapping("/quizzes/{id}")
    public ResponseEntity<ApiResponse<Quiz>> update(
            @PathVariable String id,
            @RequestBody QuizUpdateRequest request) {
        Quiz quiz = quizManagementService.updateQuiz(id, request);
        return ResponseEntity.ok(ApiResponse.success("Kuis berhasil diperbarui", quiz));
    }

    @DeleteMapping("/quizzes/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        quizManagementService.deleteQuiz(id);
        return ResponseEntity.ok(ApiResponse.success("Kuis berhasil dihapus"));
    }
}