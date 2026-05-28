package hu.bodiadam.rpg.dsl.model.action;

/**
 * Szabad narrációs szöveg kiírása.
 */
public record NarrateAction(String text) implements GameAction {
}
