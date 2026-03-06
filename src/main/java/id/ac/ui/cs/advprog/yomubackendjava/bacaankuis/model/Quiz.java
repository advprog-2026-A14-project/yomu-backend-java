package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quizzes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Quiz {
    @Id
    private String id;
    @Column(name = "article_id")
    private String articleId;
    private String question;
    private String options;
    private String answer;
}