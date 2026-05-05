package hu.bodiadam.rpg.rpg_backend.dsl.model.scene;

import hu.bodiadam.rpg.rpg_backend.dsl.model.action.GameAction;
import hu.bodiadam.rpg.rpg_backend.dsl.model.intent.IntentDefinition;
import hu.bodiadam.rpg.rpg_backend.dsl.model.intent.IntentHandler;
import java.util.List;

/**
 * Egy scene teljes definíciója.
 */
public record SceneDefinition(
        String name,
        String description,
        List<ActorReference> presentActors,
        List<GameAction> onEnterActions,
        List<GameAction> onExitActions,
        List<IntentDefinition> intents,
        List<IntentHandler> intentHandlers,
        boolean endScene) {

    public SceneDefinition {
        presentActors = presentActors == null ? List.of() : List.copyOf(presentActors);
        onEnterActions = onEnterActions == null ? List.of() : List.copyOf(onEnterActions);
        onExitActions = onExitActions == null ? List.of() : List.copyOf(onExitActions);
        intents = intents == null ? List.of() : List.copyOf(intents);
        intentHandlers = intentHandlers == null ? List.of() : List.copyOf(intentHandlers);
    }
}
