package id.ac.ui.cs.advprog.yomubackendjava.forum.service;

import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.CommentResponse;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.CreateCommentRequest;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.UpdateCommentRequest;
import id.ac.ui.cs.advprog.yomubackendjava.forum.exception.CommentNotFoundException;
import id.ac.ui.cs.advprog.yomubackendjava.forum.exception.UnauthorizedCommentAccessException;
import id.ac.ui.cs.advprog.yomubackendjava.forum.model.Comment;
import id.ac.ui.cs.advprog.yomubackendjava.forum.repository.CommentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public CommentResponse createComment(UUID articleId, CreateCommentRequest request) {
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(request.getUserId());
        comment.setContent(request.getContent());

        if (request.getParentCommentId() != null) {
            Comment parent = findCommentOrThrow(request.getParentCommentId());
            comment.setParentComment(parent);
        }

        Comment saved = commentRepository.save(comment);
        return toResponse(saved);
    }

    public List<CommentResponse> getCommentsByArticle(UUID articleId) {
        List<Comment> rootComments = commentRepository
                .findByArticleIdAndParentCommentIsNullOrderByCreatedAtDesc(articleId);

        return rootComments.stream()
                .map(this::toResponseWithReplies)
                .collect(Collectors.toList());
    }

    public CommentResponse updateComment(UUID commentId, UUID userId, UpdateCommentRequest request) {
        Comment comment = findCommentOrThrow(commentId);
        validateOwnership(comment, userId);

        comment.setContent(request.getContent());
        Comment updated = commentRepository.save(comment);
        return toResponse(updated);
    }

    public void deleteComment(UUID commentId, UUID userId) {
        Comment comment = findCommentOrThrow(commentId);
        validateOwnership(comment, userId);
        commentRepository.delete(comment);
    }

    private Comment findCommentOrThrow(UUID commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
    }

    private void validateOwnership(Comment comment, UUID userId) {
        if (!comment.getUserId().equals(userId)) {
            throw new UnauthorizedCommentAccessException();
        }
    }

    private CommentResponse toResponse(Comment comment) {
        UUID parentId = comment.getParentComment() != null
                ? comment.getParentComment().getId()
                : null;

        return CommentResponse.builder()
                .id(comment.getId())
                .articleId(comment.getArticleId())
                .userId(comment.getUserId())
                .parentCommentId(parentId)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private CommentResponse toResponseWithReplies(Comment comment) {
        List<CommentResponse> replyResponses = comment.getReplies().stream()
                .map(this::toResponseWithReplies)
                .collect(Collectors.toList());

        UUID parentId = comment.getParentComment() != null
                ? comment.getParentComment().getId()
                : null;

        return CommentResponse.builder()
                .id(comment.getId())
                .articleId(comment.getArticleId())
                .userId(comment.getUserId())
                .parentCommentId(parentId)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .replies(replyResponses)
                .build();
    }
}
