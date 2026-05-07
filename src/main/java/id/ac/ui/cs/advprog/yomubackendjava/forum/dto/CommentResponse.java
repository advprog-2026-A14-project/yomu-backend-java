package id.ac.ui.cs.advprog.yomubackendjava.forum.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("article_id")
    private UUID articleId;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("parent_comment_id")
    private UUID parentCommentId;

    private String content;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("reaction_count")
    private int reactionCount;

    private List<CommentResponse> replies;

    @JsonProperty("clan_name")
    private String clanName;

    private String tier;
}