package hu.bodiadam.rpg.rpg_backend.dsl.model.scene;

import hu.bodiadam.rpg.rpg_backend.dsl.model.action.GameAction;
import hu.bodiadam.rpg.rpg_backend.dsl.model.instantiation.ActorInstantiation;
import hu.bodiadam.rpg.rpg_backend.dsl.model.instantiation.ItemInstantiation;
import java.util.List;

/**
 * A `start` blokk reprezentációja.
 */
public record StartBlock(
        List<ItemInstantiation> itemInstantiations,
        List<ActorInstantiation> actorInstantiations,
        List<GameAction> actions) {
    public StartBlock {
        itemInstantiations = itemInstantiations == null ? List.of() : List.copyOf(itemInstantiations);
        actorInstantiations = actorInstantiations == null ? List.of() : List.copyOf(actorInstantiations);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
