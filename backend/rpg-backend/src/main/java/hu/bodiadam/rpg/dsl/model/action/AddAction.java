package hu.bodiadam.rpg.dsl.model.action;

import hu.bodiadam.rpg.dsl.model.expression.Expression;
import hu.bodiadam.rpg.dsl.model.expression.ReferenceExpression;

/**
 * `add X to Y` művelet.
 */
public record AddAction(Expression value, ReferenceExpression target) implements GameAction {
}
