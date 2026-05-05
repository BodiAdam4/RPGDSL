package hu.bodiadam.rpg.rpg_backend.dsl.model.definition;

import hu.bodiadam.rpg.rpg_backend.dsl.model.expression.Expression;

/**
 * Egy név-érték pár egy actor vagy item definícióban.
 */
public record PropertyDefinition(String name, Expression value) {
}
