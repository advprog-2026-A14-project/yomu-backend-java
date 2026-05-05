package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.ArticleStatusResponse;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Article;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.ArticleRepository;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleService {
    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public List<Article> findAll(String category) {
        if (category == null || category.isBlank()) {
            return articleRepository.findAll();
        }
        return articleRepository.findByCategoryIgnoreCase(category);
    }

    public Article findById(String id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Artikel tidak ditemukan"));
    }

    public ArticleStatusResponse checkArticleExists(String id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Artikel tidak ditemukan di Core DB"));

        return new ArticleStatusResponse(true, 1, article.getCategory());
    }
}
