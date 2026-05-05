package hu.bodiadam.rpg.rpg_backend.dsl.model.action;

import hu.bodiadam.rpg.rpg_backend.dsl.model.expression.Expression;
import hu.bodiadam.rpg.rpg_backend.dsl.model.expression.ReferenceExpression;

/**
 * `multiply Y by X` művelet.
 */
public record MultiplyAction(ReferenceExpression target, Expression factor) implements GameAction {
}
