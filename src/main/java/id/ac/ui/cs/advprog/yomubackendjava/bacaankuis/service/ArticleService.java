package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.ArticleStatusResponse;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Article;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.ArticleRepository;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.BadRequestException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.NotFoundException;
import id.ac.ui.cs.advprog.yomubackendjava.common.security.SecuritySanitizer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleService {
    private static final int MAX_ARTICLE_LOOKUP_LENGTH = 128;

    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public List<Article> findAll(String category) {
        String normalizedCategory = normalizeLookupValue(category);
        if (normalizedCategory == null) {
            return articleRepository.findAll();
        }
        return articleRepository.findByCategoryIgnoreCase(normalizedCategory);
    }

    public Article findById(String id) {
        return articleRepository.findById(requireLookupValue(id))
                .orElseThrow(() -> new NotFoundException("Artikel tidak ditemukan"));
    }

    public ArticleStatusResponse checkArticleExists(String id) {
        Article article = articleRepository.findById(requireLookupValue(id))
                .orElseThrow(() -> new NotFoundException("Artikel tidak ditemukan di Core DB"));

        return new ArticleStatusResponse(true, 1, article.getCategory());
    }

    private String requireLookupValue(String value) {
        String normalizedValue = normalizeLookupValue(value);
        if (normalizedValue == null) {
            throw new BadRequestException("article_id wajib diisi");
        }
        return normalizedValue;
    }

    private String normalizeLookupValue(String value) {
        String normalizedValue = SecuritySanitizer.normalize(value);
        if (normalizedValue != null && normalizedValue.length() > MAX_ARTICLE_LOOKUP_LENGTH) {
            throw new BadRequestException("parameter artikel terlalu panjang");
        }
        return normalizedValue;
    }
}
