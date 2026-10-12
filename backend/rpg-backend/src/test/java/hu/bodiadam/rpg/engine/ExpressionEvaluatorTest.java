package hu.bodiadam.rpg.engine;

import hu.bodiadam.rpg.dsl.model.GameDefinition;
import hu.bodiadam.rpg.dsl.model.expression.Expression;
import hu.bodiadam.rpg.dsl.model.expression.LiteralExpression;
import hu.bodiadam.rpg.dsl.parser.DslParserFacade;
import hu.bodiadam.rpg.runtime.ActorRuntimeState;
import hu.bodiadam.rpg.runtime.GameState;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import hu.bodiadam.rpg.runtime.ItemRuntimeState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionEvaluatorTest {
    private final DslParserFacade parser = new DslParserFacade();
    private final ExpressionEvaluator evaluator = new ExpressionEvaluator();
    private final GameDefinition definition = createDefinition();

    private final GameState state = new GameState(
            "entrance",
            Map.of("hp", 7, "doorOpen", true, "status", "open"),
            Map.of("GoldCoin", 6, "HealingPotion", 2, "Knife", 1),
            createItemStates(),
            createActorStates(),
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
                "entrance", state.variables(), Map.of(), state.itemStates(), state.actorStates(), false);

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
    void rejectsGlobalVariableAsPropertyRootDespiteMatchingItemDefinition() {
        var conflictingDefinition = parser.parse("""
            vars { hp: 7 }
            items {
                item hp {
                    description: "Item with a conflicting name"
                    value: 9
                }
            }
            world { scenes {
                scene entrance { description: "Entrance" }
            } }
            """);

        var expression = reference("hp.value");

        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> evaluator.evaluate(
                        expression, conflictingDefinition, state, Map.of()));

        assertTrue(exception.getMessage().contains("Scalar"));
        assertTrue(exception.getMessage().contains("hp.value"));
    }

    @Test
    void rejectsRuleParameterAsPropertyRootDespiteMatchingItemDefinition() {
        var expression = reference("Knife.damage");

        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> evaluator.evaluate(
                        expression, definition, state, Map.of("Knife", 5)));

        assertTrue(exception.getMessage().contains("Scalar"));
        assertTrue(exception.getMessage().contains("Knife.damage"));
    }


    private Map<String, ItemRuntimeState> createItemStates() {
        var items = new LinkedHashMap<String, ItemRuntimeState>();

        var weaponProperties = new LinkedHashMap<String, Object>();
        weaponProperties.put("damage", 1);
        weaponProperties.put("rarity", "Common");

        items.put(
                "Weapon",
                new ItemRuntimeState("Weapon", weaponProperties));

        var knifeProperties = new LinkedHashMap<String, Object>();
        knifeProperties.put("damage", 3);
        knifeProperties.put("rarity", "Common");
        knifeProperties.put("healing", 4);

        items.put(
                "Knife",
                new ItemRuntimeState("Knife", knifeProperties));

        items.put(
                "GoldCoin",
                new ItemRuntimeState("GoldCoin", Map.of("value", 1)));

        items.put(
                "HealingPotion",
                new ItemRuntimeState(
                        "HealingPotion", Map.of("healAmount", 4)));

        return items;
    }

    private Map<String, ActorRuntimeState> createActorStates() {
        var actors = new LinkedHashMap<String, ActorRuntimeState>();

        actors.put(
                "CityGuard",
                new ActorRuntimeState("CityGuard", Map.of("suspicion", 2)));

        actors.put(
                "GateCaptain",
                new ActorRuntimeState("GateCaptain", Map.of("suspicion", 4)));

        actors.put(
                "captain",
                new ActorRuntimeState("GateCaptain", Map.of("suspicion", 4)));

        return actors;
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

    @Test
    void readsImplicitItemPropertyFromRuntimeState() {
        var definition = parser.parse("""
            items {
                item Knife {
                    description: "Knife"
                    damage: 3
                }
            }
            world { scenes {
                scene entrance { description: "Entrance" }
            } }
            """);

        var state = new GameState(
                "entrance",
                Map.of(),
                Map.of(),
                Map.of(
                        "Knife",
                        new ItemRuntimeState(
                                "Knife",
                                Map.of("damage", 99))),
                Map.of(),
                false);

        var result = evaluator.evaluate(
                reference("Knife.damage"),
                definition,
                state,
                Map.of());

        assertEquals(
                Integer.valueOf(99),
                assertInstanceOf(Integer.class, result));
    }
    @Test
    void readsNamedItemPropertyFromRuntimeState() {
        var definition = parser.parse("""
            items {
                item Knife {
                    description: "Knife"
                    damage: 3
                }
                blade as Knife
            }
            world { scenes {
                scene entrance { description: "Entrance" }
            } }
            """);

        var state = new GameState(
                "entrance",
                Map.of(),
                Map.of(),
                Map.of(
                        "Knife",
                        new ItemRuntimeState(
                                "Knife",
                                Map.of("damage", 10)),
                        "blade",
                        new ItemRuntimeState(
                                "Knife",
                                Map.of("damage", 99))),
                Map.of(),
                false);

        var result = evaluator.evaluate(
                reference("blade.damage"),
                definition,
                state,
                Map.of());

        assertEquals(
                Integer.valueOf(99),
                assertInstanceOf(Integer.class, result));
    }
    @Test
    void readsItemPropertyWithoutInventoryOwnership() {
        var definition = createRuntimeReadingDefinition();

        var state = new GameState(
                "entrance",
                Map.of(),
                Map.of(),
                Map.of(
                        "Knife",
                        new ItemRuntimeState(
                                "Knife", Map.of("damage", 10)),
                        "blade",
                        new ItemRuntimeState(
                                "Knife", Map.of("damage", 99))),
                Map.of(),
                false);

        assertTrue(state.inventory().isEmpty());

        assertEquals(
                Integer.valueOf(10),
                assertInstanceOf(
                        Integer.class,
                        evaluator.evaluate(
                                reference("Knife.damage"),
                                definition, state, Map.of())));

        assertEquals(
                Integer.valueOf(99),
                assertInstanceOf(
                        Integer.class,
                        evaluator.evaluate(
                                reference("blade.damage"),
                                definition, state, Map.of())));
    }

    @Test
    void readsImplicitActorPropertyFromRuntimeState() {
        var definition = createRuntimeReadingDefinition();

        var state = new GameState(
                "entrance",
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(
                        "Guard",
                        new ActorRuntimeState(
                                "Guard", Map.of("suspicion", 20))),
                false);

        var result = evaluator.evaluate(
                reference("Guard.suspicion"),
                definition, state, Map.of());

        assertEquals(
                Integer.valueOf(20),
                assertInstanceOf(Integer.class, result));
    }

    @Test
    void readsNamedActorPropertyFromRuntimeState() {
        var definition = createRuntimeReadingDefinition();

        var state = new GameState(
                "entrance",
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(
                        "Guard",
                        new ActorRuntimeState(
                                "Guard", Map.of("suspicion", 20)),
                        "captain",
                        new ActorRuntimeState(
                                "Guard", Map.of("suspicion", 40))),
                false);

        var result = evaluator.evaluate(
                reference("captain.suspicion"),
                definition, state, Map.of());

        assertEquals(
                Integer.valueOf(40),
                assertInstanceOf(Integer.class, result));
    }

    @Test
    void readsVirtualCountFromInventory() {
        var definition = createRuntimeReadingDefinition();

        var itemStates = Map.of(
                "GoldCoin",
                new ItemRuntimeState(
                        "GoldCoin", Map.of("value", 1)));

        var ownedState = new GameState(
                "entrance",
                Map.of(),
                Map.of("GoldCoin", 6),
                itemStates,
                Map.of(),
                false);

        var unownedState = new GameState(
                "entrance",
                Map.of(),
                Map.of(),
                itemStates,
                Map.of(),
                false);

        assertEquals(
                Integer.valueOf(6),
                assertInstanceOf(
                        Integer.class,
                        evaluator.evaluate(
                                reference("GoldCoin.count"),
                                definition, ownedState, Map.of())));

        assertEquals(
                Integer.valueOf(0),
                assertInstanceOf(
                        Integer.class,
                        evaluator.evaluate(
                                reference("GoldCoin.count"),
                                definition, unownedState, Map.of())));

        assertFalse(
                itemStates.get("GoldCoin").properties().containsKey("count"));
    }

    @Test
    void keepsImplicitAndNamedStackCountsIndependent() {
        var definition = createRuntimeReadingDefinition();

        var itemStates = Map.of(
                "GoldCoin",
                new ItemRuntimeState(
                        "GoldCoin", Map.of("value", 1)),
                "purse",
                new ItemRuntimeState(
                        "GoldCoin", Map.of("value", 1)));

        var state = new GameState(
                "entrance",
                Map.of(),
                Map.of("GoldCoin", 6, "purse", 2),
                itemStates,
                Map.of(),
                false);

        assertEquals(
                Integer.valueOf(6),
                evaluator.evaluate(
                        reference("GoldCoin.count"),
                        definition, state, Map.of()));

        assertEquals(
                Integer.valueOf(2),
                evaluator.evaluate(
                        reference("purse.count"),
                        definition, state, Map.of()));

        var stateWithoutNamedInventoryEntry = new GameState(
                "entrance",
                Map.of(),
                Map.of("GoldCoin", 6),
                itemStates,
                Map.of(),
                false);

        assertEquals(
                Integer.valueOf(0),
                evaluator.evaluate(
                        reference("purse.count"),
                        definition, stateWithoutNamedInventoryEntry, Map.of()));
    }

    @Test
    void ruleParameterShadowsRuntimeRoot() {
        var definition = createRuntimeReadingDefinition();

        var state = new GameState(
                "entrance",
                Map.of(),
                Map.of(),
                Map.of(
                        "Knife",
                        new ItemRuntimeState(
                                "Knife", Map.of("damage", 99))),
                Map.of(
                        "Guard",
                        new ActorRuntimeState(
                                "Guard", Map.of("suspicion", 40))),
                false);

        var parameters = new LinkedHashMap<String, Object>();
        parameters.put("Knife", 5);
        parameters.put("Guard", true);

        var originalParameters = new LinkedHashMap<>(parameters);

        assertEquals(
                Integer.valueOf(5),
                evaluator.evaluate(
                        reference("Knife"),
                        definition, state, parameters));

        assertEquals(
                Boolean.TRUE,
                evaluator.evaluate(
                        reference("Guard"),
                        definition, state, parameters));

        var itemError = assertThrows(
                IllegalArgumentException.class,
                () -> evaluator.evaluate(
                        reference("Knife.damage"),
                        definition, state, parameters));

        var actorError = assertThrows(
                IllegalArgumentException.class,
                () -> evaluator.evaluate(
                        reference("Guard.suspicion"),
                        definition, state, parameters));

        assertTrue(itemError.getMessage().contains("Knife"));
        assertTrue(actorError.getMessage().contains("Guard"));

        assertEquals(originalParameters, parameters);
    }

    @Test
    void rejectsMissingRuntimeRootWithoutDefinitionFallback() {
        var definition = createRuntimeReadingDefinition();

        var state = new GameState(
                "entrance",
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                false);

        for (String path : new String[] {
                "Knife.damage",
                "blade.damage",
                "Guard.suspicion",
                "captain.suspicion"
        }) {
            var expression = reference(path);

            var exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> evaluator.evaluate(
                            expression, definition, state, Map.of()),
                    path);

            String root = path.substring(0, path.indexOf('.'));
            assertTrue(exception.getMessage().contains(root));
        }
    }

    @Test
    void rejectsMissingRuntimePropertyWithoutDefinitionFallback() {
        var definition = createRuntimeReadingDefinition();

        var state = new GameState(
                "entrance",
                Map.of(),
                Map.of(),
                Map.of(
                        "Knife",
                        new ItemRuntimeState("Knife", Map.of()),
                        "blade",
                        new ItemRuntimeState("Knife", Map.of())),
                Map.of(
                        "Guard",
                        new ActorRuntimeState("Guard", Map.of()),
                        "captain",
                        new ActorRuntimeState("Guard", Map.of())),
                false);

        for (String path : new String[] {
                "Knife.damage",
                "blade.damage",
                "Guard.suspicion",
                "captain.suspicion"
        }) {
            var expression = reference(path);

            var exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> evaluator.evaluate(
                            expression, definition, state, Map.of()),
                    path);

            int separator = path.indexOf('.');
            String root = path.substring(0, separator);
            String property = path.substring(separator + 1);

            assertTrue(exception.getMessage().contains(root));
            assertTrue(exception.getMessage().contains(property));
        }
    }

    private GameDefinition createRuntimeReadingDefinition() {
        return parser.parse("""
            items {
                item Knife {
                    description: "Knife"
                    damage: 3
                }
                blade as Knife

                stackable item GoldCoin {
                    description: "Coin"
                    value: 1
                }
                purse as GoldCoin
            }
            actors {
                actor Guard {
                    description: "Guard"
                    suspicion: 2
                }
                captain as Guard
            }
            world { scenes {
                scene entrance { description: "Entrance" }
            } }
            """);
    }
}