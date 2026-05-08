package id.ac.ui.cs.advprog.yomubackendjava.forum.strategy;

import id.ac.ui.cs.advprog.yomubackendjava.forum.model.ReactionType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReactionStrategyTest {

    @Test
    void upvoteStrategy_getReactionType_shouldReturnUpvote() {
        UpvoteReactionStrategy strategy = new UpvoteReactionStrategy();
        assertEquals(ReactionType.UPVOTE, strategy.getReactionType());
    }

    @Test
    void upvoteStrategy_onReactionAdded_shouldNotThrow() {
        UpvoteReactionStrategy strategy = new UpvoteReactionStrategy();
        assertDoesNotThrow(() -> strategy.onReactionAdded(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void upvoteStrategy_onReactionRemoved_shouldNotThrow() {
        UpvoteReactionStrategy strategy = new UpvoteReactionStrategy();
        assertDoesNotThrow(() -> strategy.onReactionRemoved(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void strategyFactory_resolve_validType_shouldReturnStrategy() {
        UpvoteReactionStrategy upvoteStrategy = new UpvoteReactionStrategy();
        ReactionStrategyFactory factory = new ReactionStrategyFactory(List.of(upvoteStrategy));
        assertNotNull(factory.resolve(ReactionType.UPVOTE));
    }

    @Test
    void strategyFactory_resolve_invalidType_shouldThrowIllegalArgument() {
        ReactionStrategyFactory factory = new ReactionStrategyFactory(List.of());
        assertThrows(IllegalArgumentException.class, () -> factory.resolve(ReactionType.UPVOTE));
    }
}