package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service.QuizService;
import id.ac.ui.cs.advprog.yomubackendjava.security.JwtAuthFilter;
import id.ac.ui.cs.advprog.yomubackendjava.security.JwtService;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PublicQuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

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
        String token = jwtService.generateToken(UUID.randomUUID(), Role.PELAJAR);

        String json = """
        {
            "user_id": "550e8400-e29b-41d4-a716-446655440000",
            "score": 100,
            "accuracy": 90.0
        }
        """;

        mockMvc.perform(post("/api/v1/quizzes/art-123/submit")
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void submitQuiz_whenUnauthenticated_returnsUnauthorized() throws Exception {
        String json = """
        {
            "user_id": "550e8400-e29b-41d4-a716-446655440000",
            "score": 100,
            "accuracy": 90.0
        }
        """;

        mockMvc.perform(post("/api/v1/quizzes/art-123/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
