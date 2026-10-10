package hu.bodiadam.rpg.engine;

import hu.bodiadam.rpg.dsl.model.GameDefinition;
import hu.bodiadam.rpg.dsl.model.expression.Expression;
import hu.bodiadam.rpg.dsl.model.expression.LiteralExpression;
import hu.bodiadam.rpg.dsl.model.expression.ReferenceExpression;
import hu.bodiadam.rpg.runtime.GameState;

import java.util.Map;

public class ExpressionEvaluator {
    public Object evaluate(
            Expression expression,
            GameDefinition definition,
            GameState state,
            Map<String, Object> ruleParameters) {
        if (expression == null) {
            throw new IllegalArgumentException("expression must not be null");
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

        if (expression instanceof LiteralExpression(Object value)) {
            return requireRuntimeValue(value, "literal");
        }
        if (expression instanceof ReferenceExpression(String root, java.util.Optional<String> property)) {
            if (property.isPresent()) {
                //TODO: implement property access
            }
            if (ruleParameters.containsKey(root)) {
                return requireRuntimeValue(ruleParameters.get(root), "rule parameter '" + root + "'");
            }
            if (state.variables().containsKey(root)) {
                return requireRuntimeValue(state.variables().get(root), "global variable '" + root + "'");
            }
            throw new IllegalArgumentException("Unknown reference: " + root);
        }
        throw new IllegalArgumentException("Unsupported expression type: " + expression.getClass().getSimpleName());
    }

    private Object requireRuntimeValue(Object value, String source) {
        if (!(value instanceof Integer || value instanceof Boolean || value instanceof String)) {
            throw new IllegalArgumentException(
                    "Runtime value must be an Integer, Boolean or String: " + source);
        }
        return value;
    }
}