package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service.QuizService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PublicQuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QuizService quizService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        public QuizService quizService() {
            return Mockito.mock(QuizService.class);
        }
    }

    @Test
    void testSubmitQuizPayloadUsesSnakeCase() throws Exception {

        String json = """
        {
            "user_id": "u1",
            "score": 100,
            "accuracy": 90.0
        }
        """;

        mockMvc.perform(post("/api/v1/quizzes/art-123/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}