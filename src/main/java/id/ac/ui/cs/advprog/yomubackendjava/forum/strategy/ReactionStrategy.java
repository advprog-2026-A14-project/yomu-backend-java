package id.ac.ui.cs.advprog.yomubackendjava.forum.strategy;

import id.ac.ui.cs.advprog.yomubackendjava.forum.model.ReactionType;

import java.util.UUID;

public interface ReactionStrategy {
    ReactionType getReactionType();
    void onReactionAdded(UUID commentId, UUID userId);
    void onReactionRemoved(UUID commentId, UUID userId);
}
