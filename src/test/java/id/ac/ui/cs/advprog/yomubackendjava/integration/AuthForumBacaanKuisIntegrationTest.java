package id.ac.ui.cs.advprog.yomubackendjava.integration;

import com.jayway.jsonpath.JsonPath;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto.QuizSyncRequest;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.integration.QuizSyncClient;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Article;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Quiz;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.ArticleRepository;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.QuizRepository;
import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.repository.UserAttemptRepository;
import id.ac.ui.cs.advprog.yomubackendjava.forum.repository.CommentReactionRepository;
import id.ac.ui.cs.advprog.yomubackendjava.forum.repository.CommentRepository;
import id.ac.ui.cs.advprog.yomubackendjava.integration.rust.RustEngineClient;
import id.ac.ui.cs.advprog.yomubackendjava.integration.rust.RustLeagueClient;
import id.ac.ui.cs.advprog.yomubackendjava.outbox.repo.FailedSyncEventRepository;
import id.ac.ui.cs.advprog.yomubackendjava.user.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthForumBacaanKuisIntegrationTest.MockExternalServices.class)
class AuthForumBacaanKuisIntegrationTest {
    private static final String ARTICLE_ID = "article-auth-forum-quiz-integration";
    private static final String REGISTER_PATH = "/api/v1/auth/register";
    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String FORUM_COMMENTS_PATH = "/api/v1/forums/{articleId}/comments";
    private static final String FORUM_REACTIONS_PATH = "/api/v1/forums/comments/{commentId}/reactions";
    private static final String QUIZ_PATH = "/api/v1/quizzes/{articleId}";
    private static final String QUIZ_SUBMIT_PATH = "/api/v1/quizzes/{articleId}/submit";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SUCCESS_JSON_PATH = "$.success";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private UserAttemptRepository userAttemptRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentReactionRepository commentReactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FailedSyncEventRepository failedSyncEventRepository;

    @Autowired
    private RustEngineClient rustEngineClient;

    @Autowired
    private RustLeagueClient rustLeagueClient;

    @Autowired
    private QuizSyncClient quizSyncClient;

    @BeforeEach
    void setUp() {
        Mockito.reset(rustEngineClient, rustLeagueClient, quizSyncClient);
        commentReactionRepository.deleteAll();
        commentRepository.deleteAll();
        userAttemptRepository.deleteAll();
        quizRepository.deleteAll();
        articleRepository.deleteAll();
        failedSyncEventRepository.deleteAll();
        userRepository.deleteAll();

        when(rustEngineClient.syncUser(any(UUID.class)))
                .thenReturn(new RustEngineClient.SyncResult(201, "created"));
        when(rustLeagueClient.getUserTier(any(UUID.class)))
                .thenAnswer(invocation -> new RustLeagueClient.UserTierResponse(
                        invocation.getArgument(0),
                        UUID.randomUUID(),
                        "Clan Integrasi",
                        "Gold"
                ));

        seedArticleAndQuizzes();
    }

