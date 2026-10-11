package hu.bodiadam.rpg.runtime;

import hu.bodiadam.rpg.dsl.model.GameDefinition;
import hu.bodiadam.rpg.dsl.model.expression.LiteralExpression;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class GameStateFactory {
    public GameState create(GameDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null");
        }

        var scenes = definition.world().scenes();
        if (scenes.isEmpty()) {
            throw new IllegalStateException("Game definition must contain at least one scene");
        }
        var firstScene = scenes.get(0);

        Map<String, Object> variables = new LinkedHashMap<>();
        for (var variable : definition.variables()) {
            if (!(variable.value() instanceof LiteralExpression literal)) {
                throw new IllegalArgumentException(
                        "Variable must have a literal initial value: " + variable.name());
            }
            if (variables.containsKey(variable.name())) {
                throw new IllegalArgumentException("Duplicate variable name: " + variable.name());
            }
            variables.put(variable.name(), literal.value());
        }

        var itemRootTypes = new LinkedHashMap<String, String>();
        var actorRootTypes = new LinkedHashMap<String, String>();

        var registeredRoots = new java.util.LinkedHashSet<String>(variables.keySet());

        for (var item : definition.items().definitions()) {
            registerRoot(item.name(), item.name(), itemRootTypes, registeredRoots);
        }

        for (var actor : definition.actors().definitions()) {
            registerRoot(actor.name(), actor.name(), actorRootTypes, registeredRoots);
        }

        for (var instance : definition.items().instantiations()) {
            registerRoot(instance.instanceName(), instance.typeName(), itemRootTypes, registeredRoots);
        }

        for (var instance : definition.actors().instantiations()) {
            registerRoot(instance.instanceName(), instance.typeName(), actorRootTypes, registeredRoots);
        }

        for (var instance : definition.start().itemInstantiations()) {
            registerRoot(instance.instanceName(), instance.typeName(), itemRootTypes, registeredRoots);
        }

        for (var instance : definition.start().actorInstantiations()) {
            registerRoot(instance.instanceName(), instance.typeName(), actorRootTypes, registeredRoots);
        }

        return new GameState(firstScene.name(), variables, Map.of(), Map.of(), Map.of(), firstScene.endScene());
    }

    private void registerRoot(
            String rootName,
            String typeName,
            Map<String, String> rootTypes,
            Set<String> registeredRoots) {

        if (rootName == null || rootName.isBlank()) {
            throw new IllegalArgumentException("Runtime root name must not be null or blank");
        }

        if (typeName == null || typeName.isBlank()) {
            throw new IllegalArgumentException("Runtime root typeName must not be null or blank: " + rootName);
        }

        if (!registeredRoots.add(rootName)) {
            throw new IllegalArgumentException("Duplicate runtime root: " + rootName);
        }

        rootTypes.put(rootName, typeName);
    }
}
