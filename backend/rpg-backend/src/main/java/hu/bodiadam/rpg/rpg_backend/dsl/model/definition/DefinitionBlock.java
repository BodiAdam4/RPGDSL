package hu.bodiadam.rpg.rpg_backend.dsl.model.definition;

import hu.bodiadam.rpg.rpg_backend.dsl.model.instantiation.Instantiation;
import java.util.List;

/**
 * Egy top-level definíciós blokk tartalma.
 */
public record DefinitionBlock<T, I extends Instantiation>(List<T> definitions, List<I> instantiations) {

    public DefinitionBlock {
        definitions = List.copyOf(definitions);
        instantiations = List.copyOf(instantiations);
    }

    // Egy üres blokk létrehozása. Factory method, hogy ne kelljen mindenhol List.of()-ot írni.
    public static <T, I extends Instantiation> DefinitionBlock<T, I> empty() {
        return new DefinitionBlock<>(List.of(), List.of());
    }
}
