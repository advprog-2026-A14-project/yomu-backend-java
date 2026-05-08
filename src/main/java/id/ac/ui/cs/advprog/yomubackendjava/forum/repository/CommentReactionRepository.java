package id.ac.ui.cs.advprog.yomubackendjava.forum.repository;

import id.ac.ui.cs.advprog.yomubackendjava.forum.model.CommentReaction;
import id.ac.ui.cs.advprog.yomubackendjava.forum.model.ReactionType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, UUID> {

    Optional<CommentReaction> findByCommentIdAndUserIdAndReactionType(
            UUID commentId, UUID userId, ReactionType reactionType);

    int countByCommentIdAndReactionType(UUID commentId, ReactionType reactionType);

    void deleteByCommentId(UUID commentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cr FROM CommentReaction cr WHERE cr.commentId = :commentId AND cr.userId = :userId AND cr.reactionType = :reactionType")
    Optional<CommentReaction> findByCommentIdAndUserIdAndReactionTypeForUpdate(
        @Param("commentId") UUID commentId,
        @Param("userId") UUID userId,
        @Param("reactionType") ReactionType reactionType);
}
