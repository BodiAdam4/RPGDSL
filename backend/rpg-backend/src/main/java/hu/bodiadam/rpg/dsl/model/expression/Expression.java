package hu.bodiadam.rpg.dsl.model.expression;

/**
 * Általános kifejezés a DSL-ben.
 */
public sealed interface Expression permits LiteralExpression, ReferenceExpression {
}
