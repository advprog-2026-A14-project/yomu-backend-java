package id.ac.ui.cs.advprog.yomubackendjava.forum.strategy;

import id.ac.ui.cs.advprog.yomubackendjava.forum.model.ReactionType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReactionStrategyFactory {

    private final Map<ReactionType, ReactionStrategy> strategyMap;

    public ReactionStrategyFactory(List<ReactionStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(ReactionStrategy::getReactionType, Function.identity()));
    }

    public ReactionStrategy resolve(ReactionType reactionType) {
        ReactionStrategy strategy = strategyMap.get(reactionType);
        if (strategy == null) {
            throw new IllegalArgumentException("Reaction type tidak didukung: " + reactionType);
        }
        return strategy;
    }
}
