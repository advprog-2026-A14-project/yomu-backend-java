package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswerRequest {
    @JsonProperty("quiz_id")
    private String quizId;

    private String answer;
}