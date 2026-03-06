package id.ac.ui.cs.advprog.yomubackendjava.forum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.CommentResponse;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.CreateCommentRequest;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.UpdateCommentRequest;
import id.ac.ui.cs.advprog.yomubackendjava.forum.exception.CommentNotFoundException;
import id.ac.ui.cs.advprog.yomubackendjava.forum.exception.UnauthorizedCommentAccessException;
import id.ac.ui.cs.advprog.yomubackendjava.forum.service.CommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
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

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID articleId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID commentId = UUID.randomUUID();

    @Test
    void createComment_validRequest_shouldReturn201() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest(userId, null, "Komentar baru");

        CommentResponse response = CommentResponse.builder()
                .id(commentId)
                .articleId(articleId)
                .userId(userId)
                .content("Komentar baru")
                .createdAt(Instant.now())
                .build();

        when(commentService.createComment(eq(articleId), any(CreateCommentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/forum/articles/{articleId}/comments", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(commentId.toString()))
                .andExpect(jsonPath("$.content").value("Komentar baru"));
    }

    @Test
    void createComment_emptyContent_shouldReturn400() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest(userId, null, "");

        mockMvc.perform(post("/api/forum/articles/{articleId}/comments", articleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
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

        mockMvc.perform(get("/api/forum/articles/{articleId}/comments", articleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(commentId.toString()));
    }

    @Test
    void updateComment_byOwner_shouldReturn200() throws Exception {
        UpdateCommentRequest request = new UpdateCommentRequest("Konten diperbarui");

        CommentResponse response = CommentResponse.builder()
                .id(commentId)
                .articleId(articleId)
                .userId(userId)
                .content("Konten diperbarui")
                .createdAt(Instant.now())
                .build();

        when(commentService.updateComment(eq(commentId), eq(userId), any(UpdateCommentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/forum/comments/{commentId}", commentId)
                        .param("userId", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Konten diperbarui"));
    }

    @Test
    void updateComment_byNonOwner_shouldReturn403() throws Exception {
        UUID otherUserId = UUID.randomUUID();
        UpdateCommentRequest request = new UpdateCommentRequest("Edit");

        when(commentService.updateComment(eq(commentId), eq(otherUserId), any(UpdateCommentRequest.class)))
                .thenThrow(new UnauthorizedCommentAccessException());

        mockMvc.perform(put("/api/forum/comments/{commentId}", commentId)
                        .param("userId", otherUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteComment_byOwner_shouldReturn204() throws Exception {
        doNothing().when(commentService).deleteComment(commentId, userId);

        mockMvc.perform(delete("/api/forum/comments/{commentId}", commentId)
                        .param("userId", userId.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteComment_notFound_shouldReturn404() throws Exception {
        UUID fakeId = UUID.randomUUID();

        doThrow(new CommentNotFoundException(fakeId))
                .when(commentService).deleteComment(eq(fakeId), eq(userId));

        mockMvc.perform(delete("/api/forum/comments/{commentId}", fakeId)
                        .param("userId", userId.toString()))
                .andExpect(status().isNotFound());
    }
}
