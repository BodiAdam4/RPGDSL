package hu.bodiadam.rpg.engine;

import static hu.bodiadam.rpg.dsl.model.condition.ComparisonOperator.*;
import static org.junit.jupiter.api.Assertions.*;

import hu.bodiadam.rpg.dsl.model.GameDefinition;
import hu.bodiadam.rpg.dsl.model.condition.ComparisonCondition;
import hu.bodiadam.rpg.dsl.model.condition.ComparisonOperator;
import hu.bodiadam.rpg.dsl.model.condition.Condition;
import hu.bodiadam.rpg.dsl.model.condition.HasCondition;
import hu.bodiadam.rpg.dsl.model.condition.LogicalCondition;
import hu.bodiadam.rpg.dsl.model.condition.LogicalOperator;
import hu.bodiadam.rpg.dsl.model.expression.LiteralExpression;
import hu.bodiadam.rpg.dsl.model.expression.ReferenceExpression;
import hu.bodiadam.rpg.dsl.parser.DslParserFacade;
import hu.bodiadam.rpg.runtime.GameState;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConditionEvaluatorTest {

    private final ExpressionEvaluator expressionEvaluator =
            new ExpressionEvaluator();

    private final ConditionEvaluator evaluator =
            new ConditionEvaluator(expressionEvaluator);

    private final DslParserFacade parser = new DslParserFacade();

    private final Map<String, Object> ruleParameters =
            Map.of("amount", 3);

    private GameDefinition definition;
    private GameState state;

    @BeforeAll
    void setUp() {
        definition = parser.parse("""
                vars {
                    hp: 12
                    doorOpen: false
                    status: "closed"
                }
                items {
                    stackable item GoldCoin {
                        description: "Coin"
                        value: 1
                    }
                    stackable item HealingPotion {
                        description: "Potion"
                        healAmount: 4
                    }
                    item Knife {
                        description: "Knife"
                        damage: 3
                    }
                }
                world {
                    scenes {
                        scene entrance {
                            description: "Entrance"
                        }
                    }
                }
                """);

        state = new GameState(
                "entrance",
                Map.of(
                        "hp", 7,
                        "doorOpen", true,
                        "status", "open"),
                Map.of(
                        "GoldCoin", 6,
                        "HealingPotion", 0),
                false);
    }

    @Test
    void returnsTrueWhenInventoryContainsPositiveCount() {
        assertTrue(evaluate(new HasCondition("GoldCoin", false)));
    }

    @Test
    void returnsFalseWhenInventoryEntryIsMissingOrZero() {
        assertFalse(evaluate(new HasCondition("HealingPotion", false)));
        assertFalse(evaluate(new HasCondition("Knife", false)));
    }

    @Test
    void negatedHasConditionReturnsExactOpposite() {
        for (String target : new String[] {
                "GoldCoin", "HealingPotion", "Knife"
        }) {
            boolean positive = evaluate(new HasCondition(target, false));
            boolean negative = evaluate(new HasCondition(target, true));

            assertEquals(!positive, negative, target);
        }
    }

    @ParameterizedTest(name = "{1} {0} {2} -> {3}")
    @MethodSource("integerComparisons")
    void evaluatesIntegerComparisons(
            ComparisonOperator operator,
            int left,
            int right,
            boolean expected) {

        assertEquals(expected, compare(left, operator, right));
    }

    static Stream<Arguments> integerComparisons() {
        return Stream.of(
                Arguments.of(LESS_THAN, 4, 5, true),
                Arguments.of(LESS_THAN, 5, 5, false),
                Arguments.of(GREATER_THAN, 6, 5, true),
                Arguments.of(GREATER_THAN, 5, 5, false),
                Arguments.of(AT_LEAST, 5, 5, true),
                Arguments.of(AT_LEAST, 4, 5, false),
                Arguments.of(AT_MOST, 5, 5, true),
                Arguments.of(AT_MOST, 6, 5, false),
                Arguments.of(EQUAL_TO, 5, 5, true),
                Arguments.of(EQUAL_TO, 5, 6, false),
                Arguments.of(NOT_EQUAL_TO, 5, 6, true),
                Arguments.of(NOT_EQUAL_TO, 5, 5, false));
    }

    @ParameterizedTest
    @MethodSource("equalityCases")
    void evaluatesEqualityForIntegerBooleanAndStringValues(
            Object left,
            Object right,
            boolean expected) {

        assertEquals(expected, compare(left, EQUAL_TO, right));
    }

    @ParameterizedTest
    @MethodSource("equalityCases")
    void evaluatesInequalityForIntegerBooleanAndStringValues(
            Object left,
            Object right,
            boolean equal) {

        assertEquals(!equal, compare(left, NOT_EQUAL_TO, right));
    }

    static Stream<Arguments> equalityCases() {
        return Stream.of(
                Arguments.of(5, 5, true),
                Arguments.of(5, 6, false),
                Arguments.of(true, true, true),
                Arguments.of(true, false, false),
                Arguments.of("open", "open", true),
                Arguments.of("open", "closed", false),
                Arguments.of("open", "Open", false));
    }

    @ParameterizedTest
    @MethodSource("differentRuntimeTypes")
    void rejectsEqualityBetweenDifferentRuntimeTypes(
            ComparisonOperator operator,
            Object left,
            Object right) {

        var error = assertThrows(
                IllegalArgumentException.class,
                () -> compare(left, operator, right));

        assertTrue(error.getMessage().contains(operator.name()));
        assertTrue(error.getMessage().contains(
                left.getClass().getSimpleName()));
        assertTrue(error.getMessage().contains(
                right.getClass().getSimpleName()));
    }

    static Stream<Arguments> differentRuntimeTypes() {
        return Stream.of(EQUAL_TO, NOT_EQUAL_TO)
                .flatMap(operator -> Stream.of(
                        Arguments.of(operator, 5, "5"),
                        Arguments.of(operator, "5", 5),
                        Arguments.of(operator, 1, true),
                        Arguments.of(operator, true, "true")));
    }

    @ParameterizedTest
    @MethodSource("unsupportedOrderingCases")
    void rejectsOrderingForBooleanAndStringValues(
            ComparisonOperator operator,
            Object left,
            Object right) {

        var error = assertThrows(
                IllegalArgumentException.class,
                () -> compare(left, operator, right));

        assertTrue(error.getMessage().contains(operator.name()));
        assertTrue(error.getMessage().contains(
                left.getClass().getSimpleName()));
        assertTrue(error.getMessage().contains(
                right.getClass().getSimpleName()));
    }

    static Stream<Arguments> unsupportedOrderingCases() {
        return Stream.of(
                        LESS_THAN, GREATER_THAN, AT_LEAST, AT_MOST)
                .flatMap(operator -> Stream.of(
                        Arguments.of(operator, true, false),
                        Arguments.of(operator, "4", "5")));
    }

    @Test
    void resolvesBothOperandsThroughExpressionEvaluator() {
        // A bal oldal aktuális state-változó: hp = 7.
        // A jobb oldal itemproperty: healAmount = 4.
        var condition = new ComparisonCondition(
                reference("hp"),
                GREATER_THAN,
                propertyReference("HealingPotion", "healAmount"));

        assertTrue(evaluate(condition));

        // A definícióban hp = 12, de a state-ben hp = 7.
        assertTrue(evaluate(new ComparisonCondition(
                reference("hp"),
                EQUAL_TO,
                new LiteralExpression(7))));

        // A Boolean és String state-változók is feloldhatók.
        assertTrue(evaluate(new ComparisonCondition(
                reference("doorOpen"),
                EQUAL_TO,
                new LiteralExpression(true))));

        assertTrue(evaluate(new ComparisonCondition(
                reference("status"),
                EQUAL_TO,
                new LiteralExpression("open"))));
    }

    @Test
    void resolvesRuleParameterOnEitherComparisonSideWhereTheModelAllowsIt() {
        var parameterOnLeft = new ComparisonCondition(
                reference("amount"),
                EQUAL_TO,
                new LiteralExpression(3));

        assertTrue(evaluate(parameterOnLeft));

        var parameterOnRight = new ComparisonCondition(
                reference("hp"),
                GREATER_THAN,
                reference("amount"));

        assertTrue(evaluate(parameterOnRight));

        var parameterOnBothSides = new ComparisonCondition(
                reference("amount"),
                EQUAL_TO,
                reference("amount"));

        assertTrue(evaluate(parameterOnBothSides));
    }

    @Test
    void propagatesExpressionEvaluationFailure() {
        var unknownLeft = new ComparisonCondition(
                reference("missingVariable"),
                EQUAL_TO,
                new LiteralExpression(1));

        var leftError = assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(unknownLeft));

        assertTrue(leftError.getMessage().contains("missingVariable"));

        var unknownRight = new ComparisonCondition(
                reference("hp"),
                EQUAL_TO,
                reference("missingRightVariable"));

        var rightError = assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(unknownRight));

        assertTrue(rightError.getMessage().contains("missingRightVariable"));

        var unsupportedLiteral = new ComparisonCondition(
                reference("hp"),
                EQUAL_TO,
                new LiteralExpression(7.0));

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(unsupportedLiteral));
    }

    @Test
    void shortCircuitsAndWhenLeftSideIsFalse() {
        var condition = new LogicalCondition(
                new HasCondition("Knife", false),
                LogicalOperator.AND,
                failingComparison());

        assertFalse(evaluate(condition));
    }

    @Test
    void shortCircuitsOrWhenLeftSideIsTrue() {
        var condition = new LogicalCondition(
                new HasCondition("GoldCoin", false),
                LogicalOperator.OR,
                failingComparison());

        assertTrue(evaluate(condition));
    }

    @Test
    void evaluatesRightSideOfAndWhenLeftSideIsTrue() {
        var condition = new LogicalCondition(
                new HasCondition("GoldCoin", false),
                LogicalOperator.AND,
                failingComparison());

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(condition));
    }

    @Test
    void evaluatesRightSideOfOrWhenLeftSideIsFalse() {
        var condition = new LogicalCondition(
                new HasCondition("Knife", false),
                LogicalOperator.OR,
                failingComparison());

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(condition));
    }

    @Test
    void evaluatesNestedLogicalConditions() {
        // (GoldCoin birtokolt ÉS hp > amount) VAGY Knife birtokolt.
        var condition = new LogicalCondition(
                new LogicalCondition(
                        new HasCondition("GoldCoin", false),
                        LogicalOperator.AND,
                        new ComparisonCondition(
                                reference("hp"),
                                GREATER_THAN,
                                reference("amount"))),
                LogicalOperator.OR,
                new HasCondition("Knife", false));

        assertTrue(evaluate(condition));

        // Ugyanaz a fa más inventoryval más eredményt ad.
        var emptyState = new GameState(
                state.currentSceneName(),
                state.variables(),
                Map.of(),
                false);

        assertFalse(evaluator.evaluate(
                condition, definition, emptyState, ruleParameters));
    }

    @Test
    void honorsParserLeftToRightGrouping() {
        // A = true, B = false, C = false.
        // (A OR B) AND C = false.
        var condition = parseCondition(
                "has GoldCoin or has Knife and has HealingPotion");

        var outer = assertInstanceOf(LogicalCondition.class, condition);
        assertEquals(LogicalOperator.AND, outer.operator());

        var left = assertInstanceOf(
                LogicalCondition.class, outer.left());
        assertEquals(LogicalOperator.OR, left.operator());

        assertFalse(evaluate(condition));
    }

    @Test
    void parenthesesOverrideDefaultGrouping() {
        // A = true, B = false, C = false.
        // A OR (B AND C) = true.
        var condition = parseCondition(
                "has GoldCoin or (has Knife and has HealingPotion)");

        var outer = assertInstanceOf(LogicalCondition.class, condition);
        assertEquals(LogicalOperator.OR, outer.operator());

        var right = assertInstanceOf(
                LogicalCondition.class, outer.right());
        assertEquals(LogicalOperator.AND, right.operator());

        assertTrue(evaluate(condition));
    }

    @Test
    void rejectsMalformedConditionsOnReachedBranches() {
        assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(new HasCondition(" ", false)));

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(new ComparisonCondition(
                        null, EQUAL_TO, new LiteralExpression(1))));

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(new ComparisonCondition(
                        reference("hp"), null, new LiteralExpression(1))));

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(new ComparisonCondition(
                        reference("hp"), EQUAL_TO, null)));

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(new LogicalCondition(
                        new HasCondition("GoldCoin", false),
                        null,
                        new HasCondition("Knife", false))));

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(new LogicalCondition(
                        null,
                        LogicalOperator.AND,
                        new HasCondition("GoldCoin", false))));

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(new LogicalCondition(
                        new HasCondition("GoldCoin", false),
                        LogicalOperator.AND,
                        null)));

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(new LogicalCondition(
                        new HasCondition("Knife", false),
                        LogicalOperator.OR,
                        null)));
    }

    @Test
    void doesNotValidateUnreachedLogicalBranches() {
        assertFalse(evaluate(new LogicalCondition(
                new HasCondition("Knife", false),
                LogicalOperator.AND,
                null)));

        assertTrue(evaluate(new LogicalCondition(
                new HasCondition("GoldCoin", false),
                LogicalOperator.OR,
                null)));
    }

    @Test
    void rejectsNullPublicInputs() {
        var condition = new HasCondition("GoldCoin", false);

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluator.evaluate(
                        null, definition, state, ruleParameters));

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluator.evaluate(
                        condition, null, state, ruleParameters));

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluator.evaluate(
                        condition, definition, null, ruleParameters));

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluator.evaluate(
                        condition, definition, state, null));
    }

    @Test
    void doesNotModifyStateOrRuleParameters() {
        var parameters = new HashMap<String, Object>(ruleParameters);
        var originalParameters = new HashMap<>(parameters);
        var originalVariables = new HashMap<>(state.variables());
        var originalInventory = new HashMap<>(state.inventory());

        var condition = new LogicalCondition(
                new HasCondition("GoldCoin", false),
                LogicalOperator.AND,
                new ComparisonCondition(
                        reference("hp"),
                        GREATER_THAN,
                        reference("amount")));

        assertTrue(evaluator.evaluate(
                condition, definition, state, parameters));

        assertEquals(originalParameters, parameters);
        assertEquals(originalVariables, state.variables());
        assertEquals(originalInventory, state.inventory());
    }

    private boolean evaluate(Condition condition) {
        return evaluator.evaluate(
                condition, definition, state, ruleParameters);
    }

    private boolean compare(
            Object left,
            ComparisonOperator operator,
            Object right) {

        var condition = new ComparisonCondition(
                reference("left"),
                operator,
                new LiteralExpression(right));

        return evaluator.evaluate(
                condition,
                definition,
                state,
                Map.<String, Object>of("left", left));
    }

    private ReferenceExpression reference(String name) {
        return new ReferenceExpression(name, Optional.empty());
    }

    private ReferenceExpression propertyReference(
            String root,
            String property) {

        return new ReferenceExpression(root, Optional.of(property));
    }

    private ComparisonCondition failingComparison() {
        return new ComparisonCondition(
                reference("missingVariable"),
                EQUAL_TO,
                new LiteralExpression(1));
    }

    private Condition parseCondition(String source) {
        var parsedDefinition = parser.parse("""
                items {
                    stackable item GoldCoin {
                        description: "Coin"
                    }
                    stackable item HealingPotion {
                        description: "Potion"
                    }
                    item Knife {
                        description: "Knife"
                    }
                }
                rules {
                    rule probe {
                        requires { %s }
                        success {}
                        fail {}
                    }
                }
                world {
                    scenes {
                        scene entrance {
                            description: "Entrance"
                        }
                    }
                }
                """.formatted(source));
        return parsedDefinition.rules().get(0).requires().orElseThrow();
    }
}