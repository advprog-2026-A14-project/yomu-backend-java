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
    void testValidateArticleFormat() throws Exception {
        String id = "art-123";

        when(articleService.checkArticleExists(id))
                .thenReturn(new ArticleStatusResponse(true, 1, "Edu"));

        mockMvc.perform(get("/api/internal/articles/" + id + "/exists")
                        .header("x-api-key", "secret")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.category_name").value("Edu"));
    }
}