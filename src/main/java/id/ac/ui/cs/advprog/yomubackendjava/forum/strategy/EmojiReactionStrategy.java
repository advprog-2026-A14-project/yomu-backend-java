package id.ac.ui.cs.advprog.yomubackendjava.forum.strategy;

import id.ac.ui.cs.advprog.yomubackendjava.forum.model.ReactionType;
import org.springframework.stereotype.Component;

@Component
public class EmojiReactionStrategy extends AbstractLoggingReactionStrategy {
    public EmojiReactionStrategy() {
        super(ReactionType.EMOJI);
    }
}
