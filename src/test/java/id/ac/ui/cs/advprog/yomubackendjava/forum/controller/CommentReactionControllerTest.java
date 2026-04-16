package id.ac.ui.cs.advprog.yomubackendjava.forum.controller;

import tools.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.yomubackendjava.common.web.GlobalExceptionHandler;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.ReactionRequest;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.ReactionResponse;
import id.ac.ui.cs.advprog.yomubackendjava.forum.exception.CommentNotFoundException;
import id.ac.ui.cs.advprog.yomubackendjava.forum.model.ReactionType;
import id.ac.ui.cs.advprog.yomubackendjava.forum.service.CommentReactionService;
import id.ac.ui.cs.advprog.yomubackendjava.forum.service.CommentService;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.UnauthorizedException;
import id.ac.ui.cs.advprog.yomubackendjava.security.JwtAuthFilter;
import id.ac.ui.cs.advprog.yomubackendjava.security.JwtService;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CommentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class
        )
)
@Import(GlobalExceptionHandler.class)
class CommentReactionControllerTest {

    private static final String REACTION_ENDPOINT = "/api/v1/forums/comments/{commentId}/reactions";
    private static final String JSON_SUCCESS = "$.success";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private CommentReactionService reactionService;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID commentId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(UUID id, Role role) {
        JwtService.JwtClaims claims = new JwtService.JwtClaims(
                id, role, Instant.now(), Instant.now().plusSeconds(3600));
        String authority = "ROLE_" + role.name();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                claims, null, List.of(new SimpleGrantedAuthority(authority)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void toggleReaction_validRequest_shouldReturn200WithReactedTrue() throws Exception {
        authenticateAs(userId, Role.PELAJAR);

        ReactionRequest request = new ReactionRequest(ReactionType.UPVOTE);
        ReactionResponse response = ReactionResponse.builder()
                .commentId(commentId)
                .reactionType(ReactionType.UPVOTE)
                .reacted(true)
                .reactionCount(1)
                .build();

        when(reactionService.toggleReaction(eq(commentId), eq(ReactionType.UPVOTE)))
                .thenReturn(response);

        mockMvc.perform(post(REACTION_ENDPOINT, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_SUCCESS).value(true))
                .andExpect(jsonPath("$.data.reacted").value(true))
                .andExpect(jsonPath("$.data.reaction_count").value(1))
                .andExpect(jsonPath("$.data.reaction_type").value("UPVOTE"));
    }

    @Test
    void toggleReaction_alreadyReacted_shouldReturn200WithReactedFalse() throws Exception {
        authenticateAs(userId, Role.PELAJAR);

        ReactionRequest request = new ReactionRequest(ReactionType.UPVOTE);
        ReactionResponse response = ReactionResponse.builder()
                .commentId(commentId)
                .reactionType(ReactionType.UPVOTE)
                .reacted(false)
                .reactionCount(0)
                .build();

        when(reactionService.toggleReaction(eq(commentId), eq(ReactionType.UPVOTE)))
                .thenReturn(response);

        mockMvc.perform(post(REACTION_ENDPOINT, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_SUCCESS).value(true))
                .andExpect(jsonPath("$.data.reacted").value(false))
                .andExpect(jsonPath("$.data.reaction_count").value(0));
    }

    @Test
    void toggleReaction_noAuth_shouldReturn401() throws Exception {
        ReactionRequest request = new ReactionRequest(ReactionType.UPVOTE);

        when(reactionService.toggleReaction(any(), any()))
                .thenThrow(new UnauthorizedException("Login diperlukan"));

        mockMvc.perform(post(REACTION_ENDPOINT, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath(JSON_SUCCESS).value(false));
    }

    @Test
    void toggleReaction_nullReactionType_shouldReturn400() throws Exception {
        authenticateAs(userId, Role.PELAJAR);

        String invalidBody = "{\"reaction_type\": null}";

        mockMvc.perform(post(REACTION_ENDPOINT, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_SUCCESS).value(false));
    }

    @Test
    void toggleReaction_commentNotFound_shouldReturn404() throws Exception {
        authenticateAs(userId, Role.PELAJAR);

        ReactionRequest request = new ReactionRequest(ReactionType.UPVOTE);
        UUID fakeId = UUID.randomUUID();

        when(reactionService.toggleReaction(eq(fakeId), eq(ReactionType.UPVOTE)))
                .thenThrow(new CommentNotFoundException(fakeId));

        mockMvc.perform(post(REACTION_ENDPOINT, fakeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(JSON_SUCCESS).value(false));
    }

    @Test
    void getReactions_shouldReturn200WithCount() throws Exception {
        when(reactionService.getReactionCount(eq(commentId), eq(ReactionType.UPVOTE)))
                .thenReturn(5);

        mockMvc.perform(get(REACTION_ENDPOINT, commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_SUCCESS).value(true))
                .andExpect(jsonPath("$.data.reaction_count").value(5))
                .andExpect(jsonPath("$.data.reaction_type").value("UPVOTE"));
    }

    @Test
    void getReactions_commentNotFound_shouldReturn404() throws Exception {
        UUID fakeId = UUID.randomUUID();

        when(reactionService.getReactionCount(eq(fakeId), eq(ReactionType.UPVOTE)))
                .thenThrow(new CommentNotFoundException(fakeId));

        mockMvc.perform(get(REACTION_ENDPOINT, fakeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(JSON_SUCCESS).value(false));
    }
}
