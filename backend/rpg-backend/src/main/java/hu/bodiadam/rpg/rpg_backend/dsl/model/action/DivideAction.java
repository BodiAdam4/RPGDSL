package hu.bodiadam.rpg.rpg_backend.dsl.model.action;

import hu.bodiadam.rpg.rpg_backend.dsl.model.expression.Expression;
import hu.bodiadam.rpg.rpg_backend.dsl.model.expression.ReferenceExpression;

/**
 * `divide Y by X` művelet.
 */
public record DivideAction(ReferenceExpression target, Expression divisor) implements GameAction {
}
