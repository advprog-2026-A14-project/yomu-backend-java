package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.ArticleCreateRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Article;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service.ArticleService;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/articles")
public class AdminArticleController {
    private final ArticleService articleService;

    public AdminArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Article>> create(@RequestBody ArticleCreateRequest request) {
        Article article = articleService.createArticle(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Artikel berhasil dibuat", article));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        articleService.deleteArticle(id);
        return ResponseEntity.ok(ApiResponse.success("Artikel berhasil dihapus"));
    }
}