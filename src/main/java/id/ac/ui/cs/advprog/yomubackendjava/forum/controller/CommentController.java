package id.ac.ui.cs.advprog.yomubackendjava.forum.controller;

import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.CommentResponse;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.CreateCommentRequest;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.UpdateCommentRequest;
import id.ac.ui.cs.advprog.yomubackendjava.forum.exception.CommentNotFoundException;
import id.ac.ui.cs.advprog.yomubackendjava.forum.exception.UnauthorizedCommentAccessException;
import id.ac.ui.cs.advprog.yomubackendjava.forum.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/forum")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/articles/{articleId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable UUID articleId,
            @Valid @RequestBody CreateCommentRequest request) {

        CommentResponse response = commentService.createComment(articleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/articles/{articleId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable UUID articleId) {

        List<CommentResponse> responses = commentService.getCommentsByArticle(articleId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable UUID commentId,
            @RequestParam UUID userId,
            @Valid @RequestBody UpdateCommentRequest request) {

        CommentResponse response = commentService.updateComment(commentId, userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID commentId,
            @RequestParam UUID userId) {

        commentService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<String> handleNotFound(CommentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedCommentAccessException.class)
    public ResponseEntity<String> handleUnauthorized(UnauthorizedCommentAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
}
