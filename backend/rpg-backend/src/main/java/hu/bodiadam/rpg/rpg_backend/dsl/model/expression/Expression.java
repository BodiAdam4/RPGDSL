package hu.bodiadam.rpg.rpg_backend.dsl.model.expression;

/**
 * Általános kifejezés a DSL-ben.
 */
public sealed interface Expression permits LiteralExpression, ReferenceExpression {
}
