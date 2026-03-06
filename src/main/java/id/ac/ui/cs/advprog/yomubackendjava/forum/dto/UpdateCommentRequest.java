package id.ac.ui.cs.advprog.yomubackendjava.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentRequest {

    @NotBlank(message = "Konten komentar tidak boleh kosong")
    @Size(max = 5000, message = "Konten komentar maksimal 5000 karakter")
    private String content;
}
