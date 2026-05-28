package hu.bodiadam.rpg.dsl.model.expression;

import java.util.Optional;

/**
 * Property referencia, például `hp` vagy `HealingPotion.healAmount`.
 */
public record ReferenceExpression(String root, Optional<String> property) implements Expression {
    public ReferenceExpression {
        property = property == null ? Optional.empty() : property;
    }
}
