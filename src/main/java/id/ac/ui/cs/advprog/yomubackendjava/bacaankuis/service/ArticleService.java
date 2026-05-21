package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.ArticleCreateRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.ArticleStatusResponse;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Article;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.ArticleRepository;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.QuizRepository;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.BadRequestException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ConflictException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final QuizRepository quizRepository;

    public ArticleService(ArticleRepository articleRepository, QuizRepository quizRepository) {
        this.articleRepository = articleRepository;
        this.quizRepository = quizRepository;
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

    @Transactional
    public Article createArticle(ArticleCreateRequest request) {
        validateCreateRequest(request);

        if (articleRepository.existsById(request.getId())) {
            throw new ConflictException("Artikel dengan id tersebut sudah ada");
        }

        Article article = new Article(
                request.getId(),
                request.getTitle(),
                request.getContent(),
                request.getCategory()
        );

        return articleRepository.save(article);
    }

    @Transactional
    public void deleteArticle(String id) {
        if (!articleRepository.existsById(id)) {
            throw new NotFoundException("Artikel tidak ditemukan");
        }

        quizRepository.deleteByArticleId(id);
        articleRepository.deleteById(id);
    }

    public ArticleStatusResponse checkArticleExists(String id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Artikel tidak ditemukan di Core DB"));

        return new ArticleStatusResponse(true, 1, article.getCategory());
    }

    private void validateCreateRequest(ArticleCreateRequest request) {
        if (request == null) {
            throw new BadRequestException("Request artikel tidak boleh kosong");
        }
        if (request.getId() == null || request.getId().isBlank()) {
            throw new BadRequestException("id wajib diisi");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("title wajib diisi");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new BadRequestException("content wajib diisi");
        }
        if (request.getCategory() == null || request.getCategory().isBlank()) {
            throw new BadRequestException("category wajib diisi");
        }
    }
}