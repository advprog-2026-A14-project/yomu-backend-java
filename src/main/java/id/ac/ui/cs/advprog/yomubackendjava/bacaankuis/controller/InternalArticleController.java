package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.ArticleStatusResponse;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service.ArticleService;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ForbiddenException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/articles")
public class InternalArticleController {

    private final ArticleService articleService;
    private final String internalApiKey;

    public InternalArticleController(
            ArticleService articleService,
            @Value("${internal.api.key}") String internalApiKey) {
        this.articleService = articleService;
        this.internalApiKey = internalApiKey;
    }

    @GetMapping("/{article_id}/exists")
    public ResponseEntity<ApiResponse<ArticleStatusResponse>> validate(
            @PathVariable("article_id") String articleId,
            @RequestHeader(value = "x-api-key", required = false) String apiKey) {

        validateApiKey(apiKey);

        ArticleStatusResponse status = articleService.checkArticleExists(articleId);
        return ResponseEntity.ok(ApiResponse.success("Validasi artikel", status));
    }

    private void validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new UnauthorizedException("Missing internal API key");
        }
        if (!internalApiKey.equals(apiKey)) {
            throw new ForbiddenException("Invalid internal API key");
        }
    }
}
