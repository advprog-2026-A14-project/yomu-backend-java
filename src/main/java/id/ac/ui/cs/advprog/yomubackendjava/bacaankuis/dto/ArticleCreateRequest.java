package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCreateRequest {
    private String id;
    private String title;
    private String content;
    private String category;
}