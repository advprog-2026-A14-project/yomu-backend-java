package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.ArticleStatusResponse;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service.ArticleService;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/articles")
public class InternalArticleController {

    private ArticleService articleService;

    public InternalArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping("/{article_id}/exists")
    public ResponseEntity<ApiResponse<ArticleStatusResponse>> validate(
            @PathVariable("article_id") String articleId,
            @RequestHeader(value = "x-api-key", required = false) String apiKey) {

        ArticleStatusResponse status = articleService.checkArticleExists(articleId);
        return ResponseEntity.ok(ApiResponse.success("Validasi artikel", status));
    }
}