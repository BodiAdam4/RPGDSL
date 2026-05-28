package hu.bodiadam.rpg.dsl.model.expression;

/**
 * Literál érték egy expression-ben.
 *
 * <p>A value lehet String, Integer vagy Boolean attól függően, mit parse-oltunk.
 */
public record LiteralExpression(Object value) implements Expression {
}
