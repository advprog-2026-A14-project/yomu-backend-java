package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.ArticleCreateRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.ArticleStatusResponse;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.ArticleUpdateRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Article;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.ArticleRepository;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.QuizRepository;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.UserAttemptRepository;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.BadRequestException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.ConflictException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.NotFoundException;
import id.ac.ui.cs.advprog.yomubackendjava.common.security.SecuritySanitizer;
import id.ac.ui.cs.advprog.yomubackendjava.forum.repository.CommentReactionRepository;
import id.ac.ui.cs.advprog.yomubackendjava.forum.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final QuizRepository quizRepository;
    private final UserAttemptRepository userAttemptRepository;
    private final CommentRepository commentRepository;
    private final CommentReactionRepository commentReactionRepository;

    public ArticleService(
            ArticleRepository articleRepository,
            QuizRepository quizRepository,
            UserAttemptRepository userAttemptRepository,
            CommentRepository commentRepository,
            CommentReactionRepository commentReactionRepository
    ) {
        this.articleRepository = articleRepository;
        this.quizRepository = quizRepository;
        this.userAttemptRepository = userAttemptRepository;
        this.commentRepository = commentRepository;
        this.commentReactionRepository = commentReactionRepository;
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
                request.getId().trim(),
                SecuritySanitizer.html(request.getTitle().trim()),
                SecuritySanitizer.html(request.getContent().trim()),
                SecuritySanitizer.html(request.getCategory().trim())
        );

        return articleRepository.save(article);
    }

    @Transactional
    public Article updateArticle(String id, ArticleUpdateRequest request) {
        validateArticleId(id);
        validateUpdateRequest(request);

        Article article = findById(id);
        if (request.getTitle() != null) {
            article.setTitle(SecuritySanitizer.html(request.getTitle().trim()));
        }
        if (request.getContent() != null) {
            article.setContent(SecuritySanitizer.html(request.getContent().trim()));
        }
        if (request.getCategory() != null) {
            article.setCategory(SecuritySanitizer.html(request.getCategory().trim()));
        }
        return articleRepository.save(article);
    }

    @Transactional
    public void deleteArticle(String id) {
        if (!articleRepository.existsById(id)) {
            throw new NotFoundException("Artikel tidak ditemukan");
        }

        commentReactionRepository.deleteByArticleId(id);
        commentRepository.deleteByArticleId(id);
        userAttemptRepository.deleteByKuisId(id);
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

    private void validateUpdateRequest(ArticleUpdateRequest request) {
        if (request == null) {
            throw new BadRequestException("Request artikel tidak boleh kosong");
        }
        if (request.getTitle() == null && request.getContent() == null && request.getCategory() == null) {
            throw new BadRequestException("minimal title, content, atau category harus diisi");
        }
        validateOptionalText("title", request.getTitle());
        validateOptionalText("content", request.getContent());
        validateOptionalText("category", request.getCategory());
    }

    private void validateArticleId(String id) {
        if (id == null || id.isBlank()) {
            throw new BadRequestException("id artikel wajib diisi");
        }
    }

    private void validateOptionalText(String field, String value) {
        if (value != null && value.isBlank()) {
            throw new BadRequestException(field + " tidak boleh kosong");
        }
    }
}
