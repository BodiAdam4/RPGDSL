package hu.bodiadam.rpg.rpg_backend.dsl.model.definition;

import java.util.List;
import java.util.Optional;

public sealed interface Definition permits ItemDefinition, ActorDefinition {
    String name();

    Optional<String> parentName();

    String description();

    List<PropertyDefinition> properties();
}
