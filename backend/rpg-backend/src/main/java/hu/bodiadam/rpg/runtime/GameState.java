package hu.bodiadam.rpg.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record GameState(String currentSceneName,
        Map<String, Object> variables,
        Map<String, Integer> inventory,
        Map<String, ItemRuntimeState> itemStates,
        Map<String, ActorRuntimeState> actorStates,
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
        if (itemStates == null) {
            throw new IllegalArgumentException("itemStates must not be null");
        }
        if (actorStates == null) {
            throw new IllegalArgumentException("actorStates must not be null");
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
            String root = entry.getKey();

            if (root == null || root.isBlank()) {
                throw new IllegalArgumentException("inventory name must not be null or blank");
            }

            if (entry.getValue() == null || entry.getValue() <= 0) {
                throw new IllegalArgumentException("inventory count must be positive: " + root);
            }

            if (!itemStates.containsKey(root)) {
                throw new IllegalArgumentException("inventory root has no item runtime state: " + root);
            }
        }

        for (var entry : itemStates.entrySet()) {
            String root = entry.getKey();

            if (root == null || root.isBlank()) {
                throw new IllegalArgumentException("item runtime root must not be null or blank");
            }

            if (entry.getValue() == null) {
                throw new IllegalArgumentException("item runtime state must not be null: " + root);
            }

            if (actorStates.containsKey(root)) {
                throw new IllegalArgumentException("root cannot belong to both item and actor states: " + root);
            }
        }

        for (var entry : actorStates.entrySet()) {
            String root = entry.getKey();

            if (root == null || root.isBlank()) {
                throw new IllegalArgumentException("actor runtime root must not be null or blank");
            }

            if (entry.getValue() == null) {
                throw new IllegalArgumentException("actor runtime state must not be null: " + root);
            }
        }

        variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));

        inventory = Collections.unmodifiableMap(new LinkedHashMap<>(inventory));

        itemStates = Collections.unmodifiableMap(new LinkedHashMap<>(itemStates));

        actorStates = Collections.unmodifiableMap(new LinkedHashMap<>(actorStates));
    }

}
