package id.ac.ui.cs.advprog.yomubackendjava.forum.strategy;

import id.ac.ui.cs.advprog.yomubackendjava.forum.model.ReactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UpvoteReactionStrategy implements ReactionStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpvoteReactionStrategy.class);

    @Override
    public ReactionType getReactionType() {
        return ReactionType.UPVOTE;
    }

    @Override
    public void onReactionAdded(UUID commentId, UUID userId) {
        LOGGER.debug("User {} menambah UPVOTE pada komentar {}", userId, commentId);
    }

    @Override
    public void onReactionRemoved(UUID commentId, UUID userId) {
        LOGGER.debug("User {} mencabut UPVOTE dari komentar {}", userId, commentId);
    }
}
