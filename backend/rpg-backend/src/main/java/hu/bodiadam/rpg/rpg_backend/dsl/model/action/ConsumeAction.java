package hu.bodiadam.rpg.rpg_backend.dsl.model.action;

import hu.bodiadam.rpg.rpg_backend.dsl.model.expression.Expression;

/**
 * `consume` művelet stackelhető elemek csökkentésére.
 */
public record ConsumeAction(Expression amount, String targetName) implements GameAction {
}
