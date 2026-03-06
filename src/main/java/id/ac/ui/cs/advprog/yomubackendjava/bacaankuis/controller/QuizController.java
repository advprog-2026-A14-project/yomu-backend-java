package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Quiz;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.QuizRepository;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service.QuizService;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quizzes")
public class QuizController {

    @Autowired
    private QuizService quizService;

    @Autowired
    private QuizRepository quizRepository;

    @GetMapping("/{article_id}")
    public ResponseEntity<ApiResponse<List<Quiz>>> getQuizzes(@PathVariable("article_id") String articleId) {
        List<Quiz> quizzes = quizRepository.findByArticleId(articleId);
        return ResponseEntity.ok(ApiResponse.success("Soal kuis ditemukan", quizzes));
    }

    @PostMapping("/{article_id}/submit")
    public ResponseEntity<ApiResponse<Void>> submitQuiz(
            @PathVariable("article_id") String articleId,
            @RequestBody QuizSyncRequest request) {

        request.setArticleId(articleId);
        quizService.submitAndSync(request);

        return ResponseEntity.ok(ApiResponse.success("Jawaban berhasil dikirim"));
    }
}