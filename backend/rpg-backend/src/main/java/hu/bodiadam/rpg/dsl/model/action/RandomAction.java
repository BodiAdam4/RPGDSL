package hu.bodiadam.rpg.dsl.model.action;

import java.util.List;

/**
 * Véletlen eséllyel lefutó blokk.
 */
public record RandomAction(int chance, List<GameAction> actions) implements GameAction {
    public RandomAction {
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
