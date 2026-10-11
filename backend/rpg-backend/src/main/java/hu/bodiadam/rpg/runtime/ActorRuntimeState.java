package hu.bodiadam.rpg.runtime;

import java.util.Map;

public record ActorRuntimeState(String typeName, Map<String, Object> properties) {
    public ActorRuntimeState {
        if (typeName == null || typeName.isBlank()) {
            throw new IllegalArgumentException("ActorRuntimeState typeName must not be null or blank");
        }

        if (properties == null) {
            throw new IllegalArgumentException("ActorRuntimeState properties must not be null");
        }

        for (var entry : properties.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("ActorRuntimeState property name must not be null or blank");
            }

            if (name.equals("count")) {
                throw new IllegalArgumentException("ActorRuntimeState property 'count' is reserved");
            }

            if (!(value instanceof Integer || value instanceof Boolean || value instanceof String)) {
                throw new IllegalArgumentException("ActorRuntimeState property '" + name + "' must contain an Integer, Boolean or String");
            }
        }
    }
}