    @Test
    void authenticatedUserCanUseForumAndBacaanKuisFlow() throws Exception {
        MvcResult registerResult = registerUser();
        UUID userId = UUID.fromString(readJson(registerResult, "$.data.user.user_id"));

        String accessToken = loginAndReadToken();

        MvcResult commentResult = mockMvc.perform(post(FORUM_COMMENTS_PATH, ARTICLE_ID)
                        .header(AUTHORIZATION, bearer(accessToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Komentar integrasi dari user terautentikasi"
                                }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(true))
                .andExpect(jsonPath("$.data.article_id").value(ARTICLE_ID))
                .andExpect(jsonPath("$.data.user_id").value(userId.toString()))
                .andReturn();
        UUID commentId = UUID.fromString(readJson(commentResult, "$.data.id"));

        mockMvc.perform(post(FORUM_REACTIONS_PATH, commentId)
                        .header(AUTHORIZATION, bearer(accessToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reaction_type": "UPVOTE"
                                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(true))
                .andExpect(jsonPath("$.data.reacted").value(true))
                .andExpect(jsonPath("$.data.reaction_count").value(1));

        mockMvc.perform(get(FORUM_COMMENTS_PATH, ARTICLE_ID)
                        .header(AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(true))
                .andExpect(jsonPath("$.data[0].id").value(commentId.toString()))
                .andExpect(jsonPath("$.data[0].reaction_count").value(1))
                .andExpect(jsonPath("$.data[0].clan_name").value("Clan Integrasi"))
                .andExpect(jsonPath("$.data[0].tier").value("Gold"));

        mockMvc.perform(get(QUIZ_PATH, ARTICLE_ID)
                        .header(AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(true))
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(post(QUIZ_SUBMIT_PATH, ARTICLE_ID)
                        .header(AUTHORIZATION, bearer(accessToken))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {"quiz_id": "quiz-int-1", "answer": "A"},
                                    {"quiz_id": "quiz-int-2", "answer": "C"}
                                  ]
                                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(true))
                .andExpect(jsonPath("$.data.score").value(100.0))
                .andExpect(jsonPath("$.data.accuracy").value(100.0))
                .andExpect(jsonPath("$.data.correct_count").value(2))
                .andExpect(jsonPath("$.data.total_questions").value(2));

        assertThat(userAttemptRepository.existsByUserIdAndKuisId(userId, ARTICLE_ID)).isTrue();
        verify(rustEngineClient).syncUser(userId);
        verify(quizSyncClient).sync(argThat(request -> quizSyncMatches(request, userId)));
    }

    @Test
    void protectedForumAndQuizActionsRejectMissingToken() throws Exception {
        mockMvc.perform(post(FORUM_COMMENTS_PATH, ARTICLE_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Komentar tanpa token"
                                }
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(false));

        mockMvc.perform(get(QUIZ_PATH, ARTICLE_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(false));
    }

    private MvcResult registerUser() throws Exception {
        return mockMvc.perform(post(REGISTER_PATH)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "integration_user",
                                  "display_name": "Integration User",
                                  "password": "rahasia123",
                                  "email": "integration.user@example.com"
                                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(true))
                .andExpect(jsonPath("$.data.access_token").isNotEmpty())
                .andReturn();
    }

    private String loginAndReadToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post(LOGIN_PATH)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "integration_user",
                                  "password": "rahasia123"
                                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SUCCESS_JSON_PATH).value(true))
                .andExpect(jsonPath("$.data.access_token").isNotEmpty())
                .andReturn();
        return readJson(loginResult, "$.data.access_token");
    }

    private void seedArticleAndQuizzes() {
        articleRepository.saveAndFlush(new Article(
                ARTICLE_ID,
                "Artikel Integrasi",
                "Konten artikel untuk integration testing auth, forum, dan kuis.",
                "Testing"
        ));
        quizRepository.saveAndFlush(new Quiz(
                "quiz-int-1",
                ARTICLE_ID,
                "Pertanyaan integrasi pertama",
                "A;B;C;D",
                "A"
        ));
        quizRepository.saveAndFlush(new Quiz(
                "quiz-int-2",
                ARTICLE_ID,
                "Pertanyaan integrasi kedua",
                "A;B;C;D",
                "C"
        ));
    }

    private String readJson(MvcResult result, String path) throws Exception {
        return JsonPath.parse(result.getResponse().getContentAsString()).read(path, String.class);
    }

    private String bearer(String token) {
        return BEARER_PREFIX + token;
    }

    private boolean quizSyncMatches(QuizSyncRequest request, UUID userId) {
        return userId.equals(request.getUserId())
                && ARTICLE_ID.equals(request.getArticleId())
                && request.getScore() == 100.0
                && request.getAccuracy() == 100.0;
    }

    @TestConfiguration
    static class MockExternalServices {
        @Bean
        @Primary
        RustEngineClient rustEngineClient() {
            return Mockito.mock(RustEngineClient.class);
        }

        @Bean
        @Primary
        RustLeagueClient rustLeagueClient() {
            return Mockito.mock(RustLeagueClient.class);
        }

        @Bean
        @Primary
        QuizSyncClient quizSyncClient() {
            return Mockito.mock(QuizSyncClient.class);
        }
    }
}
