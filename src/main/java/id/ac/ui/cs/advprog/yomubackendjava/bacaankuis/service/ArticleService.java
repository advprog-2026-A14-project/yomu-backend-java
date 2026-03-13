package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.ArticleStatusResponse;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ArticleService {
    @Autowired private ArticleRepository articleRepository;

    public ArticleStatusResponse checkArticleExists(String id) {
        boolean exists = articleRepository.existsById(id);
        return new ArticleStatusResponse(exists, 1, "Edu");
    }
}