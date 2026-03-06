package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Article;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.ArticleRepository;
import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/articles")
public class ArticleController {

    @Autowired
    private ArticleRepository articleRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Article>>> list() {
        List<Article> articles = articleRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Daftar bacaan", articles));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Article>> detail(@PathVariable String id) {
        return articleRepository.findById(id)
                .map(a -> ResponseEntity.ok(ApiResponse.success("Detail bacaan", a)))
                .orElse(ResponseEntity.notFound().build());
    }
}