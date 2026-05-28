package hu.bodiadam.rpg.dsl.model.condition;

/**
 * Logikai feltétel.
 *
 * <p>Ezt használja a `requires` blokk, illetve később a runtime validáció/feltétel-kiértékelés.
 */
public sealed interface Condition permits HasCondition, ComparisonCondition, LogicalCondition {
}
