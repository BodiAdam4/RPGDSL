package hu.bodiadam.rpg.runtime;

import java.util.Map;

public record GameState(String currentSceneName,
                        Map<String, Object> variables,
                        Map<String, Integer> inventory,
                        boolean finished) {

    public GameState {
        if (currentSceneName == null || currentSceneName.isBlank()) {
            throw new IllegalArgumentException("currentSceneName must not be null or blank");
        }
        if (variables == null) {
            throw new IllegalArgumentException("variables must not be null");
        }
        if (inventory == null) {
            throw new IllegalArgumentException("inventory must not be null");
        }

        for (var entry : variables.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("variable name must not be null or blank");
            }
            Object value = entry.getValue();
            if (!(value instanceof Integer || value instanceof Boolean || value instanceof String)) {
                throw new IllegalArgumentException("variable value must be an Integer, Boolean or String: " + entry.getKey());
            }
        }
        for (var entry : inventory.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("inventory name must not be null or blank");
            }
            if (entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalArgumentException("inventory count must not be null or negative: " + entry.getKey());
            }
        }

        variables = Map.copyOf(variables);
        inventory = Map.copyOf(inventory);
    }

}
