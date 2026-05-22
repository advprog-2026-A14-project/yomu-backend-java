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

import static id.ac.ui.cs.advprog.yomubackendjava.common.persistence.PersistenceColumns.USER_ID;

@Entity
@Table(
    name = "comment_reactions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_comment_reaction_user_type",
            columnNames = {"comment_id", USER_ID, "reaction_type"}
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

    @Column(name = USER_ID, nullable = false)
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
            name = USER_ID,
            referencedColumnName = USER_ID,
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
