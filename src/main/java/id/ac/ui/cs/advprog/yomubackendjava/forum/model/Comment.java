package id.ac.ui.cs.advprog.yomubackendjava.forum.model;

import id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model.Article;
import id.ac.ui.cs.advprog.yomubackendjava.common.persistence.EntityTime;
import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "article_id", nullable = false)
    private String articleId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "article_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_comments_article")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Article article;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_comments_user")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "parent_comment_id",
            foreignKey = @ForeignKey(name = "fk_comments_parent_comment")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Comment> replies = new ArrayList<>();

    @OneToMany(mappedBy = "comment", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<CommentReaction> reactions = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = EntityTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = EntityTime.now();
    }
}
