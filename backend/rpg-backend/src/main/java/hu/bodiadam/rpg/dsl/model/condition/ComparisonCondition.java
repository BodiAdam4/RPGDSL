package hu.bodiadam.rpg.dsl.model.condition;

import hu.bodiadam.rpg.dsl.model.expression.Expression;
import hu.bodiadam.rpg.dsl.model.expression.ReferenceExpression;

/**
 * Összehasonlító feltétel, például `hp is less than 6`.
 */
public record ComparisonCondition(
        ReferenceExpression reference,
        ComparisonOperator operator,
        Expression value) implements Condition {
}
