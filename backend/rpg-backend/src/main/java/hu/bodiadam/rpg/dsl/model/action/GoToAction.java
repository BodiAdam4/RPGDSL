package hu.bodiadam.rpg.dsl.model.action;

/**
 * Scene váltás.
 */
public record GoToAction(String sceneName) implements GameAction {
}
