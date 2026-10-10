package hu.bodiadam.rpg.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hu.bodiadam.rpg.dsl.model.GameDefinition;
import hu.bodiadam.rpg.dsl.model.expression.Expression;
import hu.bodiadam.rpg.dsl.model.expression.LiteralExpression;
import hu.bodiadam.rpg.dsl.parser.DslParserFacade;
import hu.bodiadam.rpg.runtime.GameState;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExpressionEvaluatorTest {
    private final DslParserFacade parser = new DslParserFacade();
    private final ExpressionEvaluator evaluator = new ExpressionEvaluator();
    private final GameDefinition definition = createDefinition();

    private final GameState state = new GameState(
            "entrance",
            Map.of("hp", 7, "doorOpen", true, "status", "open"),
            Map.of("GoldCoin", 6, "HealingPotion", 2, "Knife", 1),
            false);

    @Test
    void returnsIntegerBooleanAndStringLiteralsWithoutConversion() {
        var integer = evaluate(new LiteralExpression(5));
        var bool = evaluate(new LiteralExpression(true));
        var string = evaluate(new LiteralExpression("open"));

        assertEquals(Integer.valueOf(5), assertInstanceOf(Integer.class, integer));
        assertEquals(Boolean.TRUE, assertInstanceOf(Boolean.class, bool));
        assertEquals("open", assertInstanceOf(String.class, string));
    }

    @Test
    void resolvesGlobalVariableFromCurrentState() {
        assertEquals(
                Integer.valueOf(7),
                assertInstanceOf(Integer.class, evaluate(reference("hp"))));

        assertEquals(
                Boolean.TRUE,
                assertInstanceOf(Boolean.class, evaluate(reference("doorOpen"))));

        assertEquals(
                "open",
                assertInstanceOf(String.class, evaluate(reference("status"))));
    }

    @Test
    void rejectsUnknownSimpleReference() {
        var expression = reference("missingVariable");

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(expression));
    }

    @Test
    void rejectsNullAndUnsupportedLiteralValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(new LiteralExpression(null)));

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(new LiteralExpression(5.0)));
    }

    @Test
    void resolvesRuleParameter() {
        var result = evaluator.evaluate(
                reference("amount"),
                definition,
                state,
                Map.of("amount", 3));

        assertEquals(Integer.valueOf(3), assertInstanceOf(Integer.class, result));
    }

    @Test
    void ruleParameterShadowsGlobalVariableWithSameName() {
        var result = evaluator.evaluate(
                reference("hp"),
                definition,
                state,
                Map.of("hp", 99));

        assertEquals(Integer.valueOf(99), assertInstanceOf(Integer.class, result));
        assertEquals(Integer.valueOf(7), state.variables().get("hp"));
    }

    @Test
    void rejectsNullRuleParameterValue() {
        var parameters = new HashMap<String, Object>();
        parameters.put("hp", null);
        var expression = reference("hp");

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluator.evaluate(
                        expression, definition, state, parameters));
    }

    @Test
    void preservesResolvedValueType() {
        var expression = reference("amount");

        for (Object value : new Object[] {5, true, "5"}) {
            var result = evaluator.evaluate(
                    expression,
                    definition,
                    state,
                    Map.of("amount", value));

            assertEquals(value, result);
            assertEquals(value.getClass(), result.getClass());
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluator.evaluate(
                        expression, definition, state, Map.of("amount", 5L)));
    }

    @Test
    void resolvesStackableItemCountFromInventory() {
        var result = evaluate(reference("GoldCoin.count"));

        assertEquals(Integer.valueOf(6), assertInstanceOf(Integer.class, result));
    }

    @Test
    void returnsZeroForMissingStackableItemCount() {
        var emptyInventoryState = new GameState(
                "entrance", state.variables(), Map.of(), false);

        var result = evaluator.evaluate(
                reference("GoldCoin.count"),
                definition,
                emptyInventoryState,
                Map.of());

        assertEquals(Integer.valueOf(0), assertInstanceOf(Integer.class, result));
    }

    @Test
    void rejectsCountOnNonStackableItem() {
        var expression = reference("Knife.count");

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(expression));
    }

    @Test
    void resolvesDirectItemProperty() {
        var result = evaluate(reference("HealingPotion.healAmount"));

        assertEquals(Integer.valueOf(4), assertInstanceOf(Integer.class, result));
    }

    @Test
    void resolvesInheritedItemProperty() {
        var result = evaluate(reference("Knife.rarity"));

        assertEquals("Common", assertInstanceOf(String.class, result));
    }

    @Test
    void childPropertyOverridesInheritedProperty() {
        var result = evaluate(reference("Knife.damage"));

        assertEquals(Integer.valueOf(3), assertInstanceOf(Integer.class, result));
    }

    @Test
    void resolvesActorPropertyThroughNamedInstance() {
        var result = evaluate(reference("captain.suspicion"));

        assertEquals(Integer.valueOf(4), assertInstanceOf(Integer.class, result));
    }

    @Test
    void resolvesPropertyExpressionRecursively() {
        var result = evaluate(reference("Knife.healing"));

        assertEquals(Integer.valueOf(4), assertInstanceOf(Integer.class, result));
    }

    @Test
    void rejectsUnknownPropertyRoot() {
        var expression = reference("missingItem.damage");

        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(expression));

        assertTrue(exception.getMessage().contains("missingItem"));
    }

    @Test
    void rejectsUnknownProperty() {
        var expression = reference("Knife.missingProperty");

        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> evaluate(expression));

        assertTrue(exception.getMessage().contains("Knife"));
        assertTrue(exception.getMessage().contains("missingProperty"));
    }

    @Test
    void rejectsAmbiguousPropertyRoot() {
        var actorDefinition = parser.parse("""
                actors {
                    actor GoldCoin {
                        description: "Actor with a conflicting name"
                        value: 9
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

        var ambiguousDefinition = new GameDefinition(
                definition.initPrompt(),
                definition.items(),
                actorDefinition.actors(),
                definition.variables(),
                definition.start(),
                definition.rules(),
                definition.world());

        var expression = reference("GoldCoin.value");

        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> evaluator.evaluate(
                        expression, ambiguousDefinition, state, Map.of()));

        assertTrue(exception.getMessage().contains("Ambiguous"));
        assertTrue(exception.getMessage().contains("GoldCoin"));
    }

    @Test
    void rejectsCyclicPropertyResolution() {
        var cyclicDefinition = parser.parse("""
                items {
                    item Sword {
                        description: "Sword"
                        damage: Gem.power
                    }
                    item Gem {
                        description: "Gem"
                        power: Sword.damage
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

        var expression = reference("Sword.damage");

        assertThrows(
                IllegalStateException.class,
                () -> evaluator.evaluate(
                        expression, cyclicDefinition, state, Map.of()));
    }

    private Object evaluate(Expression expression) {
        return evaluator.evaluate(
                expression, definition, state, Map.of());
    }

    private Expression reference(String name) {
        var referenceDefinition = parser.parse("""
                vars {
                    probe: %s
                }
                world {
                    scenes {
                        scene entrance {
                            description: "Entrance"
                        }
                    }
                }
                """.formatted(name));

        return referenceDefinition.variables().get(0).value();
    }

    private GameDefinition createDefinition() {
        return parser.parse("""
                vars {
                    hp: 12
                    doorOpen: false
                    status: "closed"
                }
                items {
                    item Weapon {
                        description: "Base weapon"
                        damage: 1
                        rarity: "Common"
                    }
                    item Knife is Weapon {
                        description: "Knife"
                        damage: 3
                        healing: HealingPotion.healAmount
                    }
                    stackable item GoldCoin {
                        description: "Coin"
                        value: 1
                    }
                    stackable item HealingPotion {
                        description: "Potion"
                        healAmount: 4
                    }
                }
                actors {
                    actor CityGuard {
                        description: "Guard"
                        suspicion: 2
                    }
                    actor GateCaptain is CityGuard {
                        description: "Captain"
                        suspicion: 4
                    }
                }
                start {
                    captain as GateCaptain
                }
                world {
                    scenes {
                        scene entrance {
                            description: "Entrance"
                            present {
                                captain
                            }
                        }
                    }
                }
                """);
    }
}