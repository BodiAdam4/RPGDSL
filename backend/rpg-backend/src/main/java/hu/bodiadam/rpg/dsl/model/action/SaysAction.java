package hu.bodiadam.rpg.dsl.model.action;

/**
 * Actor megszólalás, például `guard1 says "..."`.
 */
public record SaysAction(String actorName, String text) implements GameAction {
}
