package id.ac.ui.cs.advprog.yomubackendjava.forum.model;

import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.UUID;

@Entity
@Table(
    name = "comment_reactions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_comment_reaction_user_type",
            columnNames = {"comment_id", "user_id", "reaction_type"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class CommentReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "comment_id", nullable = false)
    private UUID commentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false)
    private ReactionType reactionType;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "comment_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_comment_reactions_comment")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comment comment;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_comment_reactions_user")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    public CommentReaction(UUID id, UUID commentId, UUID userId, ReactionType reactionType) {
        this.id = id;
        this.commentId = commentId;
        this.userId = userId;
        this.reactionType = reactionType;
    }
}
