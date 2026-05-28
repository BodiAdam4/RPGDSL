package hu.bodiadam.rpg.dsl.model.intent;

import hu.bodiadam.rpg.dsl.model.action.GameAction;
import hu.bodiadam.rpg.dsl.model.condition.Condition;
import java.util.List;
import java.util.Optional;

/**
 * Egy újrahasznosítható szabály reprezentációja.
 */
public record RuleDefinition(
        String name,
        List<String> parameters,
        int chance,
        Optional<Condition> requires,
        List<GameAction> successActions,
        List<GameAction> failActions) {

    public RuleDefinition {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        requires = requires == null ? Optional.empty() : requires;
        successActions = successActions == null ? List.of() : List.copyOf(successActions);
        failActions = failActions == null ? List.of() : List.copyOf(failActions);
    }
}
