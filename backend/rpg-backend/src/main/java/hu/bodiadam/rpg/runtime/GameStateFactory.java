package hu.bodiadam.rpg.runtime;

import hu.bodiadam.rpg.dsl.model.GameDefinition;
import hu.bodiadam.rpg.dsl.model.definition.ActorDefinition;
import hu.bodiadam.rpg.dsl.model.definition.ItemDefinition;
import hu.bodiadam.rpg.dsl.model.expression.Expression;
import hu.bodiadam.rpg.dsl.model.expression.LiteralExpression;
import hu.bodiadam.rpg.dsl.model.expression.ReferenceExpression;

import java.util.*;

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

        var itemPropertyExpressions = new LinkedHashMap<String, Map<String, Expression>>();

        for (var entry : itemRootTypes.entrySet()) {
            itemPropertyExpressions.put(entry.getKey(), flattenItemProperties(entry.getValue(), definition));
        }

        var actorPropertyExpressions = new LinkedHashMap<String, Map<String, Expression>>();

        for (var entry : actorRootTypes.entrySet()) {
            actorPropertyExpressions.put(entry.getKey(), flattenActorProperties(entry.getValue(), definition));
        }

        var itemPropertyValues = new LinkedHashMap<String, Map<String, Object>>();

        for (String root : itemRootTypes.keySet()) {
            itemPropertyValues.put(root, new LinkedHashMap<>());
        }

        var actorPropertyValues = new LinkedHashMap<String, Map<String, Object>>();

        for (String root : actorRootTypes.keySet()) {
            actorPropertyValues.put(root, new LinkedHashMap<>());
        }

        var activeProperties = new LinkedHashSet<PropertyKey>();

        var propertyExpressions = new LinkedHashMap<String, Map<String, Expression>>();

        propertyExpressions.putAll(itemPropertyExpressions);
        propertyExpressions.putAll(actorPropertyExpressions);

        var propertyValues = new LinkedHashMap<String, Map<String, Object>>();

        propertyValues.putAll(itemPropertyValues);
        propertyValues.putAll(actorPropertyValues);

        for (var rootEntry : propertyExpressions.entrySet()) {
            String root = rootEntry.getKey();

            for (String property : rootEntry.getValue().keySet()) {
                resolveInitialProperty(root, property, propertyExpressions, propertyValues, variables, activeProperties);
            }
        }

        var itemStates = new LinkedHashMap<String, ItemRuntimeState>();

        for (var rootEntry : itemRootTypes.entrySet()) {
            String root = rootEntry.getKey();
            String typeName = rootEntry.getValue();

            var properties = new LinkedHashMap<String, Object>();

            for (String property : itemPropertyExpressions.get(root).keySet()) {
                properties.put(property, itemPropertyValues.get(root).get(property));
            }

            itemStates.put(root, new ItemRuntimeState(typeName, properties));
        }

        var actorStates = new LinkedHashMap<String, ActorRuntimeState>();

        for (var rootEntry : actorRootTypes.entrySet()) {
            String root = rootEntry.getKey();
            String typeName = rootEntry.getValue();

            var properties = new LinkedHashMap<String, Object>();

            for (String property : actorPropertyExpressions.get(root).keySet()) {
                properties.put(property, actorPropertyValues.get(root).get(property));
            }

            actorStates.put(root, new ActorRuntimeState(typeName, properties));
        }
        return new GameState(firstScene.name(), variables, Map.of(), itemStates, actorStates, firstScene.endScene());
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

    private ItemDefinition resolveItemDefinition(String typeName, GameDefinition definition) {

        var matches = definition.items().definitions().stream()
                .filter(item -> typeName.equals(item.name()))
                .toList();

        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Unknown item definition during bootstrap: " + typeName);
        }

        if (matches.size() > 1) {
            throw new IllegalArgumentException("Ambiguous item definition during bootstrap: " + typeName);
        }

        return matches.get(0);
    }

    private List<ItemDefinition> resolveItemInheritanceChain(String typeName, GameDefinition definition) {

        var chain = new ArrayList<ItemDefinition>();
        var visited = new LinkedHashSet<String>();
        String currentType = typeName;

        while (true) {
            if (!visited.add(currentType)) {
                throw new IllegalArgumentException("Item inheritance cycle during bootstrap: " + currentType);
            }

            var item = resolveItemDefinition(currentType, definition);
            chain.add(item);

            if (item.parentName().isEmpty()) {
                break;
            }

            currentType = item.parentName().get();
        }

        return List.copyOf(chain);
    }

    private ActorDefinition resolveActorDefinition(String typeName, GameDefinition definition) {

        var matches = definition.actors().definitions().stream()
                .filter(actor -> typeName.equals(actor.name()))
                .toList();

        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Unknown actor definition during bootstrap: " + typeName);
        }

        if (matches.size() > 1) {
            throw new IllegalArgumentException("Ambiguous actor definition during bootstrap: " + typeName);
        }

        return matches.get(0);
    }

    private List<ActorDefinition> resolveActorInheritanceChain(String typeName, GameDefinition definition) {

        var chain = new ArrayList<ActorDefinition>();
        var visited = new LinkedHashSet<String>();
        String currentType = typeName;

        while (true) {
            if (!visited.add(currentType)) {
                throw new IllegalArgumentException("Actor inheritance cycle during bootstrap: " + currentType);
            }

            var actor = resolveActorDefinition(currentType, definition);
            chain.add(actor);

            if (actor.parentName().isEmpty()) {
                break;
            }

            currentType = actor.parentName().get();
        }

        return List.copyOf(chain);
    }
    private Map<String, Expression> flattenItemProperties(String typeName, GameDefinition definition) {

        var chain = resolveItemInheritanceChain(typeName, definition);
        var properties = new LinkedHashMap<String, Expression>();

        for (int index = chain.size() - 1; index >= 0; index--) {
            var item = chain.get(index);

            for (var property : item.properties()) {
                if ("count".equals(property.name())) {
                    throw new IllegalArgumentException("Reserved property 'count' on item: " + item.name());
                }

                properties.put(property.name(), property.value());
            }
        }

        return properties;
    }
    private Map<String, Expression> flattenActorProperties(String typeName, GameDefinition definition) {

        var chain = resolveActorInheritanceChain(typeName, definition);
        var properties = new LinkedHashMap<String, Expression>();

        for (int index = chain.size() - 1; index >= 0; index--) {
            var actor = chain.get(index);

            for (var property : actor.properties()) {
                if ("count".equals(property.name())) {
                    throw new IllegalArgumentException("Reserved property 'count' on actor: " + actor.name());
                }

                properties.put(property.name(), property.value());
            }
        }

        return properties;
    }

    private Object requireRuntimeValue(Object value, String source) {
        if (!(value instanceof Integer || value instanceof Boolean || value instanceof String)) {
            throw new IllegalArgumentException("Bootstrap value must be an Integer, Boolean or String: " + source);
        }

        return value;
    }

    private Object evaluateInitialExpression(
            Expression expression,
            Map<String, Object> variables,
            String source,
            Map<String, Map<String, Expression>> propertyExpressions,
            Map<String, Map<String, Object>> propertyValues,
            Set<PropertyKey> activeProperties) {

        if (expression == null) {
            throw new IllegalArgumentException("Bootstrap expression must not be null: " + source);
        }

        if (expression instanceof LiteralExpression literal) {
            return requireRuntimeValue(literal.value(), source);
        }

        if (expression instanceof ReferenceExpression reference) {
            if (reference.property().isPresent()) {
                String root = reference.root();
                String property = reference.property().get();

                if (variables.containsKey(root)) {
                    throw new IllegalArgumentException(
                            "Scalar variable cannot be an initial property root: "
                                    + root + "." + property);
                }

                return resolveInitialProperty(
                        root,
                        property,
                        propertyExpressions,
                        propertyValues,
                        variables,
                        activeProperties);
            }

            String root = reference.root();

            if (!variables.containsKey(root)) {
                throw new IllegalArgumentException("Unknown bootstrap variable '" + root + "' while resolving " + source);
            }

            return requireRuntimeValue(variables.get(root), source);
        }

        throw new IllegalArgumentException("Unsupported bootstrap expression type: " + expression.getClass().getSimpleName() + " while resolving " + source);
    }

    private Object resolveInitialProperty(
            String root,
            String property,
            Map<String, Map<String, Expression>> propertyExpressions,
            Map<String, Map<String, Object>> propertyValues,
            Map<String, Object> variables,
            Set<PropertyKey> activeProperties) {

        var expressions = propertyExpressions.get(root);

        if (expressions == null) {
            throw new IllegalArgumentException("Unknown initial property root: " + root + "." + property);
        }

        if (!expressions.containsKey(property)) {
            throw new IllegalArgumentException("Unknown initial property: " + root + "." + property);
        }

        var values = propertyValues.get(root);

        if (values.containsKey(property)) {
            return values.get(property);
        }

        var key = new PropertyKey(root, property);

        if (!activeProperties.add(key)) {
            throw new IllegalStateException("Cyclic initial property resolution: " + root + "." + property);
        }

        try {
            Object value = evaluateInitialExpression(
                    expressions.get(property),
                    variables,
                    root + "." + property,
                    propertyExpressions,
                    propertyValues,
                    activeProperties);values.put(property, value);
            return value;
        } finally {
            activeProperties.remove(key);
        }
    }


    private record PropertyKey(String root, String property) {
    }
}
