package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model;

import id.ac.ui.cs.advprog.yomubackendjava.user.domain.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_attempts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_attempt_user_quiz",
                        columnNames = {"user_id", "kuis_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class UserAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "kuis_id", nullable = false)
    private String kuisId;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            referencedColumnName = "user_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_user_attempts_user")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "kuis_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_user_attempts_article")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Article article;
}
