package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "quizzes")
@Getter @Setter
@NoArgsConstructor
public class Quiz {
    @Id
    private String id;
    @Column(name = "article_id")
    private String articleId;
    private String question;
    private String options;
    private String answer;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "article_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_quizzes_article")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Article article;

    public Quiz(String id, String articleId, String question, String options, String answer) {
        this.id = id;
        this.articleId = articleId;
        this.question = question;
        this.options = options;
        this.answer = answer;
    }
}
