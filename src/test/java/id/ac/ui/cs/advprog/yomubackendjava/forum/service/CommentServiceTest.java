package id.ac.ui.cs.advprog.yomubackendjava.forum.service;

import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.CommentResponse;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.CreateCommentRequest;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.UpdateCommentRequest;
import id.ac.ui.cs.advprog.yomubackendjava.forum.exception.CommentNotFoundException;
import id.ac.ui.cs.advprog.yomubackendjava.forum.exception.UnauthorizedCommentAccessException;
import id.ac.ui.cs.advprog.yomubackendjava.forum.model.Comment;
import id.ac.ui.cs.advprog.yomubackendjava.forum.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentService commentService;

    private UUID articleId;
    private UUID userId;
    private UUID commentId;
    private Comment sampleComment;

    @BeforeEach
    void setUp() {
        articleId = UUID.randomUUID();
        userId = UUID.randomUUID();
        commentId = UUID.randomUUID();

        sampleComment = new Comment();
        sampleComment.setId(commentId);
        sampleComment.setArticleId(articleId);
        sampleComment.setUserId(userId);
        sampleComment.setContent("Ini komentar pertama");
        sampleComment.setCreatedAt(Instant.now());
        sampleComment.setReplies(new ArrayList<>());
    }

    // ========== CREATE ==========

    @Test
    void createRootComment_shouldReturnResponse() {
        CreateCommentRequest request = new CreateCommentRequest(userId, null, "Komentar baru");

        when(commentRepository.save(any(Comment.class))).thenReturn(sampleComment);

        CommentResponse response = commentService.createComment(articleId, request);

        assertNotNull(response);
        assertEquals(articleId, response.getArticleId());
        assertEquals(userId, response.getUserId());
        assertNull(response.getParentCommentId());
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    void createReply_shouldSetParentComment() {
        Comment replyComment = new Comment();
        replyComment.setId(UUID.randomUUID());
        replyComment.setArticleId(articleId);
        replyComment.setUserId(userId);
        replyComment.setContent("Ini reply");
        replyComment.setParentComment(sampleComment);
        replyComment.setCreatedAt(Instant.now());
        replyComment.setReplies(new ArrayList<>());

        CreateCommentRequest request = new CreateCommentRequest(userId, commentId, "Ini reply");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(sampleComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(replyComment);

        CommentResponse response = commentService.createComment(articleId, request);

        assertNotNull(response);
        assertEquals(commentId, response.getParentCommentId());
        verify(commentRepository).findById(commentId);
    }

    @Test
    void createReply_parentNotFound_shouldThrow() {
        UUID fakeParentId = UUID.randomUUID();
        CreateCommentRequest request = new CreateCommentRequest(userId, fakeParentId, "Reply");

        when(commentRepository.findById(fakeParentId)).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class,
                () -> commentService.createComment(articleId, request));
    }

    // ========== READ ==========

    @Test
    void getCommentsByArticle_shouldReturnRootComments() {
        when(commentRepository.findByArticleIdAndParentCommentIsNullOrderByCreatedAtDesc(articleId))
                .thenReturn(List.of(sampleComment));

        List<CommentResponse> responses = commentService.getCommentsByArticle(articleId);

        assertEquals(1, responses.size());
        assertEquals(commentId, responses.get(0).getId());
    }

    @Test
    void getCommentsByArticle_noComments_shouldReturnEmptyList() {
        when(commentRepository.findByArticleIdAndParentCommentIsNullOrderByCreatedAtDesc(articleId))
                .thenReturn(List.of());

        List<CommentResponse> responses = commentService.getCommentsByArticle(articleId);

        assertTrue(responses.isEmpty());
    }

    // ========== UPDATE ==========

    @Test
    void updateComment_byOwner_shouldSucceed() {
        UpdateCommentRequest request = new UpdateCommentRequest("Konten diperbarui");

        Comment updatedComment = new Comment();
        updatedComment.setId(commentId);
        updatedComment.setArticleId(articleId);
        updatedComment.setUserId(userId);
        updatedComment.setContent("Konten diperbarui");
        updatedComment.setCreatedAt(sampleComment.getCreatedAt());
        updatedComment.setReplies(new ArrayList<>());

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(sampleComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(updatedComment);

        CommentResponse response = commentService.updateComment(commentId, userId, request);

        assertEquals("Konten diperbarui", response.getContent());
    }

    @Test
    void updateComment_byNonOwner_shouldThrowUnauthorized() {
        UUID otherUserId = UUID.randomUUID();
        UpdateCommentRequest request = new UpdateCommentRequest("Coba edit");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(sampleComment));

        assertThrows(UnauthorizedCommentAccessException.class,
                () -> commentService.updateComment(commentId, otherUserId, request));
    }

    @Test
    void updateComment_notFound_shouldThrow() {
        UUID fakeId = UUID.randomUUID();
        UpdateCommentRequest request = new UpdateCommentRequest("Edit");

        when(commentRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class,
                () -> commentService.updateComment(fakeId, userId, request));
    }

    // ========== DELETE ==========

    @Test
    void deleteComment_byOwner_shouldSucceed() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(sampleComment));

        assertDoesNotThrow(() -> commentService.deleteComment(commentId, userId));
        verify(commentRepository).delete(sampleComment);
    }

    @Test
    void deleteComment_byNonOwner_shouldThrowUnauthorized() {
        UUID otherUserId = UUID.randomUUID();

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(sampleComment));

        assertThrows(UnauthorizedCommentAccessException.class,
                () -> commentService.deleteComment(commentId, otherUserId));
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void deleteComment_notFound_shouldThrow() {
        UUID fakeId = UUID.randomUUID();

        when(commentRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class,
                () -> commentService.deleteComment(fakeId, userId));
    }
}
