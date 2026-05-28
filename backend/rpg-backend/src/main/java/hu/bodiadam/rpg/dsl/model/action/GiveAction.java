package hu.bodiadam.rpg.dsl.model.action;

import hu.bodiadam.rpg.dsl.model.expression.Expression;
import java.util.Optional;

/**
 * `give` művelet.
 */
public record GiveAction(Optional<Expression> amount, String targetName) implements GameAction {
    public GiveAction {
        amount = amount == null ? Optional.empty() : amount;
    }
}
