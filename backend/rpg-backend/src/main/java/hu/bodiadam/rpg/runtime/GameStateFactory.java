package hu.bodiadam.rpg.runtime;

import hu.bodiadam.rpg.dsl.model.GameDefinition;
import hu.bodiadam.rpg.dsl.model.expression.LiteralExpression;

import java.util.HashMap;
import java.util.Map;

public class GameStateFactory {
    public GameState create(GameDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null");
        }

        var scenes = definition.world().scenes();
        if (scenes.isEmpty()) {
            throw new IllegalStateException("Game definition must contain at least one scene");
        }
        var firstScene = scenes.get(0);

        Map<String, Object> variables = new HashMap<>();
        for (var variable : definition.variables()) {
            if (!(variable.value() instanceof LiteralExpression literal)) {
                throw new IllegalArgumentException(
                        "Variable must have a literal initial value: " + variable.name());
            }
            if (variables.containsKey(variable.name())) {
                throw new IllegalArgumentException("Duplicate variable name: " + variable.name());
            }
            variables.put(variable.name(), literal.value());
        }

        return new GameState(firstScene.name(), variables, Map.of(), firstScene.endScene());
    }
}
