package hu.bodiadam.rpg.rpg_backend.dsl.model.action;

/**
 * Szabad narrációs szöveg kiírása.
 */
public record NarrateAction(String text) implements GameAction {
}
