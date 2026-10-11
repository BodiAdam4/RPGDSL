package hu.bodiadam.rpg.engine;

import hu.bodiadam.rpg.dsl.model.GameDefinition;
import hu.bodiadam.rpg.dsl.model.action.GameAction;
import hu.bodiadam.rpg.dsl.model.expression.Expression;
import hu.bodiadam.rpg.runtime.GameState;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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

        //Todo: implement action execution logic here
        return new GameState(null,null,null,false);
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
