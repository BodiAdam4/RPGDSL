package hu.bodiadam.rpg.engine;

import hu.bodiadam.rpg.dsl.model.GameDefinition;
import hu.bodiadam.rpg.dsl.model.definition.ActorDefinition;
import hu.bodiadam.rpg.dsl.model.definition.ItemDefinition;
import hu.bodiadam.rpg.dsl.model.expression.Expression;
import hu.bodiadam.rpg.dsl.model.expression.LiteralExpression;
import hu.bodiadam.rpg.dsl.model.expression.ReferenceExpression;
import hu.bodiadam.rpg.runtime.GameState;

import java.util.*;

public class ExpressionEvaluator {
    public Object evaluate(
            Expression expression,
            GameDefinition definition,
            GameState state,
            Map<String, Object> ruleParameters) {
        if (expression == null) {
            throw new IllegalArgumentException("expression must not be null");
        }
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        if (ruleParameters == null) {
            throw new IllegalArgumentException("ruleParameters must not be null");
        }

        return evaluate(expression, definition, state, ruleParameters, new HashSet<>());
    }

    private Object evaluate(
            Expression expression,
            GameDefinition definition,
            GameState state,
            Map<String, Object> ruleParameters,
            Set<PropertyKey> activeProperties) {

        if (expression instanceof LiteralExpression(Object value)) {
            return requireRuntimeValue(value, "literal");
        }

        if (expression instanceof ReferenceExpression(
                String root, java.util.Optional<String> property)) {

            if (property.isPresent()) {
                return resolveProperty(root, property.get(), definition, state, ruleParameters, activeProperties);
            }

            return resolveSimpleReference(root, state, ruleParameters);
        }

        throw new IllegalArgumentException(
                "Unsupported expression type: "
                        + expression.getClass().getSimpleName());
    }

    private Object resolveSimpleReference(
            String root,
            GameState state,
            Map<String, Object> ruleParameters) {

        if (ruleParameters.containsKey(root)) {
            return requireRuntimeValue(
                    ruleParameters.get(root), "rule parameter '" + root + "'");
        }

        if (state.variables().containsKey(root)) {
            return requireRuntimeValue(
                    state.variables().get(root), "global variable '" + root + "'");
        }

        throw new IllegalArgumentException("Unknown reference: " + root);
    }

    private Object resolveProperty(
            String root,
            String property,
            GameDefinition definition,
            GameState state,
            Map<String, Object> ruleParameters,
            Set<PropertyKey> activeProperties) {

        var key = new PropertyKey(root, property);

        if (!activeProperties.add(key)) {
            throw new IllegalStateException("Cyclic property resolution: " + root + "." + property);
        }

        try {
            return resolvePropertyValue(root, property, definition, state, ruleParameters, activeProperties);
        } finally {
            activeProperties.remove(key);
        }
    }

    private Object resolvePropertyValue(
            String root,
            String property,
            GameDefinition definition,
            GameState state,
            Map<String, Object> ruleParameters,
            Set<PropertyKey> activeProperties) {

        var matches = collectRootMatches(root, definition);
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Unknown property root: " + root);
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("Ambiguous property root: " + root);
        }

