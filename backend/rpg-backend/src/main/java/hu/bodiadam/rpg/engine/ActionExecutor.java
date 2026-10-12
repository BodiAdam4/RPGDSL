package hu.bodiadam.rpg.engine;

import hu.bodiadam.rpg.dsl.model.GameDefinition;
import hu.bodiadam.rpg.dsl.model.action.*;
import hu.bodiadam.rpg.dsl.model.definition.ItemDefinition;
import hu.bodiadam.rpg.dsl.model.expression.Expression;
import hu.bodiadam.rpg.dsl.model.expression.ReferenceExpression;
import hu.bodiadam.rpg.runtime.ActorRuntimeState;
import hu.bodiadam.rpg.runtime.GameState;
import hu.bodiadam.rpg.runtime.ItemRuntimeState;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ActionExecutor {

    @NonNull
    private final ExpressionEvaluator expressionEvaluator;

    public GameState execute(
            GameAction action,
            GameDefinition definition,
            GameState state,
            Map<String, Object> ruleParameters) {

        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
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

        if (action instanceof GiveAction give) {
            String targetName = give.targetName();

            var itemDefinition = resolveInventoryTarget(targetName, definition, state);

            int amount = 1;

            if (give.amount().isPresent()) {
                amount = resolvePositiveAmount(give.amount().get(), definition, state, ruleParameters, "GiveAction", targetName);
            }

            int currentCount = state.inventory().getOrDefault(targetName, 0);

            if (!itemDefinition.stackable()) {
                if (amount != 1) {
                    throw new IllegalArgumentException("GiveAction amount must be 1 for non-stackable item: " + targetName);
                }

                if (currentCount > 0) {
                    throw new IllegalArgumentException("GiveAction non-stackable item is already owned: " + targetName);
                }
            }

            int newCount;
            try {
                newCount = Math.addExact(currentCount, amount);
            } catch (ArithmeticException exception) {
                var error = new ArithmeticException("GiveAction inventory count overflow for " + targetName + ": " + currentCount + " + " + amount);
                error.initCause(exception);
                throw error;
            }

            var inventory = new LinkedHashMap<>(state.inventory());
            inventory.put(targetName, newCount);

            return new GameState(
                    state.currentSceneName(),
                    state.variables(),
                    inventory,
                    state.itemStates(),
                    state.actorStates(),
                    state.finished());
        }

        if (action instanceof ConsumeAction consume) {
            String targetName = consume.targetName();

            resolveInventoryTarget(targetName, definition, state);

            int amount = resolvePositiveAmount(
                    consume.amount(),
                    definition,
                    state,
                    ruleParameters,
                    "ConsumeAction",
                    targetName);

            int currentCount = state.inventory().getOrDefault(targetName, 0);

            if (amount > currentCount) {
                throw new IllegalArgumentException(
                        "ConsumeAction insufficient inventory for " + targetName
                                + ": requested " + amount
                                + ", available " + currentCount);
            }

            int remainingCount = currentCount - amount;

            var inventory = new LinkedHashMap<>(state.inventory());

            if (remainingCount == 0) {
                inventory.remove(targetName);
            } else {
                inventory.put(targetName, remainingCount);
            }

            return new GameState(
                    state.currentSceneName(),
                    state.variables(),
                    inventory,
                    state.itemStates(),
                    state.actorStates(),
                    state.finished());
        }

        if (action instanceof AddAction add) {
            var target = resolveWritableTarget(add.target(), state, ruleParameters);

            int operand = resolveIntegerOperand(add.value(), definition, state, ruleParameters, "AddAction");

            int newValue;
            try {
                newValue = Math.addExact(target.currentValue(), operand);
            } catch (ArithmeticException exception) {
                String targetName = target.property() == null ? target.root() : target.root() + "." + target.property();
                var error = new ArithmeticException("AddAction integer overflow for " + targetName);
                error.initCause(exception);
                throw error;
            }

            return writeTargetValue(target, newValue, state);
        }

        if (action instanceof SubtractAction subtract) {
            var target = resolveWritableTarget(subtract.target(), state, ruleParameters);

            int operand = resolveIntegerOperand(subtract.value(), definition, state, ruleParameters, "SubtractAction");

            int newValue;
            try {
                newValue = Math.subtractExact(target.currentValue(), operand);
            } catch (ArithmeticException exception) {
                String targetName = target.property() == null ? target.root() : target.root() + "." + target.property();

                var error = new ArithmeticException("SubtractAction integer overflow for " + targetName);

                error.initCause(exception);
                throw error;
            }

            return writeTargetValue(target, newValue, state);
        }

        if (action instanceof MultiplyAction multiply) {
            var target = resolveWritableTarget(multiply.target(), state, ruleParameters);

            int operand = resolveIntegerOperand(multiply.factor(), definition, state, ruleParameters, "MultiplyAction");

            int newValue;
            try {
                newValue = Math.multiplyExact(target.currentValue(), operand);
            } catch (ArithmeticException exception) {
                String targetName = target.property() == null ? target.root() : target.root() + "." + target.property();

                var error = new ArithmeticException("MultiplyAction integer overflow for " + targetName);

                error.initCause(exception);
                throw error;
            }

            return writeTargetValue(target, newValue, state);
        }

        if (action instanceof DivideAction divide) {
            var target = resolveWritableTarget(divide.target(), state, ruleParameters);

            int divisor = resolveIntegerOperand(divide.divisor(), definition, state, ruleParameters, "DivideAction");
            String targetName = target.property() == null ? target.root() : target.root() + "." + target.property();

            if (divisor == 0) {
                throw new ArithmeticException("DivideAction division by zero for " + targetName);
            }

            if (target.currentValue() == Integer.MIN_VALUE && divisor == -1) {
                throw new ArithmeticException("DivideAction integer overflow for " + targetName);
            }

            int newValue = target.currentValue() / divisor;

            return writeTargetValue(target, newValue, state);
        }

        throw new IllegalArgumentException("Unsupported action type: " + action.getClass().getSimpleName());
    }


    private int resolvePositiveAmount(
            Expression amount,
            GameDefinition definition,
            GameState state,
            Map<String, Object> ruleParameters,
            String actionName,
            String targetName) {

        if (amount == null) {
            throw new IllegalArgumentException(
                    actionName + " amount must not be null: " + targetName);
        }

        Object value = expressionEvaluator.evaluate(
                amount, definition, state, ruleParameters);

        if (!(value instanceof Integer integerAmount)) {
            throw new IllegalArgumentException(
                    actionName + " amount must be an Integer for "
                            + targetName + ": "
                            + value.getClass().getSimpleName());
        }

        if (integerAmount <= 0) {
            throw new IllegalArgumentException(
                    actionName + " amount must be positive for "
                            + targetName + ": " + integerAmount);
        }

        return integerAmount;
    }


    private ItemDefinition resolveInventoryTarget(
            String targetName,
            GameDefinition definition,
            GameState state) {

        if (targetName == null || targetName.isBlank()) {
            throw new IllegalArgumentException("Inventory target name must not be null or blank");
        }

        var runtimeItem = state.itemStates().get(targetName);

        if (runtimeItem == null) {
            if (state.actorStates().containsKey(targetName)) {
                throw new IllegalArgumentException("Inventory target cannot be an actor: " + targetName);
            }

            throw new IllegalArgumentException("Unknown runtime inventory target: " + targetName);
        }

        String typeName = runtimeItem.typeName();

        var matches = definition.items().definitions().stream()
                .filter(item -> typeName.equals(item.name()))
                .toList();

        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Unknown item type '" + typeName + "' for inventory target '" + targetName + "'");
        }

        if (matches.size() > 1) {
            throw new IllegalArgumentException("Ambiguous item type '" + typeName + "' for inventory target '" + targetName + "'");
        }

        return matches.get(0);
    }


    private enum WriteTargetKind {
        VARIABLE,
        ITEM_PROPERTY,
        ACTOR_PROPERTY
    }

    private record WritableTarget(
            WriteTargetKind kind,
            String root,
            String property,
            int currentValue) {
    }

    private WritableTarget resolveWritableTarget(
            ReferenceExpression target,
            GameState state,
            Map<String, Object> ruleParameters) {

        if (target == null) {
            throw new IllegalArgumentException("Numeric write target must not be null");
        }

        String root = target.root();

        if (root == null || root.isBlank()) {
            throw new IllegalArgumentException("Numeric write target root must not be null or blank");
        }

        if (ruleParameters.containsKey(root)) {
            throw new IllegalArgumentException("Rule parameter is read-only and shadows the write target: " + root);
        }

        if (target.property().isEmpty()) {
            if (!state.variables().containsKey(root)) {
                throw new IllegalArgumentException("Numeric write target must be an existing global variable: " + root);
            }

            int currentValue = requireIntegerTargetValue(state.variables().get(root), root);

            return new WritableTarget(
                    WriteTargetKind.VARIABLE,
                    root,
                    null,
                    currentValue);
        }

        String property = target.property().get();
        String path = root + "." + property;

        if (property.isBlank()) {
            throw new IllegalArgumentException("Numeric write target property must not be blank: " + root);
        }

        if (property.equals("count")) {
            throw new IllegalArgumentException("Virtual count cannot be a numeric write target: " + path);
        }

        if (state.variables().containsKey(root)) {
            throw new IllegalArgumentException("Scalar value cannot be used as a write-target root: " + path);
        }

        Map<String, Object> properties;
        WriteTargetKind kind;

        if (state.itemStates().containsKey(root)) {
            properties = state.itemStates().get(root).properties();
            kind = WriteTargetKind.ITEM_PROPERTY;
        } else if (state.actorStates().containsKey(root)) {
            properties = state.actorStates().get(root).properties();
            kind = WriteTargetKind.ACTOR_PROPERTY;
        } else {
            throw new IllegalArgumentException("Unknown runtime write-target root: " + path);
        }

        if (!properties.containsKey(property)) {
            throw new IllegalArgumentException("Unknown runtime write-target property: " + path);
        }

        int currentValue = requireIntegerTargetValue(properties.get(property), path);

        return new WritableTarget(
                kind,
                root,
                property,
                currentValue);
    }

    private GameState writeTargetValue(
            WritableTarget target,
            int newValue,
            GameState state) {

        switch (target.kind()) {
            case VARIABLE: {
                var variables = new LinkedHashMap<>(state.variables());
                variables.put(target.root(), newValue);

                return new GameState(
                        state.currentSceneName(),
                        variables,
                        state.inventory(),
                        state.itemStates(),
                        state.actorStates(),
                        state.finished());
            }

            case ITEM_PROPERTY: {
                var originalItem = state.itemStates().get(target.root());

                var properties = new LinkedHashMap<>(originalItem.properties());

                properties.put(target.property(), newValue);

                var itemStates = new LinkedHashMap<>(state.itemStates());

                itemStates.put(target.root(), new ItemRuntimeState(originalItem.typeName(), properties));

                return new GameState(
                        state.currentSceneName(),
                        state.variables(),
                        state.inventory(),
                        itemStates,
                        state.actorStates(),
                        state.finished());
            }

            case ACTOR_PROPERTY: {
                var originalActor = state.actorStates().get(target.root());

                var properties = new LinkedHashMap<>(originalActor.properties());

                properties.put(target.property(), newValue);

                var actorStates = new LinkedHashMap<>(state.actorStates());

                actorStates.put(target.root(), new ActorRuntimeState(originalActor.typeName(), properties));

                return new GameState(
                        state.currentSceneName(),
                        state.variables(),
                        state.inventory(),
                        state.itemStates(),
                        actorStates,
                        state.finished());
            }

            default:
                throw new IllegalStateException("Unsupported write-target kind: " + target.kind());
        }
    }

    private int requireIntegerTargetValue(Object value, String targetName) {
        if (!(value instanceof Integer integerValue)) {
            throw new IllegalArgumentException(
                    "Numeric write target must contain an Integer: " + targetName + ", actual type: " + (value == null ? "null" : value.getClass().getSimpleName()));
        }
        return integerValue;
    }

    private int resolveIntegerOperand(
            Expression expression,
            GameDefinition definition,
            GameState state,
            Map<String, Object> ruleParameters,
            String actionName) {

        if (expression == null) {
            throw new IllegalArgumentException(actionName + " operand must not be null");
        }

        Object value = expressionEvaluator.evaluate(expression, definition, state, ruleParameters);

        if (!(value instanceof Integer integerValue)) {
            throw new IllegalArgumentException(actionName + " operand must be an Integer: " + value.getClass().getSimpleName());
        }

        return integerValue;
    }
}
