package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.controller;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizCreateRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizUpdateRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Quiz;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service.QuizManagementService;
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
    private static final String ARTICLE_ID = "art-001";
    private static final String QUIZ_ID = "quiz-001";
    private static final String QUIZ_OPTIONS = "A;B;C;D";
    private static final String JSON_SUCCESS = "$.success";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private QuizManagementService quizManagementService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        public QuizManagementService quizManagementService() {
            return Mockito.mock(QuizManagementService.class);
        }
    }

    @Test
    void createQuiz_whenAdmin_returnsCreatedQuiz() throws Exception {
        String token = tokenFor(Role.ADMIN);
        Quiz created = new Quiz(QUIZ_ID, ARTICLE_ID, "Apa ide utama teks?", QUIZ_OPTIONS, "A");

        when(quizManagementService.createQuiz(eq(ARTICLE_ID), Mockito.any(QuizCreateRequest.class)))
                .thenReturn(created);

        String json = """
        {
            "id": "%s",
            "question": "Apa ide utama teks?",
            "options": "%s",
            "answer": "A"
        }
        """.formatted(QUIZ_ID, QUIZ_OPTIONS);

        mockMvc.perform(post("/api/v1/admin/articles/{articleId}/quizzes", ARTICLE_ID)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_SUCCESS).value(true))
                .andExpect(jsonPath("$.data.id").value(QUIZ_ID))
                .andExpect(jsonPath("$.data.article_id").value(ARTICLE_ID));

        ArgumentCaptor<QuizCreateRequest> captor = ArgumentCaptor.forClass(QuizCreateRequest.class);
        verify(quizManagementService).createQuiz(eq(ARTICLE_ID), captor.capture());
        assertEquals(QUIZ_ID, captor.getValue().getId());
        assertEquals("A", captor.getValue().getAnswer());
    }

    @Test
    void createQuiz_whenPelajar_returnsForbidden() throws Exception {
        String token = tokenFor(Role.PELAJAR);

        String json = """
        {
            "id": "%s",
            "question": "Apa ide utama teks?",
            "options": "%s",
            "answer": "A"
        }
        """.formatted(QUIZ_ID, QUIZ_OPTIONS);

        mockMvc.perform(post("/api/v1/admin/articles/{articleId}/quizzes", ARTICLE_ID)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(JSON_SUCCESS).value(false));
    }

    @Test
    void updateQuiz_whenAdmin_returnsUpdatedQuiz() throws Exception {
        String token = tokenFor(Role.ADMIN);
        Quiz updated = new Quiz(QUIZ_ID, ARTICLE_ID, "Pertanyaan baru?", QUIZ_OPTIONS, "B");

        when(quizManagementService.updateQuiz(eq(QUIZ_ID), Mockito.any(QuizUpdateRequest.class)))
                .thenReturn(updated);

        String json = """
        {
            "question": "Pertanyaan baru?",
            "options": "%s",
            "answer": "B"
        }
        """.formatted(QUIZ_OPTIONS);

        mockMvc.perform(patch("/api/v1/admin/quizzes/{quizId}", QUIZ_ID)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_SUCCESS).value(true))
                .andExpect(jsonPath("$.data.id").value(QUIZ_ID))
                .andExpect(jsonPath("$.data.answer").value("B"));

        verify(quizManagementService).updateQuiz(eq(QUIZ_ID), Mockito.any(QuizUpdateRequest.class));
    }

    @Test
    void deleteQuiz_whenAdmin_returnsSuccess() throws Exception {
        String token = tokenFor(Role.ADMIN);

        mockMvc.perform(delete("/api/v1/admin/quizzes/{quizId}", QUIZ_ID)
                        .header(JwtAuthFilter.AUTHORIZATION_HEADER, JwtAuthFilter.BEARER_PREFIX + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_SUCCESS).value(true));

        verify(quizManagementService).deleteQuiz(QUIZ_ID);
    }

    private String tokenFor(Role role) {
        UserEntity user = new UserEntity();
        user.setUsername("admin_quiz_" + role.name().toLowerCase());
        user.setDisplayName("Admin Quiz " + role.name());
        user.setRole(role);
        user.setPasswordHash("hash");
        UserEntity saved = userRepository.saveAndFlush(user);
        return jwtService.generateToken(saved.getUserId(), saved.getRole());
    }
}