        var match = matches.get(0);
        if (match.category() == RootCategory.ITEM_INSTANCE || match.category() == RootCategory.ITEM_DEFINITION) {
            var item = resolveItemDefinition(match.typeName(), definition);
            if (property.equals("count")) {
                if (!item.stackable()) {
                    throw new IllegalArgumentException(
                            "Count requires a stackable item: " + root + ".count");
                }

                return requireRuntimeValue(
                        state.inventory().getOrDefault(root, 0),
                        root + ".count");
            }
            var propertyExpression = findItemProperty(item, root, property, definition);
            return evaluate(propertyExpression, definition, state, ruleParameters, activeProperties);
        }
        if (match.category() == RootCategory.ACTOR_INSTANCE || match.category() == RootCategory.ACTOR_DEFINITION) {
            var actor = resolveActorDefinition(match.typeName(), definition);

            if (property.equals("count")) {
                throw new IllegalArgumentException(
                        "Count is not supported for actors: " + root + ".count");
            }
            var propertyExpression = findActorProperty(actor, root, property, definition);
            return evaluate(propertyExpression, definition, state, ruleParameters,activeProperties);
        }
        throw new IllegalStateException("Unsupported root category: " + match.category());
    }

    private ItemDefinition resolveItemDefinition(
            String typeName,
            GameDefinition definition) {

        var matches = definition.items().definitions().stream()
                .filter(item -> typeName.equals(item.name()))
                .toList();

        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unknown item definition: " + typeName);
        }

        if (matches.size() > 1) {
            throw new IllegalArgumentException(
                    "Ambiguous item definition: " + typeName);
        }

        return matches.get(0);
    }

    private ActorDefinition resolveActorDefinition(
            String typeName,
            GameDefinition definition) {

        var matches = definition.actors().definitions().stream()
                .filter(actor -> typeName.equals(actor.name()))
                .toList();

        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unknown actor definition: " + typeName);
        }

        if (matches.size() > 1) {
            throw new IllegalArgumentException(
                    "Ambiguous actor definition: " + typeName);
        }

        return matches.get(0);
    }

    private Expression findItemProperty(
            ItemDefinition item,
            String root,
            String property,
            GameDefinition definition) {

        var visited = new HashSet<String>();

        while (true) {
            if (!visited.add(item.name())) {
                throw new IllegalStateException(
                        "Cyclic item inheritance: " + root + "." + property);
            }

            for (var candidate : item.properties()) {
                if (property.equals(candidate.name())) {
                    return candidate.value();
                }
            }

            if (item.parentName().isEmpty()) {
                throw new IllegalArgumentException(
                        "Unknown property: " + root + "." + property);
            }

            item = resolveItemDefinition(item.parentName().get(), definition);
        }
    }

    private Expression findActorProperty(
            ActorDefinition actor,
            String root,
            String property,
            GameDefinition definition) {

        var visited = new HashSet<String>();

        while (true) {
            if (!visited.add(actor.name())) {
                throw new IllegalStateException(
                        "Cyclic actor inheritance: " + root + "." + property);
            }

            for (var candidate : actor.properties()) {
                if (property.equals(candidate.name())) {
                    return candidate.value();
                }
            }

            if (actor.parentName().isEmpty()) {
                throw new IllegalArgumentException(
                        "Unknown property: " + root + "." + property);
            }

            actor = resolveActorDefinition(actor.parentName().get(), definition);
        }
    }

    private List<RootMatch> collectRootMatches(
            String root,
            GameDefinition definition) {

        var matches = new ArrayList<RootMatch>();

        for (var item : definition.items().definitions()) {
            if (root.equals(item.name())) {
                matches.add(new RootMatch(
                        RootCategory.ITEM_DEFINITION, item.name()));
            }
        }

        for (var actor : definition.actors().definitions()) {
            if (root.equals(actor.name())) {
                matches.add(new RootMatch(
                        RootCategory.ACTOR_DEFINITION, actor.name()));
            }
        }

        for (var instance : definition.items().instantiations()) {
            if (root.equals(instance.instanceName())) {
                matches.add(new RootMatch(
                        RootCategory.ITEM_INSTANCE, instance.typeName()));
            }
        }

        for (var instance : definition.start().itemInstantiations()) {
            if (root.equals(instance.instanceName())) {
                matches.add(new RootMatch(
                        RootCategory.ITEM_INSTANCE, instance.typeName()));
            }
        }

        for (var instance : definition.actors().instantiations()) {
            if (root.equals(instance.instanceName())) {
                matches.add(new RootMatch(
                        RootCategory.ACTOR_INSTANCE, instance.typeName()));
            }
        }

        for (var instance : definition.start().actorInstantiations()) {
            if (root.equals(instance.instanceName())) {
                matches.add(new RootMatch(
                        RootCategory.ACTOR_INSTANCE, instance.typeName()));
            }
        }

        return matches;
    }

    private Object requireRuntimeValue(Object value, String source) {
        if (!(value instanceof Integer || value instanceof Boolean || value instanceof String)) {
            throw new IllegalArgumentException(
                    "Runtime value must be an Integer, Boolean or String: " + source);
        }
        return value;
    }

    private enum RootCategory {
        ITEM_DEFINITION,
        ACTOR_DEFINITION,
        ITEM_INSTANCE,
        ACTOR_INSTANCE
    }

    private record RootMatch(RootCategory category, String typeName) {}
    private record PropertyKey(String root, String property) {}
}