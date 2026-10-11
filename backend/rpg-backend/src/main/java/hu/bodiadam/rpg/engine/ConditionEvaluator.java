package hu.bodiadam.rpg.engine;

import hu.bodiadam.rpg.dsl.model.GameDefinition;
import hu.bodiadam.rpg.dsl.model.condition.ComparisonCondition;
import hu.bodiadam.rpg.dsl.model.condition.Condition;
import hu.bodiadam.rpg.dsl.model.condition.HasCondition;
import hu.bodiadam.rpg.dsl.model.condition.LogicalCondition;
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
        if (condition instanceof LogicalCondition logical) {
            return evaluateLogical(logical, definition, state, ruleParameters);
        }

        if (condition instanceof ComparisonCondition comparison) {
            return evaluateComparison(comparison, definition, state, ruleParameters);
        }

        throw new IllegalArgumentException("Unsupported condition type: " + condition.getClass().getSimpleName());
    }

    private boolean evaluateLogical(
            LogicalCondition condition,
            GameDefinition definition,
            GameState state,
            Map<String, Object> ruleParameters) {

        if (condition.operator() == null) {
            throw new IllegalArgumentException("LogicalCondition operator must not be null");
        }

        boolean left = evaluate(condition.left(), definition, state, ruleParameters);

        return switch (condition.operator()) {
            case AND -> left && evaluate(
                    condition.right(), definition, state, ruleParameters);

            case OR -> left || evaluate(
                    condition.right(), definition, state, ruleParameters);
        };
    }

    private boolean evaluateComparison(
            ComparisonCondition condition,
            GameDefinition definition,
            GameState state,
            Map<String, Object> ruleParameters) {

        if (condition.reference() == null) {
            throw new IllegalArgumentException("ComparisonCondition reference must not be null");
        }
        if (condition.operator() == null) {
            throw new IllegalArgumentException("ComparisonCondition operator must not be null");
        }
        if (condition.value() == null) {
            throw new IllegalArgumentException("ComparisonCondition value must not be null");
        }

        Object left = expressionEvaluator.evaluate(condition.reference(), definition, state, ruleParameters);

        Object right = expressionEvaluator.evaluate(condition.value(), definition, state, ruleParameters);

        if (left.getClass() != right.getClass()) {
            throw new IllegalArgumentException(
                    "Incompatible operand types for " + condition.operator()
                            + ": " + left.getClass().getSimpleName()
                            + " and " + right.getClass().getSimpleName());
        }

        switch (condition.operator()) {
            case EQUAL_TO:
                return left.equals(right);
            case NOT_EQUAL_TO:
                return !left.equals(right);
            case LESS_THAN:
            case GREATER_THAN:
            case AT_LEAST:
            case AT_MOST:
                if (!(left instanceof Integer leftInteger) || !(right instanceof Integer rightInteger)) {
                    throw new IllegalArgumentException(
                            "Ordering comparison requires Integer operands for "
                                    + condition.operator()
                                    + ": " + left.getClass().getSimpleName()
                                    + " and " + right.getClass().getSimpleName());
                }

                switch (condition.operator()) {
                    case LESS_THAN:
                        return leftInteger < rightInteger;
                    case GREATER_THAN:
                        return leftInteger > rightInteger;
                    case AT_LEAST:
                        return leftInteger >= rightInteger;
                    case AT_MOST:
                        return leftInteger <= rightInteger;
                    default:
                        throw new IllegalStateException("Unexpected ordering operator: " + condition.operator());
                }
            default:
                throw new IllegalArgumentException(
                        "Unsupported comparison operator: " + condition.operator());
        }
    }

}