package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "articles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Article {
    @Id
    private String id;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;
    private String category;
}