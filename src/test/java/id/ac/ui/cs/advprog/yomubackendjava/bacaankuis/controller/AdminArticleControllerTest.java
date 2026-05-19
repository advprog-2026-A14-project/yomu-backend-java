package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.ArticleCreateRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Article;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service.ArticleService;
import id.ac.ui.cs.advprog.yomubackendjava.security.JwtAuthFilter;
import id.ac.ui.cs.advprog.yomubackendjava.security.JwtService;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ArticleService articleService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        public ArticleService articleService() {
            return Mockito.mock(ArticleService.class);
        }
    }

    @Test
    void createArticle_whenAdmin_returnsCreatedArticle() throws Exception {
        String token = jwtService.generateToken(UUID.randomUUID(), Role.ADMIN);

        Article created = new Article("art-001", "Judul Bacaan", "Isi bacaan", "Olahraga");
        when(articleService.createArticle(Mockito.any(ArticleCreateRequest.class))).thenReturn(created);

        String json = """
        {
            "id": "art-001",
            "title": "Judul Bacaan",
            "content": "Isi bacaan",
            "category": "Olahraga"
        }
        """;

        mockMvc.perform(post("/api/v1/admin/articles")
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("art-001"))
                .andExpect(jsonPath("$.data.category").value("Olahraga"));

        ArgumentCaptor<ArticleCreateRequest> captor = ArgumentCaptor.forClass(ArticleCreateRequest.class);
        verify(articleService).createArticle(captor.capture());
        assertEquals("art-001", captor.getValue().getId());
        assertEquals("Judul Bacaan", captor.getValue().getTitle());
    }

    @Test
    void createArticle_whenPelajar_returnsForbidden() throws Exception {
        String token = jwtService.generateToken(UUID.randomUUID(), Role.PELAJAR);

        String json = """
        {
            "id": "art-001",
            "title": "Judul Bacaan",
            "content": "Isi bacaan",
            "category": "Olahraga"
        }
        """;

        mockMvc.perform(post("/api/v1/admin/articles")
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deleteArticle_whenAdmin_returnsSuccess() throws Exception {
        String token = jwtService.generateToken(UUID.randomUUID(), Role.ADMIN);

        mockMvc.perform(delete("/api/v1/admin/articles/art-001")
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(articleService).deleteArticle("art-001");
    }
}