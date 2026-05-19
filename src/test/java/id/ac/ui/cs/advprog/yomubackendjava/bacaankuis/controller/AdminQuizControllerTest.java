package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizCreateRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizUpdateRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Quiz;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service.QuizManagementService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminQuizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private QuizManagementService quizManagementService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        public QuizManagementService quizManagementService() {
            return Mockito.mock(QuizManagementService.class);
        }
    }

    @Test
    void createQuiz_whenAdmin_returnsCreatedQuiz() throws Exception {
        String token = jwtService.generateToken(UUID.randomUUID(), Role.ADMIN);
        Quiz created = new Quiz("quiz-001", "art-001", "Apa ide utama teks?", "A;B;C;D", "A");

        when(quizManagementService.createQuiz(eq("art-001"), Mockito.any(QuizCreateRequest.class)))
                .thenReturn(created);

        String json = """
        {
            "id": "quiz-001",
            "question": "Apa ide utama teks?",
            "options": "A;B;C;D",
            "answer": "A"
        }
        """;

        mockMvc.perform(post("/api/v1/admin/articles/art-001/quizzes")
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("quiz-001"))
                .andExpect(jsonPath("$.data.article_id").value("art-001"));

        ArgumentCaptor<QuizCreateRequest> captor = ArgumentCaptor.forClass(QuizCreateRequest.class);
        verify(quizManagementService).createQuiz(eq("art-001"), captor.capture());
        assertEquals("quiz-001", captor.getValue().getId());
        assertEquals("A", captor.getValue().getAnswer());
    }

    @Test
    void createQuiz_whenPelajar_returnsForbidden() throws Exception {
        String token = jwtService.generateToken(UUID.randomUUID(), Role.PELAJAR);

        String json = """
        {
            "id": "quiz-001",
            "question": "Apa ide utama teks?",
            "options": "A;B;C;D",
            "answer": "A"
        }
        """;

        mockMvc.perform(post("/api/v1/admin/articles/art-001/quizzes")
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateQuiz_whenAdmin_returnsUpdatedQuiz() throws Exception {
        String token = jwtService.generateToken(UUID.randomUUID(), Role.ADMIN);
        Quiz updated = new Quiz("quiz-001", "art-001", "Pertanyaan baru?", "A;B;C;D", "B");

        when(quizManagementService.updateQuiz(eq("quiz-001"), Mockito.any(QuizUpdateRequest.class)))
                .thenReturn(updated);

        String json = """
        {
            "question": "Pertanyaan baru?",
            "options": "A;B;C;D",
            "answer": "B"
        }
        """;

        mockMvc.perform(patch("/api/v1/admin/quizzes/quiz-001")
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("quiz-001"))
                .andExpect(jsonPath("$.data.answer").value("B"));

        verify(quizManagementService).updateQuiz(eq("quiz-001"), Mockito.any(QuizUpdateRequest.class));
    }

    @Test
    void deleteQuiz_whenAdmin_returnsSuccess() throws Exception {
        String token = jwtService.generateToken(UUID.randomUUID(), Role.ADMIN);

        mockMvc.perform(delete("/api/v1/admin/quizzes/quiz-001")
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(quizManagementService).deleteQuiz("quiz-001");
    }
}