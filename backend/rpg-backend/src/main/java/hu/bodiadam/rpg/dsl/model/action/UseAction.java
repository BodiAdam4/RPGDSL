package hu.bodiadam.rpg.dsl.model.action;

import hu.bodiadam.rpg.dsl.model.expression.Expression;
import java.util.List;

/**
 * Másik rule meghívása.
 */
public record UseAction(String ruleName, List<Expression> arguments) implements GameAction {
    public UseAction {
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
    }
}
