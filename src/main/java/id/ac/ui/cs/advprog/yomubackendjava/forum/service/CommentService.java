package id.ac.ui.cs.advprog.yomubackendjava.forum.service;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.service.ArticleService;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.BadRequestException;
import id.ac.ui.cs.advprog.yomubackendjava.common.exception.UnauthorizedException;
import id.ac.ui.cs.advprog.yomubackendjava.common.security.SecuritySanitizer;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.CommentResponse;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.CreateCommentRequest;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.UpdateCommentRequest;
import id.ac.ui.cs.advprog.yomubackendjava.forum.exception.CommentNotFoundException;
import id.ac.ui.cs.advprog.yomubackendjava.forum.exception.UnauthorizedCommentAccessException;
import id.ac.ui.cs.advprog.yomubackendjava.integration.rust.RustLeagueClient;
import id.ac.ui.cs.advprog.yomubackendjava.forum.model.Comment;
import id.ac.ui.cs.advprog.yomubackendjava.forum.model.ReactionType;
import id.ac.ui.cs.advprog.yomubackendjava.forum.repository.CommentRepository;
import id.ac.ui.cs.advprog.yomubackendjava.security.CurrentUser;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.Role;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private static final String LOGIN_REQUIRED_MESSAGE = "Login diperlukan";

    private final CommentRepository commentRepository;
    private final ICommentReactionService reactionService;
    private final RustLeagueClient rustLeagueClient;
    private final ArticleService articleService;

    public CommentService(
            CommentRepository commentRepository,
            @Lazy ICommentReactionService reactionService,
            RustLeagueClient rustLeagueClient,
            ArticleService articleService
    ) {
        this.commentRepository = commentRepository;
        this.reactionService = reactionService;
        this.rustLeagueClient = rustLeagueClient;
        this.articleService = articleService;
    }

    public CommentResponse createComment(String articleId, CreateCommentRequest request) {
        UUID userId = CurrentUser.userId()
                .orElseThrow(() -> new UnauthorizedException(LOGIN_REQUIRED_MESSAGE));
        ensureArticleExists(articleId);

        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(SecuritySanitizer.html(request.getContent()));

        if (request.getParentCommentId() != null) {
            Comment parent = findCommentOrThrow(request.getParentCommentId());
            validateParentCommentBelongsToArticle(parent, articleId);
            comment.setParentComment(parent);
        }

        Comment saved = commentRepository.save(comment);
        return toResponse(saved);
    }

    public List<CommentResponse> getCommentsByArticle(String articleId) {
        ensureArticleExists(articleId);

        List<Comment> rootComments = commentRepository
                .findByArticleIdAndParentCommentIsNullOrderByCreatedAtDesc(articleId);

        return rootComments.stream()
                .map(this::toResponseWithReplies)
                .collect(Collectors.toList());
    }

    public CommentResponse updateComment(UUID commentId, UpdateCommentRequest request) {
        UUID userId = CurrentUser.userId()
                .orElseThrow(() -> new UnauthorizedException(LOGIN_REQUIRED_MESSAGE));

        Comment comment = findCommentOrThrow(commentId);
        validateOwnership(comment, userId);

        comment.setContent(SecuritySanitizer.html(request.getContent()));
        Comment updated = commentRepository.save(comment);
        return toResponse(updated);
    }

    public void deleteComment(UUID commentId) {
        UUID userId = CurrentUser.userId()
                .orElseThrow(() -> new UnauthorizedException(LOGIN_REQUIRED_MESSAGE));
        Role role = CurrentUser.role().orElse(Role.PELAJAR);

        Comment comment = findCommentOrThrow(commentId);
        if (role != Role.ADMIN) {
            validateOwnership(comment, userId);
        }
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

    private void ensureArticleExists(String articleId) {
        articleService.findById(articleId);
    }

    private void validateParentCommentBelongsToArticle(Comment parent, String articleId) {
        if (!articleId.equals(parent.getArticleId())) {
            throw new BadRequestException("Parent comment tidak sesuai dengan artikel tujuan");
        }
    }

    private CommentResponse toResponse(Comment comment) {
        return buildResponse(comment, null);
    }

    private CommentResponse toResponseWithReplies(Comment comment) {
        List<CommentResponse> replyResponses = comment.getReplies().stream()
                .map(this::toResponseWithReplies)
                .collect(Collectors.toList());

        return buildResponse(comment, replyResponses);
    }

    private CommentResponse buildResponse(Comment comment, List<CommentResponse> replies) {
        UUID parentId = comment.getParentComment() != null
                ? comment.getParentComment().getId()
                : null;

        int upvoteCount = reactionService.getReactionCount(comment.getId(), ReactionType.UPVOTE);
        int downvoteCount = reactionService.getReactionCount(comment.getId(), ReactionType.DOWNVOTE);
        int emojiCount = reactionService.getReactionCount(comment.getId(), ReactionType.EMOJI);

        String clanName = null;
        String tier = null;
        try {
            RustLeagueClient.UserTierResponse tierResponse =
                    rustLeagueClient.getUserTier(comment.getUserId());
            if (tierResponse != null) {
                clanName = SecuritySanitizer.html(tierResponse.clanName());
                tier = SecuritySanitizer.html(tierResponse.tier());
            }
        } catch (Exception e) {
            log.debug("Tier fetch failed for comment user: {}", e.getMessage());
        }

        return CommentResponse.builder()
                .id(comment.getId())
                .articleId(comment.getArticleId())
                .userId(comment.getUserId())
                .parentCommentId(parentId)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .reactionCount(upvoteCount)
                .upvoteCount(upvoteCount)
                .downvoteCount(downvoteCount)
                .emojiCount(emojiCount)
                .replies(replies)
                .clanName(clanName)
                .tier(tier)
                .build();
    }
}
