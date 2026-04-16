package id.ac.ui.cs.advprog.yomubackendjava.forum.service;

import id.ac.ui.cs.advprog.yomubackendjava.forum.dto.ReactionResponse;
import id.ac.ui.cs.advprog.yomubackendjava.forum.model.ReactionType;

import java.util.UUID;

public interface ICommentReactionService {
    ReactionResponse toggleReaction(UUID commentId, ReactionType reactionType);
    int getReactionCount(UUID commentId, ReactionType reactionType);
}
