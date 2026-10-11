package hu.bodiadam.rpg.engine;

import hu.bodiadam.rpg.dsl.model.GameDefinition;
import hu.bodiadam.rpg.dsl.model.action.ConsumeAction;
import hu.bodiadam.rpg.dsl.model.action.GameAction;
import hu.bodiadam.rpg.dsl.model.action.GiveAction;
import hu.bodiadam.rpg.dsl.model.expression.Expression;
import hu.bodiadam.rpg.runtime.GameState;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ActionExecutor {

    @NonNull
    private final ExpressionEvaluator expressionEvaluator;

    public GameState execute(
            GameAction action,
            GameDefinition definition,
            GameState state,
            Map<String, Object> ruleParameters) {

        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        if (ruleParameters == null) {
            throw new IllegalArgumentException("ruleParameters must not be null");
        }

        if (action instanceof GiveAction give) {
            String targetName = give.targetName();

            if (targetName == null || targetName.isBlank()) {
                throw new IllegalArgumentException("GiveAction targetName must not be null or blank");
            }

            int amount = 1;

            if (give.amount().isPresent()) {
                amount = resolvePositiveAmount(give.amount().get(), definition, state, ruleParameters, "GiveAction", targetName);
            }

            int currentCount = state.inventory().getOrDefault(targetName, 0);

            int newCount;
            try {
                newCount = Math.addExact(currentCount, amount);
            } catch (ArithmeticException exception) {
                var error = new ArithmeticException("GiveAction inventory count overflow for " + targetName + ": " + currentCount + " + " + amount);
                error.initCause(exception);
                throw error;
            }

            var inventory = new HashMap<>(state.inventory());
            inventory.put(targetName, newCount);

            return new GameState(
                    state.currentSceneName(),
                    state.variables(),
                    inventory,
                    state.finished());
        }

        if (action instanceof ConsumeAction consume) {
            String targetName = consume.targetName();

            if (targetName == null || targetName.isBlank()) {
                throw new IllegalArgumentException("ConsumeAction targetName must not be null or blank");
            }

            int amount = resolvePositiveAmount(
                    consume.amount(),
                    definition,
                    state,
                    ruleParameters,
                    "ConsumeAction",
                    targetName);

            int currentCount = state.inventory().getOrDefault(targetName, 0);

            if (amount > currentCount) {
                throw new IllegalArgumentException(
                        "ConsumeAction insufficient inventory for " + targetName
                                + ": requested " + amount
                                + ", available " + currentCount);
            }

            int remainingCount = currentCount - amount;

            var inventory = new HashMap<>(state.inventory());

            if (remainingCount == 0) {
                inventory.remove(targetName);
            } else {
                inventory.put(targetName, remainingCount);
            }

            return new GameState(state.currentSceneName(), state.variables(), inventory, state.finished());
        }
        throw new IllegalArgumentException("Unsupported action type: " + action.getClass().getSimpleName());
    }


    private int resolvePositiveAmount(
            Expression amount,
            GameDefinition definition,
            GameState state,
            Map<String, Object> ruleParameters,
            String actionName,
            String targetName) {

        if (amount == null) {
            throw new IllegalArgumentException(
                    actionName + " amount must not be null: " + targetName);
        }

        Object value = expressionEvaluator.evaluate(
                amount, definition, state, ruleParameters);

        if (!(value instanceof Integer integerAmount)) {
            throw new IllegalArgumentException(
                    actionName + " amount must be an Integer for "
                            + targetName + ": "
                            + value.getClass().getSimpleName());
        }

        if (integerAmount <= 0) {
            throw new IllegalArgumentException(
                    actionName + " amount must be positive for "
                            + targetName + ": " + integerAmount);
        }

        return integerAmount;
    }
}
