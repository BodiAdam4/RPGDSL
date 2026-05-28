package hu.bodiadam.rpg.dsl.model.action;

import hu.bodiadam.rpg.dsl.model.expression.Expression;

/**
 * `consume` művelet stackelhető elemek csökkentésére.
 */
public record ConsumeAction(Expression amount, String targetName) implements GameAction {
}
