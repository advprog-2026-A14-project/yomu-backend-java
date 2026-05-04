package id.ac.ui.cs.advprog.yomubackendjava.forum.repository;

import id.ac.ui.cs.advprog.yomubackendjava.forum.model.CommentReaction;
import id.ac.ui.cs.advprog.yomubackendjava.forum.model.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, UUID> {

    Optional<CommentReaction> findByCommentIdAndUserIdAndReactionType(
            UUID commentId, UUID userId, ReactionType reactionType);

    int countByCommentIdAndReactionType(UUID commentId, ReactionType reactionType);

    void deleteByCommentId(UUID commentId);
}
