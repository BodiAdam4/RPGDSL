package hu.bodiadam.rpg.dsl.model.action;

import hu.bodiadam.rpg.dsl.model.expression.Expression;
import hu.bodiadam.rpg.dsl.model.expression.ReferenceExpression;

/**
 * `multiply Y by X` művelet.
 */
public record MultiplyAction(ReferenceExpression target, Expression factor) implements GameAction {
}
