package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmitRequest {
    private List<QuizAnswerRequest> answers;
}