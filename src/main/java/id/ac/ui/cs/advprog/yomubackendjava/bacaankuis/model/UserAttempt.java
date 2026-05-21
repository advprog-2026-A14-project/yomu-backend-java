package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model;

import jakarta.persistence.*;
import lombok.*;

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
}