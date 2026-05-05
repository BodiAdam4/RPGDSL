package hu.bodiadam.rpg.rpg_backend.dsl.model.intent;

import hu.bodiadam.rpg.rpg_backend.dsl.model.action.GameAction;
import hu.bodiadam.rpg.rpg_backend.dsl.model.condition.Condition;
import java.util.List;
import java.util.Optional;

/**
 * Egy `on <intent> chance <n>` blokk reprezentációja.
 */
public record IntentHandler(
        String intentName,
        int chance,
        Optional<Condition> requires,
        List<GameAction> successActions,
        List<GameAction> failActions) {

    public IntentHandler {
        requires = requires == null ? Optional.empty() : requires;
        successActions = successActions == null ? List.of() : List.copyOf(successActions);
        failActions = failActions == null ? List.of() : List.copyOf(failActions);
    }
}
