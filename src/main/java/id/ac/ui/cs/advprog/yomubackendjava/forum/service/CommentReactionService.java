package id.ac.ui.cs.advprog.yomubackendjava.forum.service;

import id.ac.ui.cs.advprog.yomubackendjava.common.exception.UnauthorizedException;
import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.ReactionResponse;
import id.ac.ui.cs.advprog.yomubackendjava.forum.exception.CommentNotFoundException;
import id.ac.ui.cs.advprog.yomubackendjava.forum.model.CommentReaction;
import id.ac.ui.cs.advprog.yomubackendjava.forum.model.ReactionType;
import id.ac.ui.cs.advprog.yomubackendjava.forum.repository.CommentReactionRepository;
import id.ac.ui.cs.advprog.yomubackendjava.forum.repository.CommentRepository;
import id.ac.ui.cs.advprog.yomubackendjava.forum.strategy.ReactionStrategy;
import id.ac.ui.cs.advprog.yomubackendjava.forum.strategy.ReactionStrategyFactory;
import id.ac.ui.cs.advprog.yomubackendjava.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class CommentReactionService implements ICommentReactionService {

    private static final String LOGIN_REQUIRED_MESSAGE = "Login diperlukan";

    private final CommentReactionRepository reactionRepository;
    private final CommentRepository commentRepository;
    private final ReactionStrategyFactory strategyFactory;

    public CommentReactionService(
            CommentReactionRepository reactionRepository,
            CommentRepository commentRepository,
            ReactionStrategyFactory strategyFactory
    ) {
        this.reactionRepository = reactionRepository;
        this.commentRepository = commentRepository;
        this.strategyFactory = strategyFactory;
    }

    @Override
    @Transactional
    public ReactionResponse toggleReaction(UUID commentId, ReactionType reactionType) {
        UUID userId = CurrentUser.userId()
                .orElseThrow(() -> new UnauthorizedException(LOGIN_REQUIRED_MESSAGE));

        assertCommentExists(commentId);

        ReactionStrategy strategy = strategyFactory.resolve(reactionType);
        Optional<CommentReaction> existing = reactionRepository
                .findByCommentIdAndUserIdAndReactionTypeForUpdate(commentId, userId, reactionType);

        boolean reacted;
        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            strategy.onReactionRemoved(commentId, userId);
            reacted = false;
        } else {
            reactionRepository.save(new CommentReaction(null, commentId, userId, reactionType));
            strategy.onReactionAdded(commentId, userId);
            reacted = true;
        }

        int count = reactionRepository.countByCommentIdAndReactionType(commentId, reactionType);

        return ReactionResponse.builder()
                .commentId(commentId)
                .reactionType(reactionType)
                .reacted(reacted)
                .reactionCount(count)
                .build();
    }

    @Override
    public int getReactionCount(UUID commentId, ReactionType reactionType) {
        assertCommentExists(commentId);
        return reactionRepository.countByCommentIdAndReactionType(commentId, reactionType);
    }

    private void assertCommentExists(UUID commentId) {
        commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
    }
}
