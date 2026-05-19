package id.ac.ui.cs.advprog.yomubackendjava.bacaankuis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizCreateRequest {
    private String id;
    private String question;
    private String options;
    private String answer;
}