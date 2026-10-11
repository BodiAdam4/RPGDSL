package hu.bodiadam.rpg.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ItemRuntimeState(String typeName, Map<String, Object> properties) {

    public ItemRuntimeState {
        if (typeName == null || typeName.isBlank()) {
            throw new IllegalArgumentException("ItemRuntimeState typeName must not be null or blank");
        }

        if (properties == null) {
            throw new IllegalArgumentException("ItemRuntimeState properties must not be null");
        }

        for (var entry : properties.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("ItemRuntimeState property name must not be null or blank");
            }

            if (name.equals("count")) {
                throw new IllegalArgumentException("ItemRuntimeState property 'count' is reserved");
            }

            if (!(value instanceof Integer || value instanceof Boolean || value instanceof String)) {
                throw new IllegalArgumentException("ItemRuntimeState property '" + name + "' must contain an Integer, Boolean or String");
            }
        }
        
        properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }

}
