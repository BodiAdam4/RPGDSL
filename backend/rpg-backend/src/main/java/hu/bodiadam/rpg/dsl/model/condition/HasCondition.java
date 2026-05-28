package hu.bodiadam.rpg.dsl.model.condition;

/**
 * Megléti feltétel, például `has HealingPotion` vagy `does not have RustyKey`.
 */
public record HasCondition(String targetName, boolean negated) implements Condition {
}
