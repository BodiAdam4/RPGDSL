package hu.bodiadam.rpg.rpg_backend.dsl.model.action;

import hu.bodiadam.rpg.rpg_backend.dsl.model.expression.Expression;
import hu.bodiadam.rpg.rpg_backend.dsl.model.expression.ReferenceExpression;

/**
 * `add X to Y` művelet.
 */
public record AddAction(Expression value, ReferenceExpression target) implements GameAction {
}
