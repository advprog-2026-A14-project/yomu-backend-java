package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.UUID;

@Data @AllArgsConstructor @NoArgsConstructor
public class QuizSyncRequest {
    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("article_id")
    private String articleId;

    private double score;
    private double accuracy;
}