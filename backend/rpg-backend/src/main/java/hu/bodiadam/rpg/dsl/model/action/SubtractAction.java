package hu.bodiadam.rpg.dsl.model.action;

import hu.bodiadam.rpg.dsl.model.expression.Expression;
import hu.bodiadam.rpg.dsl.model.expression.ReferenceExpression;

/**
 * `subtract X from Y` művelet.
 */
public record SubtractAction(Expression value, ReferenceExpression target) implements GameAction {
}
