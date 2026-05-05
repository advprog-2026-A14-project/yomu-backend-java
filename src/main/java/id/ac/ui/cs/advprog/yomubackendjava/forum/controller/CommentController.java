package id.ac.ui.cs.advprog.yomubackendjava.forum.controller;

import id.ac.ui.cs.advprog.yomubackendjava.common.api.ApiResponse;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.CommentResponse;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.CreateCommentRequest;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.ReactionRequest;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.ReactionResponse;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.UpdateCommentRequest;
import id.ac.ui.cs.advprog.yomubackendjava.forum.service.CommentReactionService;
import id.ac.ui.cs.advprog.yomubackendjava.forum.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forums")
public class CommentController {

    private final CommentService commentService;
    private final CommentReactionService reactionService;

    public CommentController(CommentService commentService, CommentReactionService reactionService) {
        this.commentService = commentService;
        this.reactionService = reactionService;
    }

    @PostMapping("/{articleId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable UUID articleId,
            @Valid @RequestBody CreateCommentRequest request) {

        CommentResponse data = commentService.createComment(articleId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Komentar berhasil dibuat", data));
    }

    @GetMapping("/{articleId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable UUID articleId) {

        List<CommentResponse> data = commentService.getCommentsByArticle(articleId);
        return ResponseEntity.ok(ApiResponse.success("Komentar berhasil diambil", data));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request) {

        CommentResponse data = commentService.updateComment(commentId, request);
        return ResponseEntity.ok(ApiResponse.success("Komentar berhasil diperbarui", data));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID commentId) {

        commentService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.success("Komentar berhasil dihapus"));
    }

    @PostMapping("/comments/{commentId}/reactions")
    public ResponseEntity<ApiResponse<ReactionResponse>> toggleReaction(
            @PathVariable UUID commentId,
            @Valid @RequestBody ReactionRequest request) {

        ReactionResponse data = reactionService.toggleReaction(commentId, request.getReactionType());
        return ResponseEntity.ok(ApiResponse.success("Reaksi berhasil diperbarui", data));
    }

    @GetMapping("/comments/{commentId}/reactions")
    public ResponseEntity<ApiResponse<ReactionResponse>> getReactions(
            @PathVariable UUID commentId) {

        int count = reactionService.getReactionCount(commentId, id.ac.ui.cs.advprog.yomubackendjava.forum.model.ReactionType.UPVOTE);
        ReactionResponse data = ReactionResponse.builder()
                .commentId(commentId)
                .reactionType(id.ac.ui.cs.advprog.yomubackendjava.forum.model.ReactionType.UPVOTE)
                .reactionCount(count)
                .build();
        return ResponseEntity.ok(ApiResponse.success("Reaksi berhasil diambil", data));
    }
}