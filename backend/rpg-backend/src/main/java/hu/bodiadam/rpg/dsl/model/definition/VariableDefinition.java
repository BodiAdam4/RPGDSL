package hu.bodiadam.rpg.dsl.model.definition;

import hu.bodiadam.rpg.dsl.model.expression.Expression;

/**
 * Egy globális változó definíciója.
 */
public record VariableDefinition(String name, Expression value) {
}
