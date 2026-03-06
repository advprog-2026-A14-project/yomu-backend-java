package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuizSyncRequest {
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("article_id")
    private String articleId;
    private int score;
    private double accuracy;
}