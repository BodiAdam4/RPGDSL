package hu.bodiadam.rpg.rpg_backend.dsl.model.definition;

import java.util.List;
import java.util.Optional;

/**
 * Egy actor típusdefiníció.
 */
public record ActorDefinition(
        String name,
        Optional<String> parentName,
        String description,
        List<PropertyDefinition> properties) implements Definition {

    public ActorDefinition {
        parentName = parentName == null ? Optional.empty() : parentName;
        properties = properties == null ? List.of() : List.copyOf(properties);
    }
}
