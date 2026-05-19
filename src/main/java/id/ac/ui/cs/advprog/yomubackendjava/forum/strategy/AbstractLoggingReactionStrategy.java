package id.ac.ui.cs.advprog.yomubackendjava.forum.strategy;

import id.ac.ui.cs.advprog.yomubackendjava.forum.model.ReactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public abstract class AbstractLoggingReactionStrategy implements ReactionStrategy {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ReactionType reactionType;

    protected AbstractLoggingReactionStrategy(ReactionType reactionType) {
        this.reactionType = reactionType;
    }

    @Override
    public ReactionType getReactionType() {
        return reactionType;
    }

    @Override
    public void onReactionAdded(UUID commentId, UUID userId) {
        logger.debug("User {} menambah {} pada komentar {}", userId, reactionType, commentId);
    }

    @Override
    public void onReactionRemoved(UUID commentId, UUID userId) {
        logger.debug("User {} mencabut {} dari komentar {}", userId, reactionType, commentId);
    }
}
