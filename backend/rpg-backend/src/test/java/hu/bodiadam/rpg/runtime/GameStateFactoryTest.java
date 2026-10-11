package hu.bodiadam.rpg.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hu.bodiadam.rpg.dsl.parser.DslParserFacade;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GameStateFactoryTest {
    private final DslParserFacade parser = new DslParserFacade();
    private final GameStateFactory factory = new GameStateFactory();

    @Test
    void createsStateFromLiteralVariablesAndFirstScene() {
        var definition = parser.parse("""
                vars {
                    health: 12
                    doorOpen: false
                    title: "Traveler"
                }
                world { scenes {
                    scene entrance { description: "The entrance" }
                    scene cellar { description: "The cellar" }
                } }
                """);

        var state = factory.create(definition);

        assertEquals("entrance", state.currentSceneName());
        assertEquals(12, assertInstanceOf(Integer.class, state.variables().get("health")));
        assertEquals(false, assertInstanceOf(Boolean.class, state.variables().get("doorOpen")));
        assertEquals("Traveler", assertInstanceOf(String.class, state.variables().get("title")));
        assertEquals(3, state.variables().size());
        assertFalse(state.finished());
    }

    @Test
    void startsWithEmptyInventoryBeforeStartActionsAreExecuted() {
        var definition = parser.parse("""
        items {
            stackable item Coin {
                description: "Coin"
            }
        }
        start { give 2 Coin }
        world { scenes {
            scene entrance { description: "The entrance" }
        } }
        """);

        var state = factory.create(definition);

        assertTrue(state.inventory().isEmpty());
    }

    @Test
    void marksStateFinishedWhenInitialSceneIsTerminal() {
        var definition = parser.parse("""
                world { scenes {
                    scene ending { description: "The end" end scene }
                    scene later { description: "Unreachable" }
                } }
                """);

        var state = factory.create(definition);

        assertEquals("ending", state.currentSceneName());
        assertTrue(state.finished());
    }

    @Test
    void rejectsGameWithoutScenes() {
        var definition = parser.parse("""
                world { scenes { } }
                """);

        var exception = assertThrows(IllegalStateException.class, () -> factory.create(definition));
        assertTrue(exception.getMessage().toLowerCase().contains("game definition must contain at least one scene"));
    }

    @Test
    void rejectsNonLiteralVariableInitializer() {
        var definition = parser.parse("""
                vars { health: startingHealth }
                world { scenes {
                    scene entrance { description: "The entrance" }
                } }
                """);

        var exception = assertThrows(IllegalArgumentException.class, () -> factory.create(definition));
        assertTrue(exception.getMessage().contains("health"));
    }

    @Test
    void doesNotExposeMutableInputMaps() {
        var variables = new HashMap<String, Object>();
        variables.put("health", 12);
        var inventory = new HashMap<String, Integer>();
        inventory.put("Coin", 2);
        var state = new GameState(
                "entrance",
                variables,
                inventory,
                Map.of("Coin", new ItemRuntimeState("Coin", Map.of())),
                Map.of(),
                false);

        variables.put("health", 1);
        inventory.put("Coin", 99);
        assertEquals(12, state.variables().get("health"));
        assertEquals(2, state.inventory().get("Coin"));
        assertThrows(UnsupportedOperationException.class, () -> state.variables().put("health", 1));
        assertThrows(UnsupportedOperationException.class, () -> state.inventory().put("Coin", 99));
        assertEquals(Map.of("health", 12), state.variables());
        assertEquals(Map.of("Coin", 2), state.inventory());
    }
}
