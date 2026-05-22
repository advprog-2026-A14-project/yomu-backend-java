package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.ArticleCreateRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Article;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service.ArticleService;
import id.ac.ui.cs.advprog.yomubackendjava.security.JwtAuthFilter;
import id.ac.ui.cs.advprog.yomubackendjava.security.JwtService;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminArticleControllerTest {
    private static final String ARTICLE_ID = "art-001";
    private static final String JSON_SUCCESS = "$.success";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        public ArticleService articleService() {
            return Mockito.mock(ArticleService.class);
        }
    }

    @Test
    void createArticle_whenAdmin_returnsCreatedArticle() throws Exception {
        String token = tokenFor(Role.ADMIN);

        Article created = new Article(ARTICLE_ID, "Judul Bacaan", "Isi bacaan", "Olahraga");
        when(articleService.createArticle(Mockito.any(ArticleCreateRequest.class))).thenReturn(created);

        String json = """
        {
            "id": "%s",
            "title": "Judul Bacaan",
            "content": "Isi bacaan",
            "category": "Olahraga"
        }
        """.formatted(ARTICLE_ID);

        mockMvc.perform(post("/api/v1/admin/articles")
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_SUCCESS).value(true))
                .andExpect(jsonPath("$.data.id").value(ARTICLE_ID))
                .andExpect(jsonPath("$.data.category").value("Olahraga"));

        ArgumentCaptor<ArticleCreateRequest> captor = ArgumentCaptor.forClass(ArticleCreateRequest.class);
        verify(articleService).createArticle(captor.capture());
        assertEquals(ARTICLE_ID, captor.getValue().getId());
        assertEquals("Judul Bacaan", captor.getValue().getTitle());
    }

    @Test
    void createArticle_whenPelajar_returnsForbidden() throws Exception {
        String token = tokenFor(Role.PELAJAR);

        String json = """
        {
            "id": "%s",
            "title": "Judul Bacaan",
            "content": "Isi bacaan",
            "category": "Olahraga"
        }
        """.formatted(ARTICLE_ID);

        mockMvc.perform(post("/api/v1/admin/articles")
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(JSON_SUCCESS).value(false));
    }

    @Test
    void deleteArticle_whenAdmin_returnsSuccess() throws Exception {
        String token = tokenFor(Role.ADMIN);

        mockMvc.perform(delete("/api/v1/admin/articles/{articleId}", ARTICLE_ID)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_SUCCESS).value(true));

        verify(articleService).deleteArticle(ARTICLE_ID);
    }

    @Test
    void updateArticle_whenAdmin_returnsUpdatedArticle() throws Exception {
        String token = tokenFor(Role.ADMIN);
        Article updated = new Article(ARTICLE_ID, "Judul Baru", "Isi baru", "News");
        when(articleService.updateArticle(Mockito.eq(ARTICLE_ID), Mockito.any()))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/v1/admin/articles/{articleId}", ARTICLE_ID)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Judul Baru",
                                  "content": "Isi baru",
                                  "category": "News"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_SUCCESS).value(true))
                .andExpect(jsonPath("$.data.title").value("Judul Baru"));
    }

    private String tokenFor(Role role) {
        UserEntity user = new UserEntity();
        user.setUsername("admin_article_" + role.name().toLowerCase(Locale.ROOT));
        user.setDisplayName("Admin Article " + role.name());
        user.setRole(role);
        user.setPasswordHash("hash");
        UserEntity saved = userRepository.saveAndFlush(user);
        return jwtService.generateToken(saved.getUserId(), saved.getRole());
    }
}
