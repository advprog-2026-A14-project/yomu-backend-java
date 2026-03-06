package id.ac.ui.cs.advprog.yomubackendjava.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    @NotNull(message = "User ID wajib diisi")
    private UUID userId;

    private UUID parentCommentId;

    @NotBlank(message = "Konten komentar tidak boleh kosong")
    @Size(max = 5000, message = "Konten komentar maksimal 5000 karakter")
    private String content;
}
