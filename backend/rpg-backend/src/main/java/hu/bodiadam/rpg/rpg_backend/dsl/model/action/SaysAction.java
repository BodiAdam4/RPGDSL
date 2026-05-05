package hu.bodiadam.rpg.rpg_backend.dsl.model.action;

/**
 * Actor megszólalás, például `guard1 says "..."`.
 */
public record SaysAction(String actorName, String text) implements GameAction {
}
