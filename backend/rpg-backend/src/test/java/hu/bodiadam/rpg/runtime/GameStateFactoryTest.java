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
    @Test
    void createsImplicitRuntimeStateForEveryDefinition() {
        var definition = parser.parse("""
            items {
                stackable item GoldCoin {
                    description: "Coin"
                }
                item Knife {
                    description: "Knife"
                }
            }
            actors {
                actor Guard {
                    description: "Guard"
                }
                actor Merchant {
                    description: "Merchant"
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

        var state = factory.create(definition);

        assertEquals(
                java.util.List.of("GoldCoin", "Knife"),
                new java.util.ArrayList<>(state.itemStates().keySet()));

        assertEquals(
                java.util.List.of("Guard", "Merchant"),
                new java.util.ArrayList<>(state.actorStates().keySet()));

        assertEquals(
                new ItemRuntimeState("GoldCoin", Map.of()),
                state.itemStates().get("GoldCoin"));

        assertEquals(
                new ItemRuntimeState("Knife", Map.of()),
                state.itemStates().get("Knife"));

        assertEquals(
                new ActorRuntimeState("Guard", Map.of()),
                state.actorStates().get("Guard"));

        assertEquals(
                new ActorRuntimeState("Merchant", Map.of()),
                state.actorStates().get("Merchant"));
    }
    @Test
    void createsTopLevelNamedItemAndActorInstances() {
        var definition = parser.parse("""
            items {
                stackable item GoldCoin {
                    description: "Coin"
                }
                coins as GoldCoin
            }
            actors {
                actor Guard {
                    description: "Guard"
                }
                gateGuard as Guard
            }
            world {
                scenes {
                    scene entrance {
                        description: "Entrance"
                    }
                }
            }
            """);

        var state = factory.create(definition);

        assertEquals(
                java.util.List.of("GoldCoin", "coins"),
                new java.util.ArrayList<>(state.itemStates().keySet()));

        assertEquals(
                java.util.List.of("Guard", "gateGuard"),
                new java.util.ArrayList<>(state.actorStates().keySet()));

        assertEquals(
                new ItemRuntimeState("GoldCoin", Map.of()),
                state.itemStates().get("coins"));

        assertEquals(
                new ActorRuntimeState("Guard", Map.of()),
                state.actorStates().get("gateGuard"));
    }
    @Test
    void createsStartBlockNamedItemAndActorInstances() {
        var definition = parser.parse("""
            items {
                stackable item GoldCoin {
                    description: "Coin"
                }
                coins as GoldCoin
            }
            actors {
                actor Guard {
                    description: "Guard"
                }
                gateGuard as Guard
            }
            start {
                purse as GoldCoin
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

        var state = factory.create(definition);

        assertEquals(
                java.util.List.of("GoldCoin", "coins", "purse"),
                new java.util.ArrayList<>(state.itemStates().keySet()));

        assertEquals(
                java.util.List.of("Guard", "gateGuard", "captain"),
                new java.util.ArrayList<>(state.actorStates().keySet()));

        assertEquals(
                new ItemRuntimeState("GoldCoin", Map.of()),
                state.itemStates().get("purse"));

        assertEquals(
                new ActorRuntimeState("Guard", Map.of()),
                state.actorStates().get("captain"));
    }
    @Test
    void startsWithEmptyInventoryEvenWhenRuntimeItemStatesExist() {
        var definition = parser.parse("""
            items {
                stackable item GoldCoin {
                    description: "Coin"
                }
                coins as GoldCoin
            }
            start {
                purse as GoldCoin
                give 2 GoldCoin
                give 3 coins
                give 4 purse
            }
            world {
                scenes {
                    scene entrance {
                        description: "Entrance"
                    }
                }
            }
            """);

        var state = factory.create(definition);

        assertEquals(
                java.util.List.of("GoldCoin", "coins", "purse"),
                new java.util.ArrayList<>(state.itemStates().keySet()));

        assertTrue(
                state.inventory().isEmpty(),
                "The factory must register runtime roots without executing start actions");
    }
    @Test
    void rejectsDuplicateRuntimeRootDuringBootstrap() {
        var definition = parser.parse("""
            items {
                stackable item GoldCoin {
                    description: "Coin"
                }
                item Knife {
                    description: "Knife"
                }
                loot as GoldCoin
            }
            start {
                loot as Knife
            }
            world {
                scenes {
                    scene entrance {
                        description: "Entrance"
                    }
                }
            }
            """);

        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> factory.create(definition));

        assertTrue(
                exception.getMessage().contains("loot"),
                "The bootstrap error must identify the duplicate runtime root");
    }
    @Test
    void materializesInheritedItemPropertiesWithChildOverride() {
        var definition = parser.parse("""
            items {
                item Equipment {
                    description: "Base equipment"
                    rarity: "Common"
                    damage: 1
                }
                item Weapon is Equipment {
                    description: "Weapon"
                    damage: 2
                    usable: true
                }
                item Knife is Weapon {
                    description: "Knife"
                    damage: 3
                    weight: 1
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

        var state = factory.create(definition);
        var knife = state.itemStates().get("Knife");

        assertInstanceOf(ItemRuntimeState.class, knife);
        assertEquals("Knife", knife.typeName());

        assertEquals(
                Map.of(
                        "rarity", "Common",
                        "damage", 3,
                        "usable", true,
                        "weight", 1),
                knife.properties());

        assertEquals(
                java.util.List.of("rarity", "damage", "usable", "weight"),
                new java.util.ArrayList<>(knife.properties().keySet()));

        assertEquals(
                1,
                state.itemStates().get("Equipment").properties().get("damage"));

        assertEquals(
                2,
                state.itemStates().get("Weapon").properties().get("damage"));
    }

    @Test
    void materializesInheritedActorPropertiesWithChildOverride() {
        var definition = parser.parse("""
            actors {
                actor Person {
                    description: "Person"
                    title: "Citizen"
                    hp: 10
                }
                actor Guard is Person {
                    description: "Guard"
                    hp: 15
                    hostile: false
                }
                actor Captain is Guard {
                    description: "Captain"
                    hp: 20
                    suspicion: 4
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

        var state = factory.create(definition);
        var captain = state.actorStates().get("Captain");

        assertInstanceOf(ActorRuntimeState.class, captain);
        assertEquals("Captain", captain.typeName());

        assertEquals(
                Map.of(
                        "title", "Citizen",
                        "hp", 20,
                        "hostile", false,
                        "suspicion", 4),
                captain.properties());

        assertEquals(
                java.util.List.of("title", "hp", "hostile", "suspicion"),
                new java.util.ArrayList<>(captain.properties().keySet()));

        assertEquals(
                10,
                state.actorStates().get("Person").properties().get("hp"));

        assertEquals(
                15,
                state.actorStates().get("Guard").properties().get("hp"));
    }
    @Test
    void createsIndependentSnapshotsForImplicitAndNamedInstances() {
        var definition = parser.parse("""
            items {
                item Sword {
                    description: "Sword"
                    damage: 3
                }
                rustySword as Sword
            }
            actors {
                actor Guard {
                    description: "Guard"
                    hp: 10
                }
                gateGuard as Guard
            }
            world {
                scenes {
                    scene entrance {
                        description: "Entrance"
                    }
                }
            }
            """);

        var original = factory.create(definition);

        assertEquals(
                Map.of("damage", 3),
                original.itemStates().get("rustySword").properties());

        assertEquals(
                Map.of("hp", 10),
                original.actorStates().get("gateGuard").properties());

        var itemProperties = new java.util.LinkedHashMap<>(
                original.itemStates().get("rustySword").properties());
        itemProperties.put("damage", 9);

        var items = new java.util.LinkedHashMap<>(original.itemStates());
        items.put(
                "rustySword",
                new ItemRuntimeState("Sword", itemProperties));

        var actorProperties = new java.util.LinkedHashMap<>(
                original.actorStates().get("gateGuard").properties());
        actorProperties.put("hp", 25);

        var actors = new java.util.LinkedHashMap<>(original.actorStates());
        actors.put(
                "gateGuard",
                new ActorRuntimeState("Guard", actorProperties));

        var updated = new GameState(
                original.currentSceneName(),
                original.variables(),
                original.inventory(),
                items,
                actors,
                original.finished());

        assertEquals(
                9,
                updated.itemStates().get("rustySword").properties().get("damage"));
        assertEquals(
                3,
                updated.itemStates().get("Sword").properties().get("damage"));

        assertEquals(
                25,
                updated.actorStates().get("gateGuard").properties().get("hp"));
        assertEquals(
                10,
                updated.actorStates().get("Guard").properties().get("hp"));

        assertEquals(
                3,
                original.itemStates().get("rustySword").properties().get("damage"));
        assertEquals(
                10,
                original.actorStates().get("gateGuard").properties().get("hp"));
    }
    @Test
    void doesNotStoreDescriptionOrCountAsRuntimeProperty() {
        var definition = parser.parse("""
            items {
                stackable item GoldCoin {
                    description: "Coin"
                    value: 1
                }
                coins as GoldCoin
            }
            actors {
                actor Guard {
                    description: "Guard"
                    hp: 10
                }
                gateGuard as Guard
            }
            world {
                scenes {
                    scene entrance {
                        description: "Entrance"
                    }
                }
            }
            """);

        var state = factory.create(definition);

        assertEquals(
                java.util.List.of("GoldCoin", "coins"),
                new java.util.ArrayList<>(state.itemStates().keySet()));

        assertEquals(
                java.util.List.of("Guard", "gateGuard"),
                new java.util.ArrayList<>(state.actorStates().keySet()));

        for (var item : state.itemStates().values()) {
            assertEquals(Map.of("value", 1), item.properties());
            assertFalse(item.properties().containsKey("description"));
            assertFalse(item.properties().containsKey("count"));
        }

        for (var actor : state.actorStates().values()) {
            assertEquals(Map.of("hp", 10), actor.properties());
            assertFalse(actor.properties().containsKey("description"));
            assertFalse(actor.properties().containsKey("count"));
        }
    }

    @Test
    void evaluatesPropertyInitializerOnceAtBootstrap() {
        var definition = parser.parse("""
            vars { baseDamage: 7 }
            items {
                item Weapon {
                    description: "Weapon"
                    damage: baseDamage
                    copiedDamage: Weapon.damage
                    anotherCopy: Weapon.damage
                }
            }
            world { scenes {
                scene entrance { description: "Entrance" }
            } }
            """);

        var state = factory.create(definition);
        var properties = state.itemStates().get("Weapon").properties();

        assertEquals(7, properties.get("damage"));
        assertEquals(7, properties.get("copiedDamage"));
        assertEquals(7, properties.get("anotherCopy"));
    }
    @Test
    void snapshotDoesNotTrackLaterGlobalVariableChange() {
        var definition = parser.parse("""
            vars { baseDamage: 7 }
            items {
                item Weapon {
                    description: "Weapon"
                    damage: baseDamage
                }
            }
            world { scenes {
                scene entrance { description: "Entrance" }
            } }
            """);

        var initialState = factory.create(definition);

        var changedVariables = new java.util.LinkedHashMap<>(
                initialState.variables());
        changedVariables.put("baseDamage", 99);

        var changedState = new GameState(
                initialState.currentSceneName(),
                changedVariables,
                initialState.inventory(),
                initialState.itemStates(),
                initialState.actorStates(),
                initialState.finished());

        assertEquals(99, changedState.variables().get("baseDamage"));
        assertEquals(
                7,
                changedState.itemStates().get("Weapon")
                        .properties().get("damage"));

        assertEquals(7, initialState.variables().get("baseDamage"));
    }
    @Test
    void rejectsDirectPropertyCycleDuringBootstrap() {
        var definition = parser.parse("""
            items {
                item Weapon {
                    description: "Weapon"
                    damage: Weapon.damage
                }
            }
            world { scenes {
                scene entrance { description: "Entrance" }
            } }
            """);

        var exception = assertThrows(
                IllegalStateException.class,
                () -> factory.create(definition));

        assertTrue(exception.getMessage().contains("Weapon.damage"));
    }
    @Test
    void rejectsIndirectPropertyCycleDuringBootstrap() {
        var definition = parser.parse("""
            items {
                item Weapon {
                    description: "Weapon"
                    damage: Guard.strength
                }
                item Potion {
                    description: "Potion"
                    power: Weapon.damage
                }
            }
            actors {
                actor Guard {
                    description: "Guard"
                    strength: Potion.power
                }
            }
            world { scenes {
                scene entrance { description: "Entrance" }
            } }
            """);

        var exception = assertThrows(
                IllegalStateException.class,
                () -> factory.create(definition));

        assertTrue(exception.getMessage().contains("Weapon.damage"));
    }

    @Test
    void allowsMultipleReferencesToAlreadyMaterializedProperty() {
        var definition = parser.parse("""
            items {
                item Weapon {
                    description: "Weapon"
                    damage: Source.value
                    bonus: Source.value
                }
                item Source {
                    description: "Source"
                    value: 7
                }
            }
            actors {
                actor Guard {
                    description: "Guard"
                    strength: Source.value
                }
            }
            world { scenes {
                scene entrance { description: "Entrance" }
            } }
            """);

        var state = factory.create(definition);

        assertEquals(
                7,
                state.itemStates().get("Source").properties().get("value"));
        assertEquals(
                7,
                state.itemStates().get("Weapon").properties().get("damage"));
        assertEquals(
                7,
                state.itemStates().get("Weapon").properties().get("bonus"));
        assertEquals(
                7,
                state.actorStates().get("Guard").properties().get("strength"));
    }

    @Test
    void doesNotExecuteStartActionsDuringBootstrap() {
        var definition = parser.parse("""
            vars { hp: 7 }
            items {
                stackable item GoldCoin {
                    description: "Coin"
                    value: 1
                }
                item Charm {
                    description: "Charm"
                    initialHp: hp
                }
            }
            start {
                give 2 GoldCoin
                add 10 to hp
            }
            world { scenes {
                scene entrance { description: "Entrance" }
            } }
            """);

        var state = factory.create(definition);

        assertTrue(
                state.inventory().isEmpty(),
                "A start give action végrehajtása a motor feladata");
        assertEquals(
                7,
                state.variables().get("hp"),
                "A factory nem hajthatja végre a start add actiont");
        assertEquals(
                7,
                state.itemStates().get("Charm")
                        .properties().get("initialHp"));
    }
}

