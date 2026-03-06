package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ArticleStatusResponse {
    private final boolean exists;
    @JsonProperty("category_id")
    private final int categoryId;
    @JsonProperty("category_name")
    private final String categoryName;
}