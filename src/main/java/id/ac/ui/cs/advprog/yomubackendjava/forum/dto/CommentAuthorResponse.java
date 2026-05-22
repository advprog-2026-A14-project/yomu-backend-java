package id.ac.ui.cs.advprog.yomubackendjava.forum.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CommentAuthorResponse {
    @JsonProperty("user_id")
    private final UUID userId;
    private final String username;
    @JsonProperty("display_name")
    private final String displayName;
    private final String role;
}
