package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.ArticleStatusResponse;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InternalArticleControllerTest {

    private static final String VALID_API_KEY = "test-key-untuk-keperluan-testing-lokal";

    @Autowired
    private MockMvc mockMvc;

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
    void validate_whenApiKeyIsValid_returnsArticleStatus() throws Exception {
        String id = "art-123";

        when(articleService.checkArticleExists(id))
                .thenReturn(new ArticleStatusResponse(true, 1, "Edu"));

        mockMvc.perform(get("/api/internal/articles/" + id + "/exists")
                        .header("x-api-key", VALID_API_KEY)
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.category_name").value("Edu"));
    }

    @Test
    void validate_whenApiKeyIsMissing_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/internal/articles/art-123/exists")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void validate_whenApiKeyIsWrong_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/internal/articles/art-123/exists")
                        .header("x-api-key", "wrong-key")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
