package hu.bodiadam.rpg.dsl.model.condition;

/**
 * Két feltétel logikai összekapcsolása.
 */
public record LogicalCondition(
        Condition left,
        LogicalOperator operator,
        Condition right) implements Condition {
}
