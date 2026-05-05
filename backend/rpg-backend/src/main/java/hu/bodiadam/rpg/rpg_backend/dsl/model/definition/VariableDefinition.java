package hu.bodiadam.rpg.rpg_backend.dsl.model.definition;

import hu.bodiadam.rpg.rpg_backend.dsl.model.expression.Expression;

/**
 * Egy globális változó definíciója.
 */
public record VariableDefinition(String name, Expression value) {
}
