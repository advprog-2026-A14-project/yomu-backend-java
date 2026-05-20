package id.ac.ui.cs.advprog.yomubackendjava.forum.strategy;

import id.ac.ui.cs.advprog.yomubackendjava.forum.model.ReactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EmojiReactionStrategy implements ReactionStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmojiReactionStrategy.class);

    @Override
    public ReactionType getReactionType() {
        return ReactionType.EMOJI;
    }

    @Override
    public void onReactionAdded(UUID commentId, UUID userId) {
        LOGGER.debug("User {} menambah EMOJI pada komentar {}", userId, commentId);
    }

    @Override
    public void onReactionRemoved(UUID commentId, UUID userId) {
        LOGGER.debug("User {} mencabut EMOJI dari komentar {}", userId, commentId);
    }
}
