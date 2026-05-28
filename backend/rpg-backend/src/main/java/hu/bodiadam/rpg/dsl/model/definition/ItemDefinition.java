package hu.bodiadam.rpg.dsl.model.definition;

import java.util.List;
import java.util.Optional;

/**
 * Egy item típusdefiníció.
 */
public record ItemDefinition(
        String name,
        boolean stackable,
        Optional<String> parentName,
        String description,
        List<PropertyDefinition> properties) implements Definition {

    public ItemDefinition {
        parentName = parentName == null ? Optional.empty() : parentName;
        properties = properties == null ? List.of() : List.copyOf(properties);
    }
}
