package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleUpdateRequest {
    private String title;
    private String content;
    private String category;
}
