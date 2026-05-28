package hu.bodiadam.rpg.dsl.model.definition;

import hu.bodiadam.rpg.dsl.model.expression.Expression;

/**
 * Egy név-érték pár egy actor vagy item definícióban.
 */
public record PropertyDefinition(String name, Expression value) {
}
