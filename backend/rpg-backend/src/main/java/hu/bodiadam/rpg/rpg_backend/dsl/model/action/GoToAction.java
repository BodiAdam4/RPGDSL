package hu.bodiadam.rpg.rpg_backend.dsl.model.action;

/**
 * Scene váltás.
 */
public record GoToAction(String sceneName) implements GameAction {
}
