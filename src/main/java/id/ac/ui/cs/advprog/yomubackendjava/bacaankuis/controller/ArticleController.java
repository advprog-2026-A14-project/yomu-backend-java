package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Article;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service.ArticleService;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Article>>> list(
            @RequestParam(value = "category", required = false) String category) {
        List<Article> articles = articleService.findAll(category);
        return ResponseEntity.ok(ApiResponse.success("Daftar bacaan", articles));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Article>> detail(@PathVariable String id) {
        Article article = articleService.findById(id);
        return ResponseEntity.ok(ApiResponse.success("Detail bacaan", article));
    }
}
