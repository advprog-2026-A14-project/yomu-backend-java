package id.ac.ui.cs.advprog.yomubackendjava.forum.controller;

import id.ac.ui.cs.advprog.yomubackendjava.security.JwtAuthFilter;
import tools.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.CommentResponse;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.CreateCommentRequest;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.UpdateCommentRequest;
import id.ac.ui.cs.advprog.yomubackendjava.forum.exception.CommentNotFoundException;
import id.ac.ui.cs.advprog.yomubackendjava.common.web.GlobalExceptionHandler;
import id.ac.ui.cs.advprog.yomubackendjava.forum.exception.UnauthorizedCommentAccessException;
import id.ac.ui.cs.advprog.yomubackendjava.forum.service.CommentService;
import id.ac.ui.cs.advprog.yomubackendjava.security.JwtService;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = CommentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthFilter.class
        )
)
@Import(GlobalExceptionHandler.class)
class CommentControllerTest {

    private static final String ARTICLE_COMMENTS_ENDPOINT = "/api/v1/forums/{articleId}/comments";
    private static final String COMMENT_BY_ID_ENDPOINT = "/api/v1/forums/comments/{commentId}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID articleId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID commentId = UUID.randomUUID();

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
    void createComment_validRequest_shouldReturn201() throws Exception {
        authenticateAs(userId, Role.PELAJAR);

        CreateCommentRequest request = new CreateCommentRequest(null, "Komentar baru");

        CommentResponse response = CommentResponse.builder()
                .id(commentId)
                .articleId(articleId)
                .userId(userId)
                .content("Komentar baru")
                .createdAt(Instant.now())
                .build();

        when(commentService.createComment(eq(articleId), any(CreateCommentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post(ARTICLE_COMMENTS_ENDPOINT, articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(commentId.toString()))
                .andExpect(jsonPath("$.data.content").value("Komentar baru"));
    }

    @Test
    void createComment_emptyContent_shouldReturn400() throws Exception {
        authenticateAs(userId, Role.PELAJAR);

        CreateCommentRequest request = new CreateCommentRequest(null, "");

        mockMvc.perform(post(ARTICLE_COMMENTS_ENDPOINT, articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getComments_shouldReturn200WithList() throws Exception {
        CommentResponse response = CommentResponse.builder()
                .id(commentId)
                .articleId(articleId)
                .userId(userId)
                .content("Komentar")
                .createdAt(Instant.now())
                .replies(List.of())
                .build();

        when(commentService.getCommentsByArticle(articleId)).thenReturn(List.of(response));

        mockMvc.perform(get(ARTICLE_COMMENTS_ENDPOINT, articleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(commentId.toString()));
    }

    @Test
    void updateComment_byOwner_shouldReturn200() throws Exception {
        authenticateAs(userId, Role.PELAJAR);

        UpdateCommentRequest request = new UpdateCommentRequest("Konten diperbarui");

        CommentResponse response = CommentResponse.builder()
                .id(commentId)
                .articleId(articleId)
                .userId(userId)
                .content("Konten diperbarui")
                .createdAt(Instant.now())
                .build();

        when(commentService.updateComment(eq(commentId), any(UpdateCommentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put(COMMENT_BY_ID_ENDPOINT, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("Konten diperbarui"));
    }

    @Test
    void updateComment_byNonOwner_shouldReturn403() throws Exception {
        authenticateAs(UUID.randomUUID(), Role.PELAJAR);

        UpdateCommentRequest request = new UpdateCommentRequest("Edit");

        when(commentService.updateComment(eq(commentId), any(UpdateCommentRequest.class)))
                .thenThrow(new UnauthorizedCommentAccessException());

        mockMvc.perform(put(COMMENT_BY_ID_ENDPOINT, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deleteComment_byOwner_shouldReturn200() throws Exception {
        authenticateAs(userId, Role.PELAJAR);

        doNothing().when(commentService).deleteComment(commentId);

        mockMvc.perform(delete(COMMENT_BY_ID_ENDPOINT, commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteComment_byAdmin_shouldReturn200() throws Exception {
        authenticateAs(UUID.randomUUID(), Role.ADMIN);

        doNothing().when(commentService).deleteComment(commentId);

        mockMvc.perform(delete(COMMENT_BY_ID_ENDPOINT, commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteComment_notFound_shouldReturn404() throws Exception {
        authenticateAs(userId, Role.PELAJAR);

        UUID fakeId = UUID.randomUUID();

        doThrow(new CommentNotFoundException(fakeId))
                .when(commentService).deleteComment(fakeId);

        mockMvc.perform(delete(COMMENT_BY_ID_ENDPOINT, fakeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}