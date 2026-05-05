package id.ac.ui.cs.advprog.yomubackendjava.forum.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import id.ac.ui.cs.advprog.yomubackendjava.forum.model.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionResponse {

    @JsonProperty("comment_id")
    private UUID commentId;

    @JsonProperty("reaction_type")
    private ReactionType reactionType;

    private boolean reacted;

    @JsonProperty("reaction_count")
    private int reactionCount;
}
