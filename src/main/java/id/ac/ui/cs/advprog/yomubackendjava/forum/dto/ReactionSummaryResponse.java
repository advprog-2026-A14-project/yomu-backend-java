package id.ac.ui.cs.advprog.yomubackendjava.forum.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ReactionSummaryResponse {
    @JsonProperty("comment_id")
    private final UUID commentId;

    @JsonProperty("upvote_count")
    private final int upvoteCount;

    @JsonProperty("downvote_count")
    private final int downvoteCount;

    @JsonProperty("emoji_count")
    private final int emojiCount;
}
