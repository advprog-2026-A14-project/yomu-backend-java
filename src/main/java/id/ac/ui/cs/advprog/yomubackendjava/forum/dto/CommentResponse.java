package id.ac.ui.cs.advprog.yomubackendjava.forum.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {

    private UUID id;
    private UUID articleId;
    private UUID userId;
    private UUID parentCommentId;
    private String content;
    private Instant createdAt;
    private List<CommentResponse> replies;
}
