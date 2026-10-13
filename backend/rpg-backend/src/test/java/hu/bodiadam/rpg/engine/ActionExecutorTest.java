package hu.bodiadam.rpg.engine;

import static org.junit.jupiter.api.Assertions.*;

import hu.bodiadam.rpg.dsl.model.GameDefinition;
import hu.bodiadam.rpg.dsl.model.action.*;
import hu.bodiadam.rpg.dsl.model.expression.Expression;
import hu.bodiadam.rpg.dsl.model.expression.LiteralExpression;
import hu.bodiadam.rpg.dsl.model.expression.ReferenceExpression;
import hu.bodiadam.rpg.dsl.parser.DslParserFacade;
import hu.bodiadam.rpg.runtime.ActorRuntimeState;
import hu.bodiadam.rpg.runtime.GameState;

import java.util.*;

import hu.bodiadam.rpg.runtime.ItemRuntimeState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActionExecutorTest {

    private final ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

    private final ActionExecutor executor = new ActionExecutor(expressionEvaluator);

    private final Map<String, Object> ruleParameters = Map.of(
            "amount", 3,
            "zero", 0,
            "negative", -2,
            "enabled", true,
            "text", "3",
            "booleanAmount", true,
            "stringAmount", "3");

    private GameDefinition definition;
    private GameState state;

    @BeforeAll
    void setUp() {
        definition = new DslParserFacade().parse("""
                vars {
                    hp: 12
                    gold: 0
                    doorOpen: false
                    status: "closed"
                }
                items {
                    stackable item GoldCoin {
                        description: "Coin"
                        value: 1
                    }
                    purse as GoldCoin
                    item Knife {
                        description: "Knife"
                        damage: 3
                    }
                    blade as Knife
                }
                actors {
                    actor Guard {
                        description: "Guard"
                        hp: 10
                    }
                    captain as Guard
                }
                world {
                    scenes {
                        scene entrance {
                            description: "Entrance"
                        }
                    }
                }
                """);

        var itemStates = new LinkedHashMap<String, ItemRuntimeState>();

        itemStates.put(
                "GoldCoin",
                new ItemRuntimeState("GoldCoin", Map.of("value", 1)));

        itemStates.put(
                "Knife",
                new ItemRuntimeState("Knife", Map.of("damage", 3)));

        state = new GameState(
                "entrance",
                Map.of(
                        "hp", 7,
                        "gold", 10,
                        "doorOpen", true,
                        "status", "open"),
                Map.of(
                        "GoldCoin", 6,
                        "Knife", 1),
                itemStates,
                Map.of(),
                false);
    }

    @Test
    void giveWithLiteralAmountAddsToMissingInventoryEntry() {
        var initialState = new GameState(
                state.currentSceneName(),
                state.variables(),
                Map.of("Knife", 1),
                state.itemStates(),
                state.actorStates(),
                state.finished());

        var action = new GiveAction(
                Optional.of(new LiteralExpression(4)),
                "GoldCoin");

        var result = executor.execute(
                action, definition, initialState, ruleParameters);

        assertInventoryTransition(
                initialState,
                result,
                Map.of("GoldCoin", 4, "Knife", 1));

        assertEquals(Map.of("Knife", 1), initialState.inventory());
    }

    @Test
    void giveWithResolvedAmountAddsToExistingInventory() {
        var action = new GiveAction(
                Optional.of(reference("amount")),
                "GoldCoin");

        var parameters = new HashMap<String, Object>(ruleParameters);
        var originalParameters = new HashMap<>(parameters);

        var result = executor.execute(
                action, definition, state, parameters);

        assertInventoryTransition(
                state,
                result,
                Map.of("GoldCoin", 9, "Knife", 1));

        assertEquals(originalParameters, parameters);
        assertOriginalStateUnchanged();
    }

    @Test
    void rejectsNonIntegerZeroAndNegativeGiveAmounts() {
        for (String parameter : new String[] {
                "zero", "negative", "enabled", "text"
        }) {
            var action = new GiveAction(
                    Optional.of(reference(parameter)),
                    "GoldCoin");

            assertThrows(
                    IllegalArgumentException.class,
                    () -> execute(action),
                    "GiveAction must reject parameter: " + parameter);

            assertOriginalStateUnchanged();
        }

        var unsupportedAmount = new GiveAction(
                Optional.of(new LiteralExpression(3L)),
                "GoldCoin");

        assertThrows(
                IllegalArgumentException.class,
                () -> execute(unsupportedAmount));

        assertOriginalStateUnchanged();
    }

    @Test
    void rejectsInventoryOverflowWithoutChangingState() {
        var fullState = new GameState(
                state.currentSceneName(),
                state.variables(),
                Map.of(
                        "GoldCoin", Integer.MAX_VALUE,
                        "Knife", 1),
                state.itemStates(),
                state.actorStates(),
                state.finished());

        var action = new GiveAction(Optional.empty(), "GoldCoin");

        var error = assertThrows(
                ArithmeticException.class,
                () -> executor.execute(
                        action, definition, fullState, ruleParameters));

        assertAll(
                () -> assertTrue(
                        error.getMessage().contains("GiveAction")),
                () -> assertTrue(
                        error.getMessage().contains("GoldCoin")),
                () -> assertEquals(
                        Map.of(
                                "GoldCoin", Integer.MAX_VALUE,
                                "Knife", 1),
                        fullState.inventory()),
                () -> assertEquals(
                        state.variables(), fullState.variables()),
                () -> assertEquals(
                        state.currentSceneName(),
                        fullState.currentSceneName()),
                () -> assertEquals(
                        state.finished(), fullState.finished()));
    }

    @Test
    void consumeReducesExistingInventory() {
        var action = new ConsumeAction(
                reference("amount"),
                "GoldCoin");

        var result = execute(action);

        assertInventoryTransition(
                state,
                result,
                Map.of("GoldCoin", 3, "Knife", 1));

        assertOriginalStateUnchanged();
    }

    @Test
    void consumeRemovesExactRemainingInventoryEntry() {
        var action = new ConsumeAction(
                new LiteralExpression(6),
                "GoldCoin");

        var result = execute(action);

        assertInventoryTransition(
                state,
                result,
                Map.of("Knife", 1));

        assertFalse(result.inventory().containsKey("GoldCoin"));
        assertOriginalStateUnchanged();
    }

    @Test
    void consumeRejectsMissingOrInsufficientInventoryWithoutChangingState() {
        var missingState = new GameState(
                state.currentSceneName(),
                state.variables(),
                Map.of("Knife", 1),
                state.itemStates(),
                state.actorStates(),
                state.finished());

        var missingAction = new ConsumeAction(
                new LiteralExpression(2),
                "GoldCoin");

        var missingError = assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        missingAction,
                        definition,
                        missingState,
                        ruleParameters));

        assertAll(
                () -> assertTrue(
                        missingError.getMessage().contains("GoldCoin")),
                () -> assertTrue(
                        missingError.getMessage().contains("2")),
                () -> assertTrue(
                        missingError.getMessage().contains("0")),
                () -> assertEquals(
                        Map.of("Knife", 1),
                        missingState.inventory()),
                () -> assertEquals(
                        state.variables(), missingState.variables()),
                () -> assertEquals(
                        state.currentSceneName(),
                        missingState.currentSceneName()),
                () -> assertEquals(
                        state.finished(), missingState.finished()));

        var insufficientAction = new ConsumeAction(
                new LiteralExpression(8),
                "GoldCoin");

        var insufficientError = assertThrows(
                IllegalArgumentException.class,
                () -> execute(insufficientAction));

        assertAll(
                () -> assertTrue(
                        insufficientError.getMessage().contains("GoldCoin")),
                () -> assertTrue(
                        insufficientError.getMessage().contains("8")),
                () -> assertTrue(
                        insufficientError.getMessage().contains("6")));

        assertOriginalStateUnchanged();
    }

    @Test
    void rejectsNonIntegerZeroAndNegativeConsumeAmounts() {
        for (String parameter : new String[] {
                "zero", "negative", "enabled", "text"
        }) {
            var action = new ConsumeAction(
                    reference(parameter),
                    "GoldCoin");

            assertThrows(
                    IllegalArgumentException.class,
                    () -> execute(action),
                    "ConsumeAction must reject parameter: " + parameter);

            assertOriginalStateUnchanged();
        }

        var unsupportedAmount = new ConsumeAction(
                new LiteralExpression(3L),
                "GoldCoin");

        assertThrows(
                IllegalArgumentException.class,
                () -> execute(unsupportedAmount));

        assertOriginalStateUnchanged();
    }

    @Test
    void preservesVariablesSceneAndFinishedFlagForInventoryActions() {
        for (boolean finished : new boolean[] {false, true}) {
            var initialState = new GameState(
                    "entrance",
                    state.variables(),
                    Map.of("GoldCoin", 6, "Knife", 1),
                    state.itemStates(),
                    state.actorStates(),
                    finished);

            GameAction[] actions = {
                    new GiveAction(Optional.empty(), "GoldCoin"),
                    new ConsumeAction(
                            new LiteralExpression(1), "GoldCoin")
            };

            for (var action : actions) {
                var result = executor.execute(
                        action,
                        definition,
                        initialState,
                        ruleParameters);

                assertAll(
                        () -> assertNotSame(initialState, result),
                        () -> assertEquals(
                                initialState.variables(),
                                result.variables()),
                        () -> assertEquals(
                                initialState.currentSceneName(),
                                result.currentSceneName()),
                        () -> assertEquals(finished, result.finished()),
                        () -> assertEquals(
                                Map.of("GoldCoin", 6, "Knife", 1),
                                initialState.inventory()),
                        () -> assertEquals(
                                state.variables(),
                                initialState.variables()),
                        () -> assertEquals(
                                "entrance",
                                initialState.currentSceneName()),
                        () -> assertEquals(
                                finished,
                                initialState.finished()));
            }
        }
    }

    @Test
    void giveWithoutAmountAddsOne() {
        var original = state(Map.of());

        var result = execute(
                new GiveAction(Optional.empty(), "Knife"),
                original);

        assertNotSame(original, result);
        assertEquals(Map.of("Knife", 1), result.inventory());
        assertTrue(original.inventory().isEmpty());

        assertNonInventoryStateUnchanged(original, result);
    }

    @Test
    void consumeEvaluatesRequiredAmount() {
        var original = state(Map.of("GoldCoin", 6));

        var parameters = new LinkedHashMap<String, Object>();
        parameters.put("hp", 2);

        var parametersBefore = new LinkedHashMap<>(parameters);

        var result = executor.execute(
                new ConsumeAction(reference("hp"), "GoldCoin"),
                definition,
                original,
                parameters);

        assertEquals(Map.of("GoldCoin", 4), result.inventory());
        assertEquals(Map.of("GoldCoin", 6), original.inventory());
        assertEquals(parametersBefore, parameters);

        assertNonInventoryStateUnchanged(original, result);
    }

    @Test
    void givesMultipleStackableItems() {
        var original = state(Map.of());

        var first = execute(
                give(new LiteralExpression(2), "GoldCoin"),
                original);

        var second = execute(
                give(reference("amount"), "GoldCoin"),
                first);

        assertEquals(Map.of("GoldCoin", 2), first.inventory());
        assertEquals(Map.of("GoldCoin", 5), second.inventory());
        assertTrue(original.inventory().isEmpty());

        assertNonInventoryStateUnchanged(original, second);
    }

    @Test
    void rejectsSecondGiveOfSameNonStackableRoot() {
        var original = state(Map.of());

        var ownedState = execute(
                new GiveAction(Optional.empty(), "Knife"),
                original);

        var snapshot = copyState(ownedState);

        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> execute(
                        new GiveAction(Optional.empty(), "Knife"),
                        ownedState));

        assertTrue(exception.getMessage().contains("Knife"));
        assertEquals(snapshot, ownedState);
        assertEquals(Map.of("Knife", 1), ownedState.inventory());
        assertTrue(original.inventory().isEmpty());
    }

    @Test
    void allowsImplicitAndNamedNonStackableRootsOfSameType() {
        var original = state(Map.of());

        var withImplicitRoot = execute(
                new GiveAction(Optional.empty(), "Knife"),
                original);

        var withBothRoots = execute(
                new GiveAction(Optional.empty(), "blade"),
                withImplicitRoot);

        assertEquals(
                Map.of("Knife", 1, "blade", 1),
                withBothRoots.inventory());

        assertEquals(
                "Knife",
                withBothRoots.itemStates().get("Knife").typeName());

        assertEquals(
                "Knife",
                withBothRoots.itemStates().get("blade").typeName());

        assertNonInventoryStateUnchanged(original, withBothRoots);
    }

    @Test
    void keepsNamedAndImplicitStacksIndependent() {
        var original = state(Map.of("GoldCoin", 6, "purse", 2));

        var afterGive = execute(
                give(reference("amount"), "purse"),
                original);

        assertEquals(
                Map.of("GoldCoin", 6, "purse", 5),
                afterGive.inventory());

        var afterConsume = execute(
                new ConsumeAction(
                        new LiteralExpression(2), "GoldCoin"),
                afterGive);

        assertEquals(
                Map.of("GoldCoin", 4, "purse", 5),
                afterConsume.inventory());

        assertEquals(
                Map.of("GoldCoin", 6, "purse", 2),
                original.inventory());

        assertNonInventoryStateUnchanged(original, afterConsume);
    }

    @Test
    void removesInventoryKeyWhenConsumeReachesZero() {
        var original = state(Map.of("GoldCoin", 3, "purse", 2));

        var result = execute(
                new ConsumeAction(
                        reference("amount"), "GoldCoin"),
                original);

        assertEquals(Map.of("purse", 2), result.inventory());

        assertEquals(
                Map.of("GoldCoin", 3, "purse", 2),
                original.inventory());

        assertNonInventoryStateUnchanged(original, result);
    }

    @Test
    void preservesItemRuntimeStateAfterConsume() {
        var original = state(Map.of("Knife", 1));

        var result = execute(
                new ConsumeAction(
                        new LiteralExpression(1), "Knife"),
                original);

        assertTrue(result.inventory().isEmpty());
        assertTrue(result.itemStates().containsKey("Knife"));

        assertEquals(
                original.itemStates().get("Knife"),
                result.itemStates().get("Knife"));

        assertEquals(
                Integer.valueOf(9),
                assertInstanceOf(
                        Integer.class,
                        result.itemStates().get("Knife")
                                .properties().get("damage")));

        assertNonInventoryStateUnchanged(original, result);
    }

    @Test
    void reacquiringItemPreservesPreviousProperties() {
        var original = state(Map.of("Knife", 1));

        var consumed = execute(
                new ConsumeAction(
                        new LiteralExpression(1), "Knife"),
                original);

        var reacquired = execute(
                new GiveAction(Optional.empty(), "Knife"),
                consumed);

        assertTrue(consumed.inventory().isEmpty());
        assertEquals(Map.of("Knife", 1), reacquired.inventory());

        assertEquals(
                Integer.valueOf(9),
                reacquired.itemStates().get("Knife")
                        .properties().get("damage"));

        assertNonInventoryStateUnchanged(original, reacquired);
    }

    @Test
    void rejectsUnknownOrActorInventoryTarget() {
        var items = new LinkedHashMap<>(state(Map.of()).itemStates());
        items.remove("Knife");

        var original = new GameState(
                "entrance",
                variables(),
                Map.of(),
                items,
                actorStates(),
                false);

        var snapshot = copyState(original);

        for (String target : new String[] {
                "missing", "Knife", "Guard", "captain"
        }) {
            for (GameAction action : new GameAction[] {
                    new GiveAction(Optional.empty(), target),
                    new ConsumeAction(
                            new LiteralExpression(1), target)
            }) {
                var exception = assertThrows(
                        IllegalArgumentException.class,
                        () -> execute(action, original),
                        action.toString());

                assertTrue(
                        exception.getMessage().contains(target),
                        exception.getMessage());

                assertEquals(snapshot, original);
            }
        }
    }

    @Test
    void rejectsNonPositiveOrNonIntegerAmount() {
        var original = state(Map.of("GoldCoin", 6));
        var snapshot = copyState(original);

        for (Expression amount : new Expression[] {
                new LiteralExpression(0),
                new LiteralExpression(-1),
                new LiteralExpression(true),
                new LiteralExpression("3"),
                reference("zero"),
                reference("negative"),
                reference("booleanAmount"),
                reference("stringAmount")
        }) {
            for (GameAction action : new GameAction[] {
                    give(amount, "GoldCoin"),
                    new ConsumeAction(amount, "GoldCoin")
            }) {
                assertThrows(
                        IllegalArgumentException.class,
                        () -> execute(action, original),
                        action.toString());

                assertEquals(snapshot, original);
            }
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> execute(
                        new ConsumeAction(null, "GoldCoin"),
                        original));

        assertThrows(
                IllegalArgumentException.class,
                () -> execute(
                        give(new LiteralExpression(2), "Knife"),
                        original));

        assertEquals(snapshot, original);
    }

    @Test
    void leavesStateUnchangedAfterFailedInventoryAction() {
        var original = state(Map.of(
                "GoldCoin", Integer.MAX_VALUE,
                "purse", 2,
                "Knife", 1));

        var snapshot = copyState(original);

        var parameters = new LinkedHashMap<>(ruleParameters);
        var parametersBefore = new LinkedHashMap<>(parameters);

        assertThrows(
                ArithmeticException.class,
                () -> executor.execute(
                        give(new LiteralExpression(1), "GoldCoin"),
                        definition,
                        original,
                        parameters));

        assertEquals(snapshot, original);

        var insufficientStock = assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        new ConsumeAction(
                                reference("amount"), "purse"),
                        definition,
                        original,
                        parameters));

        assertTrue(
                insufficientStock.getMessage().contains("purse"));

        assertEquals(snapshot, original);

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        new GiveAction(Optional.empty(), "Knife"),
                        definition,
                        original,
                        parameters));

        assertEquals(snapshot, original);
        assertEquals(parametersBefore, parameters);
    }

    @Test
    void addsToGlobalIntegerVariable() {
        var original = numericState();

        var result = executeNumeric(
                new AddAction(
                        new LiteralExpression(3),
                        numericReference("hp")),
                original);

        assertNumericTransition(original, result, "hp", 10);
    }

    @Test
    void subtractsFromGlobalIntegerVariable() {
        var original = numericState();

        var result = executeNumeric(
                new SubtractAction(
                        new LiteralExpression(10),
                        numericReference("hp")),
                original);

        assertNumericTransition(original, result, "hp", -3);
    }

    @Test
    void multipliesGlobalIntegerVariable() {
        var original = numericState();

        var result = executeNumeric(
                new MultiplyAction(
                        numericReference("hp"),
                        new LiteralExpression(0)),
                original);

        assertNumericTransition(original, result, "hp", 0);
    }

    @Test
    void dividesGlobalIntegerVariable() {
        for (int initialValue : new int[] {7, -7}) {
            var original = numericStateWithHp(initialValue);

            var result = executeNumeric(
                    new DivideAction(
                            numericReference("hp"),
                            new LiteralExpression(2)),
                    original);

            assertNumericTransition(
                    original,
                    result,
                    "hp",
                    initialValue > 0 ? 3 : -3);
        }
    }

    @Test
    void modifiesImplicitItemIntegerProperty() {
        var original = numericState();

        var result = executeNumeric(
                new AddAction(
                        new LiteralExpression(3),
                        numericReference("Knife.damage")),
                original);

        assertNumericTransition(
                original, result, "Knife.damage", 12);
    }

    @Test
    void modifiesNamedItemIntegerProperty() {
        var original = numericState();

        var result = executeNumeric(
                new SubtractAction(
                        new LiteralExpression(15),
                        numericReference("blade.damage")),
                original);

        assertNumericTransition(
                original, result, "blade.damage", -3);
    }

    @Test
    void modifiesImplicitActorIntegerProperty() {
        var original = numericState();

        var result = executeNumeric(
                new MultiplyAction(
                        numericReference("Guard.hp"),
                        new LiteralExpression(2)),
                original);

        assertNumericTransition(
                original, result, "Guard.hp", 20);
    }

    @Test
    void modifiesNamedActorIntegerProperty() {
        var original = numericState();

        var result = executeNumeric(
                new DivideAction(
                        numericReference("captain.hp"),
                        new LiteralExpression(3)),
                original);

        assertNumericTransition(
                original, result, "captain.hp", 6);
    }

    @Test
    void resolvesNumericOperandsThroughExpressionEvaluator() {
        var expressions = List.<Expression>of(
                new LiteralExpression(3),
                numericReference("energy"),
                numericReference("amount"),
                numericReference("blade.damage"),
                numericReference("GoldCoin.count"));

        int[] operandValues = {3, 5, 3, 12, 6};

        for (int index = 0; index < expressions.size(); index++) {
            Expression operand = expressions.get(index);
            int value = operandValues[index];

            var actions = numericActions(
                    numericReference("hp"), operand);

            int[] expectedValues = {
                    7 + value,
                    7 - value,
                    7 * value,
                    7 / value
            };

            for (int actionIndex = 0;
                 actionIndex < actions.size();
                 actionIndex++) {

                var original = numericState();

                var result = executeNumeric(
                        actions.get(actionIndex), original);

                assertNumericTransition(
                        original,
                        result,
                        "hp",
                        expectedValues[actionIndex]);
            }
        }
    }

    @Test
    void rejectsRuleParameterAsWriteTarget() {
        var original = numericState();

        for (var action : numericActions(
                numericReference("amount"),
                new LiteralExpression(1))) {

            assertRejectedNumericAction(action, original);
        }
    }

    @Test
    void rejectsShadowedGlobalWriteTarget() {
        var original = numericState();

        var parameters = new LinkedHashMap<String, Object>();
        parameters.put("hp", 99);

        var originalParameters = new LinkedHashMap<>(parameters);
        var snapshot = numericCopy(original);

        for (var action : numericActions(
                numericReference("hp"),
                new LiteralExpression(1))) {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> executor.execute(
                            action,
                            numericDefinition,
                            original,
                            parameters));

            assertEquals(snapshot, original);
            assertEquals(originalParameters, parameters);
        }
    }

    @Test
    void rejectsVirtualCountWriteTarget() {
        var original = numericState();

        for (String target : List.of(
                "GoldCoin.count", "purse.count")) {

            for (var action : numericActions(
                    numericReference(target),
                    new LiteralExpression(1))) {

                assertRejectedNumericAction(action, original);
            }
        }
    }

    @Test
    void rejectsBooleanAndStringWriteTargets() {
        var original = numericState();

        for (String target : List.of(
                "doorOpen",
                "status",
                "Knife.usable",
                "Knife.label",
                "blade.usable",
                "blade.label",
                "Guard.hostile",
                "Guard.title",
                "captain.hostile",
                "captain.title")) {

            for (var action : numericActions(
                    numericReference(target),
                    new LiteralExpression(1))) {

                assertRejectedNumericAction(action, original);
            }
        }
    }

    @Test
    void rejectsUnknownRootOrPropertyWriteTarget() {
        var original = numericState();

        for (String target : List.of(
                "missing",
                "Missing.damage",
                "Knife.missing",
                "blade.missing",
                "Guard.missing",
                "captain.missing")) {

            for (var action : numericActions(
                    numericReference(target),
                    new LiteralExpression(1))) {

                assertRejectedNumericAction(action, original);
            }
        }
    }

    @Test
    void rejectsAdditionOverflow() {
        var original = numericStateWithHp(Integer.MAX_VALUE);

        assertArithmeticFailure(
                new AddAction(
                        new LiteralExpression(1),
                        numericReference("hp")),
                original,
                "AddAction",
                "hp");
    }

    @Test
    void rejectsSubtractionOverflow() {
        var original = numericStateWithHp(Integer.MIN_VALUE);

        assertArithmeticFailure(
                new SubtractAction(
                        new LiteralExpression(1),
                        numericReference("hp")),
                original,
                "SubtractAction",
                "hp");
    }

    @Test
    void rejectsMultiplicationOverflow() {
        var original = numericStateWithHp(Integer.MAX_VALUE);

        assertArithmeticFailure(
                new MultiplyAction(
                        numericReference("hp"),
                        new LiteralExpression(2)),
                original,
                "MultiplyAction",
                "hp");
    }

    @Test
    void rejectsDivisionByZero() {
        var original = numericState();

        assertArithmeticFailure(
                new DivideAction(
                        numericReference("captain.hp"),
                        new LiteralExpression(0)),
                original,
                "DivideAction",
                "captain.hp");
    }

    @Test
    void rejectsIntegerMinValueDivisionOverflow() {
        var original = numericStateWithHp(Integer.MIN_VALUE);

        assertArithmeticFailure(
                new DivideAction(
                        numericReference("hp"),
                        new LiteralExpression(-1)),
                original,
                "DivideAction",
                "hp");
    }

    private GameState execute(GameAction action) {
        return executor.execute(
                action, definition, state, ruleParameters);
    }

    private ReferenceExpression reference(String name) {
        return new ReferenceExpression(name, Optional.empty());
    }

    private void assertInventoryTransition(
            GameState original,
            GameState result,
            Map<String, Integer> expectedInventory) {

        assertAll(
                () -> assertNotSame(original, result),
                () -> assertEquals(
                        expectedInventory, result.inventory()),
                () -> assertEquals(
                        original.variables(), result.variables()),
                () -> assertEquals(
                        original.currentSceneName(),
                        result.currentSceneName()),
                () -> assertEquals(
                        original.finished(), result.finished()));
    }

    private void assertOriginalStateUnchanged() {
        assertAll(
                () -> assertEquals(
                        Map.of("GoldCoin", 6, "Knife", 1),
                        state.inventory()),
                () -> assertEquals(
                        Map.of(
                                "hp", 7,
                                "gold", 10,
                                "doorOpen", true,
                                "status", "open"),
                        state.variables()),
                () -> assertEquals(
                        "entrance", state.currentSceneName()),
                () -> assertFalse(state.finished()));
    }

    private GameState execute(GameAction action, GameState initialState) {
        return executor.execute(
                action, definition, initialState, ruleParameters);
    }

    private GiveAction give(Expression amount, String target) {
        return new GiveAction(Optional.of(amount), target);
    }

    private GameState state(Map<String, Integer> inventory) {
        return new GameState(
                state.currentSceneName(),
                variables(),
                inventory,
                runtimeItemStates(),
                actorStates(),
                state.finished());
    }

    private Map<String, Object> variables() {
        return state.variables();
    }

    private Map<String, ItemRuntimeState> runtimeItemStates() {
        var items = new LinkedHashMap<String, ItemRuntimeState>();

        items.put(
                "GoldCoin",
                new ItemRuntimeState(
                        "GoldCoin", Map.of("value", 1)));

        items.put(
                "purse",
                new ItemRuntimeState(
                        "GoldCoin", Map.of("value", 2)));

        items.put(
                "Knife",
                new ItemRuntimeState(
                        "Knife", Map.of(
                        "damage", 9,
                        "usable", true,
                        "label", "Used knife")));

        items.put(
                "blade",
                new ItemRuntimeState(
                        "Knife", Map.of(
                        "damage", 12,
                        "usable", false,
                        "label", "Named blade")));

        return items;
    }

    private Map<String, ActorRuntimeState> actorStates() {
        var actors = new LinkedHashMap<String, ActorRuntimeState>();

        actors.put(
                "Guard",
                new ActorRuntimeState(
                        "Guard", Map.of("hp", 10)));

        actors.put(
                "captain",
                new ActorRuntimeState(
                        "Guard", Map.of("hp", 20)));

        return actors;
    }

    private GameState copyState(GameState original) {
        return new GameState(
                original.currentSceneName(),
                original.variables(),
                original.inventory(),
                original.itemStates(),
                original.actorStates(),
                original.finished());
    }

    private void assertNonInventoryStateUnchanged(
            GameState original,
            GameState result) {

        assertEquals(
                original.currentSceneName(),
                result.currentSceneName());

        assertEquals(original.finished(), result.finished());
        assertEquals(original.variables(), result.variables());
        assertEquals(original.itemStates(), result.itemStates());
        assertEquals(original.actorStates(), result.actorStates());
    }

    private final GameDefinition numericDefinition =
            new DslParserFacade().parse("""
                vars {
                    hp: 10
                    energy: 5
                    doorOpen: false
                    status: "ready"
                }
                items {
                    stackable item GoldCoin {
                        description: "Coin"
                        value: 1
                    }
                    purse as GoldCoin

                    item Knife {
                        description: "Knife"
                        damage: 3
                        usable: true
                        label: "Knife"
                    }
                    blade as Knife
                }
                actors {
                    actor Guard {
                        description: "Guard"
                        hp: 10
                        hostile: false
                        title: "Guard"
                    }
                    captain as Guard
                }
                world { scenes {
                    scene entrance {
                        description: "Entrance"
                    }
                } }
                """);

    private GameState numericState() {
        return new GameState(
                "entrance",
                Map.of(
                        "hp", 7,
                        "energy", 5,
                        "doorOpen", true,
                        "status", "open"),
                Map.of(
                        "GoldCoin", 6,
                        "purse", 2),
                Map.of(
                        "GoldCoin",
                        new ItemRuntimeState(
                                "GoldCoin",
                                Map.of("value", 1)),
                        "purse",
                        new ItemRuntimeState(
                                "GoldCoin",
                                Map.of("value", 2)),
                        "Knife",
                        new ItemRuntimeState(
                                "Knife",
                                Map.of(
                                        "damage", 9,
                                        "usable", true,
                                        "label", "Used knife")),
                        "blade",
                        new ItemRuntimeState(
                                "Knife",
                                Map.of(
                                        "damage", 12,
                                        "usable", false,
                                        "label", "Named blade"))),
                Map.of(
                        "Guard",
                        new ActorRuntimeState(
                                "Guard",
                                Map.of(
                                        "hp", 10,
                                        "hostile", false,
                                        "title", "Guard")),
                        "captain",
                        new ActorRuntimeState(
                                "Guard",
                                Map.of(
                                        "hp", 20,
                                        "hostile", true,
                                        "title", "Captain"))),
                true);
    }

    private GameState numericStateWithHp(int hp) {
        var original = numericState();
        var variables = new LinkedHashMap<>(original.variables());
        variables.put("hp", hp);

        return new GameState(
                original.currentSceneName(),
                variables,
                original.inventory(),
                original.itemStates(),
                original.actorStates(),
                original.finished());
    }

    private ReferenceExpression numericReference(String path) {
        int separator = path.indexOf('.');

        if (separator < 0) {
            return new ReferenceExpression(
                    path, Optional.empty());
        }

        return new ReferenceExpression(
                path.substring(0, separator),
                Optional.of(path.substring(separator + 1)));
    }

    private List<GameAction> numericActions(
            ReferenceExpression target,
            Expression operand) {

        return List.of(
                new AddAction(operand, target),
                new SubtractAction(operand, target),
                new MultiplyAction(target, operand),
                new DivideAction(target, operand));
    }

    private GameState executeNumeric(
            GameAction action,
            GameState original) {

        var parameters = new LinkedHashMap<String, Object>();
        parameters.put("amount", 3);

        var parametersBefore = new LinkedHashMap<>(parameters);

        var result = executor.execute(
                action, numericDefinition, original, parameters);

        assertEquals(parametersBefore, parameters);

        return result;
    }

    private GameState numericCopy(GameState original) {
        return new GameState(
                original.currentSceneName(),
                original.variables(),
                original.inventory(),
                original.itemStates(),
                original.actorStates(),
                original.finished());
    }

    private void assertRejectedNumericAction(
            GameAction action,
            GameState original) {

        var snapshot = numericCopy(original);

        var parameters = new LinkedHashMap<String, Object>();
        parameters.put("amount", 3);

        var parametersBefore = new LinkedHashMap<>(parameters);

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        action,
                        numericDefinition,
                        original,
                        parameters),
                action.toString());

        assertEquals(snapshot, original);
        assertEquals(parametersBefore, parameters);
    }

    private void assertArithmeticFailure(
            GameAction action,
            GameState original,
            String actionName,
            String targetName) {

        var snapshot = numericCopy(original);

        var parameters = new LinkedHashMap<String, Object>();
        parameters.put("amount", 3);

        var parametersBefore = new LinkedHashMap<>(parameters);

        var exception = assertThrows(
                ArithmeticException.class,
                () -> executor.execute(
                        action,
                        numericDefinition,
                        original,
                        parameters));

        assertTrue(
                exception.getMessage().contains(actionName),
                exception.getMessage());

        assertTrue(
                exception.getMessage().contains(targetName),
                exception.getMessage());

        assertEquals(snapshot, original);
        assertEquals(parametersBefore, parameters);
    }

    private void assertNumericTransition(
            GameState original,
            GameState result,
            String path,
            int expectedValue) {

        var expectedVariables =
                new LinkedHashMap<>(original.variables());

        var expectedItems =
                new LinkedHashMap<>(original.itemStates());

        var expectedActors =
                new LinkedHashMap<>(original.actorStates());

        int separator = path.indexOf('.');

        if (separator < 0) {
            expectedVariables.put(path, expectedValue);
        } else {
            String root = path.substring(0, separator);
            String property = path.substring(separator + 1);

            if (expectedItems.containsKey(root)) {
                var item = expectedItems.get(root);

                var properties =
                        new LinkedHashMap<>(item.properties());

                properties.put(property, expectedValue);

                expectedItems.put(
                        root,
                        new ItemRuntimeState(
                                item.typeName(), properties));
            } else {
                var actor = expectedActors.get(root);

                var properties =
                        new LinkedHashMap<>(actor.properties());

                properties.put(property, expectedValue);

                expectedActors.put(
                        root,
                        new ActorRuntimeState(
                                actor.typeName(), properties));
            }
        }

        var expectedState = new GameState(
                original.currentSceneName(),
                expectedVariables,
                original.inventory(),
                expectedItems,
                expectedActors,
                original.finished());

        assertNotSame(original, result);
        assertEquals(expectedState, result);
        assertEquals(numericStateWithHp(
                (Integer) original.variables().get("hp")), original);
    }

}