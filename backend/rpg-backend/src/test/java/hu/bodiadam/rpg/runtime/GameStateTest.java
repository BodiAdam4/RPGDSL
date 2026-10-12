package hu.bodiadam.rpg.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GameStateTest {

    @Test
    void copiesVariablesInventoryAndRuntimeStatesDeeply() {
        var variables = variablesFixture();
        var inventory = inventoryFixture();

        var itemProperties = itemPropertiesFixture();
        var actorProperties = actorPropertiesFixture();

        var item = new ItemRuntimeState("GoldCoin", itemProperties);
        var actor = new ActorRuntimeState("Guard", actorProperties);

        var itemStates = itemStatesFixture();
        var actorStates = actorStatesFixture();

        itemStates.put("GoldCoin", item);
        actorStates.put("Guard", actor);

        var state = new GameState(
                "entrance",
                variables,
                inventory,
                itemStates,
                actorStates,
                false);

        variables.put("hp", 999);
        inventory.put("GoldCoin", 999);

        itemProperties.put("value", 999);
        actorProperties.put("hp", 999);

        itemStates.clear();
        actorStates.clear();

        assertAll(
                () -> assertEquals(7, state.variables().get("hp")),
                () -> assertEquals(6, state.inventory().get("GoldCoin")),
                () -> assertEquals(
                        1,
                        state.itemStates()
                                .get("GoldCoin")
                                .properties()
                                .get("value")),
                () -> assertEquals(
                        10,
                        state.actorStates()
                                .get("Guard")
                                .properties()
                                .get("hp")),
                () -> assertEquals(
                        List.of("GoldCoin", "coins", "Knife"),
                        keys(state.itemStates())),
                () -> assertEquals(
                        List.of("Guard", "gateGuard"),
                        keys(state.actorStates())));
    }

    @Test
    void preservesDeclarationOrderInAllStateMaps() {
        var state = stateFixture();

        assertAll(
                () -> assertEquals(
                        List.of("hp", "doorOpen", "status"),
                        keys(state.variables())),
                () -> assertEquals(
                        List.of("GoldCoin", "coins", "Knife"),
                        keys(state.inventory())),
                () -> assertEquals(
                        List.of("GoldCoin", "coins", "Knife"),
                        keys(state.itemStates())),
                () -> assertEquals(
                        List.of("Guard", "gateGuard"),
                        keys(state.actorStates())));

        for (var item : state.itemStates().values()) {
            assertEquals(
                    List.of("value", "usable", "label"),
                    keys(item.properties()));
        }

        for (var actor : state.actorStates().values()) {
            assertEquals(
                    List.of("hp", "hostile", "title"),
                    keys(actor.properties()));
        }
    }

    @Test
    void rejectsNullAndBlankSceneName() {
        for (String sceneName : new String[] {null, "", " ", "\t\n"}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new GameState(
                            sceneName,
                            variablesFixture(),
                            inventoryFixture(),
                            itemStatesFixture(),
                            actorStatesFixture(),
                            false));
        }
    }

    @Test
    void rejectsNullStateMaps() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new GameState(
                                "entrance",
                                null,
                                inventoryFixture(),
                                itemStatesFixture(),
                                actorStatesFixture(),
                                false)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new GameState(
                                "entrance",
                                variablesFixture(),
                                null,
                                itemStatesFixture(),
                                actorStatesFixture(),
                                false)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new GameState(
                                "entrance",
                                variablesFixture(),
                                inventoryFixture(),
                                null,
                                actorStatesFixture(),
                                false)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new GameState(
                                "entrance",
                                variablesFixture(),
                                inventoryFixture(),
                                itemStatesFixture(),
                                null,
                                false)));
    }

    @Test
    void rejectsNullAndBlankKeysInStateMaps() {
        for (String key : new String[] {null, "", " ", "\t\n"}) {
            var variables = variablesFixture();
            variables.put(key, 1);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> createState(
                            variables,
                            inventoryFixture(),
                            itemStatesFixture(),
                            actorStatesFixture()));

            var inventory = inventoryFixture();
            inventory.put(key, 1);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> createState(
                            variablesFixture(),
                            inventory,
                            itemStatesFixture(),
                            actorStatesFixture()));

            var items = itemStatesFixture();
            items.put(key, itemFixture());

            assertThrows(
                    IllegalArgumentException.class,
                    () -> createState(
                            variablesFixture(),
                            inventoryFixture(),
                            items,
                            actorStatesFixture()));

            var actors = actorStatesFixture();
            actors.put(key, actorFixture());

            assertThrows(
                    IllegalArgumentException.class,
                    () -> createState(
                            variablesFixture(),
                            inventoryFixture(),
                            itemStatesFixture(),
                            actors));
        }
    }

    @Test
    void rejectsNullAndUnsupportedVariableValues() {
        for (Object value : new Object[] {
                null, 1L, 1.5, List.of(1)
        }) {
            var variables = variablesFixture();
            variables.put("hp", value);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> createState(
                            variables,
                            inventoryFixture(),
                            itemStatesFixture(),
                            actorStatesFixture()));
        }
    }

    @Test
    void rejectsNullRuntimeStateEntries() {
        var items = itemStatesFixture();
        items.put("coins", null);

        var actors = actorStatesFixture();
        actors.put("gateGuard", null);

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> createState(
                                variablesFixture(),
                                inventoryFixture(),
                                items,
                                actorStatesFixture())),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> createState(
                                variablesFixture(),
                                inventoryFixture(),
                                itemStatesFixture(),
                                actors)));
    }

    @Test
    void rejectsRootSharedByItemAndActorStates() {
        var actors = actorStatesFixture();
        actors.put("GoldCoin", actorFixture());

        assertThrows(
                IllegalArgumentException.class,
                () -> createState(
                        variablesFixture(),
                        inventoryFixture(),
                        itemStatesFixture(),
                        actors));
    }

    @Test
    void rejectsInventoryRootMissingFromItemStates() {
        var inventory = inventoryFixture();

        inventory.put("gateGuard", 1);

        assertThrows(
                IllegalArgumentException.class,
                () -> createState(
                        variablesFixture(),
                        inventory,
                        itemStatesFixture(),
                        actorStatesFixture()));

        inventory.remove("gateGuard");
        inventory.put("missingItem", 1);

        assertThrows(
                IllegalArgumentException.class,
                () -> createState(
                        variablesFixture(),
                        inventory,
                        itemStatesFixture(),
                        actorStatesFixture()));
    }

    @Test
    void rejectsZeroAndNegativeInventoryCounts() {
        for (int count : new int[] {0, -1, Integer.MIN_VALUE}) {
            var inventory = inventoryFixture();
            inventory.put("GoldCoin", count);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> createState(
                            variablesFixture(),
                            inventory,
                            itemStatesFixture(),
                            actorStatesFixture()));
        }
    }

    @Test
    void rejectsNullInventoryCount() {
        var inventory = inventoryFixture();
        inventory.put("GoldCoin", null);

        assertThrows(
                IllegalArgumentException.class,
                () -> createState(
                        variablesFixture(),
                        inventory,
                        itemStatesFixture(),
                        actorStatesFixture()));
    }

    @Test
    void allowsRuntimeItemsWithoutInventoryEntries() {
        var state = createState(
                variablesFixture(),
                new LinkedHashMap<>(),
                itemStatesFixture(),
                actorStatesFixture());

        assertTrue(state.inventory().isEmpty());
        assertEquals(
                List.of("GoldCoin", "coins", "Knife"),
                keys(state.itemStates()));
    }

    @Test
    void rejectsNullAndBlankRuntimeTypeNames() {
        for (String typeName : new String[] {null, "", " ", "\t\n"}) {
            assertAll(
                    () -> assertThrows(
                            IllegalArgumentException.class,
                            () -> new ItemRuntimeState(
                                    typeName, itemPropertiesFixture())),
                    () -> assertThrows(
                            IllegalArgumentException.class,
                            () -> new ActorRuntimeState(
                                    typeName, actorPropertiesFixture())));
        }
    }

    @Test
    void rejectsNullRuntimePropertyMaps() {
        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new ItemRuntimeState("GoldCoin", null)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new ActorRuntimeState("Guard", null)));
    }

    @Test
    void rejectsNullAndBlankRuntimePropertyNames() {
        for (String name : new String[] {null, "", " ", "\t\n"}) {
            var properties = new LinkedHashMap<String, Object>();
            properties.put(name, 1);

            assertAll(
                    () -> assertThrows(
                            IllegalArgumentException.class,
                            () -> new ItemRuntimeState(
                                    "GoldCoin", properties)),
                    () -> assertThrows(
                            IllegalArgumentException.class,
                            () -> new ActorRuntimeState(
                                    "Guard", properties)));
        }
    }

    @Test
    void rejectsUnsupportedRuntimePropertyValue() {
        for (Object value : new Object[] {
                null, 1L, 1.5, List.of(1)
        }) {
            var properties = new LinkedHashMap<String, Object>();
            properties.put("value", value);

            assertAll(
                    () -> assertThrows(
                            IllegalArgumentException.class,
                            () -> new ItemRuntimeState(
                                    "GoldCoin", properties)),
                    () -> assertThrows(
                            IllegalArgumentException.class,
                            () -> new ActorRuntimeState(
                                    "Guard", properties)));
        }
    }

    @Test
    void rejectsCountInsideRuntimePropertyMap() {
        var properties = new LinkedHashMap<String, Object>();
        properties.put("count", 0);

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new ItemRuntimeState(
                                "GoldCoin", properties)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new ActorRuntimeState(
                                "Guard", properties)));
    }

    @Test
    void doesNotExposeMutableOuterOrInnerMaps() {
        var state = stateFixture();

        assertAll(
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> state.variables().put("hp", 999)),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> state.inventory().put("GoldCoin", 999)),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> state.itemStates().remove("coins")),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> state.actorStates().remove("gateGuard")));

        for (var item : state.itemStates().values()) {
            assertThrows(
                    UnsupportedOperationException.class,
                    () -> item.properties().put("value", 999));
        }

        for (var actor : state.actorStates().values()) {
            assertThrows(
                    UnsupportedOperationException.class,
                    () -> actor.properties().put("hp", 999));
        }

        assertEquals(7, state.variables().get("hp"));
        assertEquals(6, state.inventory().get("GoldCoin"));
        assertEquals(
                1,
                state.itemStates().get("GoldCoin").properties().get("value"));
        assertEquals(
                10,
                state.actorStates().get("Guard").properties().get("hp"));
    }

    @Test
    void allowsZeroAndNegativeIntegerProperties() {
        var variables = variablesFixture();
        variables.put("hp", -5);

        var itemProperties = itemPropertiesFixture();
        itemProperties.put("value", 0);

        var actorProperties = actorPropertiesFixture();
        actorProperties.put("hp", -10);

        var items = itemStatesFixture();
        items.put(
                "GoldCoin",
                new ItemRuntimeState("GoldCoin", itemProperties));

        var actors = actorStatesFixture();
        actors.put(
                "Guard",
                new ActorRuntimeState("Guard", actorProperties));

        var state = createState(
                variables, inventoryFixture(), items, actors);

        assertEquals(-5, state.variables().get("hp"));
        assertEquals(
                0,
                state.itemStates().get("GoldCoin").properties().get("value"));
        assertEquals(
                -10,
                state.actorStates().get("Guard").properties().get("hp"));
    }

    private GameState stateFixture() {
        return createState(
                variablesFixture(),
                inventoryFixture(),
                itemStatesFixture(),
                actorStatesFixture());
    }

    private GameState createState(
            Map<String, Object> variables,
            Map<String, Integer> inventory,
            Map<String, ItemRuntimeState> items,
            Map<String, ActorRuntimeState> actors) {

        return new GameState(
                "entrance", variables, inventory, items, actors, false);
    }

    private LinkedHashMap<String, Object> variablesFixture() {
        var variables = new LinkedHashMap<String, Object>();
        variables.put("hp", 7);
        variables.put("doorOpen", true);
        variables.put("status", "open");
        return variables;
    }

    private LinkedHashMap<String, Integer> inventoryFixture() {
        var inventory = new LinkedHashMap<String, Integer>();
        inventory.put("GoldCoin", 6);
        inventory.put("coins", 3);
        inventory.put("Knife", 1);
        return inventory;
    }

    private LinkedHashMap<String, ItemRuntimeState> itemStatesFixture() {
        var items = new LinkedHashMap<String, ItemRuntimeState>();
        items.put("GoldCoin", itemFixture());
        items.put("coins", itemFixture());
        items.put("Knife", new ItemRuntimeState("Knife", itemPropertiesFixture()));
        return items;
    }

    private LinkedHashMap<String, ActorRuntimeState> actorStatesFixture() {
        var actors = new LinkedHashMap<String, ActorRuntimeState>();
        actors.put("Guard", actorFixture());
        actors.put("gateGuard", actorFixture());
        return actors;
    }

    private ItemRuntimeState itemFixture() {
        return new ItemRuntimeState(
                "GoldCoin", itemPropertiesFixture());
    }

    private ActorRuntimeState actorFixture() {
        return new ActorRuntimeState(
                "Guard", actorPropertiesFixture());
    }

    private LinkedHashMap<String, Object> itemPropertiesFixture() {
        var properties = new LinkedHashMap<String, Object>();
        properties.put("value", 1);
        properties.put("usable", true);
        properties.put("label", "Common");
        return properties;
    }

    private LinkedHashMap<String, Object> actorPropertiesFixture() {
        var properties = new LinkedHashMap<String, Object>();
        properties.put("hp", 10);
        properties.put("hostile", false);
        properties.put("title", "Guard");
        return properties;
    }

    private List<String> keys(Map<String, ?> map) {
        return new ArrayList<>(map.keySet());
    }
}
