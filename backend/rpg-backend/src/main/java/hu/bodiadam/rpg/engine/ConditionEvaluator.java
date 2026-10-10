package hu.bodiadam.rpg.engine;

import hu.bodiadam.rpg.dsl.model.GameDefinition;
import hu.bodiadam.rpg.dsl.model.condition.Condition;
import hu.bodiadam.rpg.dsl.model.condition.HasCondition;
import hu.bodiadam.rpg.runtime.GameState;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.expression.ExpressionParser;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ConditionEvaluator {
    @NonNull
    private final ExpressionEvaluator expressionEvaluator;

    public boolean evaluate(
            Condition condition,
            GameDefinition definition,
            GameState state,
            Map<String, Object> ruleParameters) {

        if (condition == null) {
            throw new IllegalArgumentException("condition must not be null");
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

        if (condition instanceof HasCondition(String targetName, boolean negated)) {

            if (targetName == null || targetName.isBlank()) {
                throw new IllegalArgumentException("HasCondition targetName must not be null or blank");
            }

            int count = state.inventory().getOrDefault(targetName, 0);
            boolean present = count > 0;

            if (negated) {
                return !present;
            }
            return present;
        }

        //TODO: Implement the evaluation logic
        return false;
    }
}