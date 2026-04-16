package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Article;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.ArticleRepository;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @deprecated
 * File ini dipertahankan hanya untuk Legacy Support (kebutuhan modul lain yang belum migrasi).
 * Fungsi utama telah dipindahkan ke:
 * - {@link ArticleController} untuk daftar bacaan (/api/v1/articles)
 * - {@link QuizController} untuk soal kuis (/api/v1/quizzes)
 * * Silakan migrasi endpoint Anda ke struktur baru sesuai API Contract terbaru.
 */
@Deprecated
@RestController
@RequestMapping({"/api/bacaankuis", "/api/v1/bacaankuis"})
@CrossOrigin(origins = "*")
public class BacaanKuisController {
    private final ArticleRepository articleRepository;

    public BacaanKuisController(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Article>>> getAllKuis() {
        List<Article> data = articleRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Bacaan kuis fetched", data));
    }
}
