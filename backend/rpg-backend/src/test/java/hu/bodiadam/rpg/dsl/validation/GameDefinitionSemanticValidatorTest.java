package hu.bodiadam.rpg.dsl.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hu.bodiadam.rpg.dsl.parser.DslParserFacade;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class GameDefinitionSemanticValidatorTest {
    private final DslParserFacade parser = new DslParserFacade();
    private final GameDefinitionSemanticValidator validator =
            new GameDefinitionSemanticValidator();

    @Test
    void acceptsCountOnStackableItemDefinition() {
        var diagnostics = validate("""
                items {
                    stackable item GoldCoin {
                        description: "Coin"
                    }
                }
                vars { amount: 0 }
                start {
                    add GoldCoin.count to amount
                }
                """);

        assertTrue(diagnostics.isEmpty(), diagnostics.toString());
    }

    @Test
    void acceptsCountOnTopLevelStackableItemInstance() {
        var diagnostics = validate("""
                items {
                    stackable item GoldCoin {
                        description: "Coin"
                    }
                    coins as GoldCoin
                }
                vars { amount: 0 }
                start {
                    add coins.count to amount
                }
                """);

        assertTrue(diagnostics.isEmpty(), diagnostics.toString());
    }

    @Test
    void acceptsCountOnStartStackableItemInstance() {
        var diagnostics = validate("""
                items {
                    stackable item GoldCoin {
                        description: "Coin"
                    }
                }
                vars { amount: 0 }
                start {
                    coins as GoldCoin
                    add coins.count to amount
                }
                """);

        assertTrue(diagnostics.isEmpty(), diagnostics.toString());
    }

    @Test
    void rejectsUnknownPropertyOnNonStackableItem() {
        var diagnostics = validate("""
                items {
                    item Knife {
                        description: "Knife"
                    }
                }
                vars { amount: 0 }
                start {
                    add Knife.nonexistent to amount
                }
                """);

        assertCodes(diagnostics, "UNKNOWN_PROPERTY");
    }

    @Test
    void rejectsImplicitCountOnNonStackableItem() {
        var diagnostics = validate("""
                items {
                    item Knife {
                        description: "Knife"
                    }
                }
                vars { amount: 0 }
                start {
                    add Knife.count to amount
                }
                """);

        assertCodes(diagnostics, "UNKNOWN_PROPERTY");
    }

    @Test
    void rejectsImplicitCountOnActorDefinitionAndInstance() {
        var diagnostics = validate("""
                actors {
                    actor Guard {
                        description: "Guard"
                    }
                }
                vars { amount: 0 }
                start {
                    captain as Guard
                    add Guard.count to amount
                    add captain.count to amount
                }
                """);

        assertCodes(
                diagnostics,
                "UNKNOWN_PROPERTY",
                "UNKNOWN_PROPERTY");
    }

    @Test
    void rejectsNonStackableChildOfStackableParent() {
        var diagnostics = validate("""
                items {
                    stackable item Coin {
                        description: "Base coin"
                    }
                    item GoldCoin is Coin {
                        description: "Gold coin"
                    }
                }
                """);

        assertCodes(diagnostics, "INVALID_STACKABLE_INHERITANCE");

        var diagnostic = diagnostics.get(0);
        assertEquals(DiagnosticSeverity.ERROR, diagnostic.severity());
        assertTrue(diagnostic.line() > 0);
        assertTrue(diagnostic.character() > 0);
    }

    @Test
    void acceptsStackableChildOfStackableParent() {
        var diagnostics = validate("""
                items {
                    stackable item Coin {
                        description: "Base coin"
                    }
                    stackable item GoldCoin is Coin {
                        description: "Gold coin"
                    }
                }
                vars { amount: 0 }
                start {
                    add GoldCoin.count to amount
                }
                """);

        assertTrue(diagnostics.isEmpty(), diagnostics.toString());
    }

    @Test
    void acceptsStackableChildOfNonStackableParent() {
        var diagnostics = validate("""
                items {
                    item Item {
                        description: "Base item"
                    }
                    stackable item GoldCoin is Item {
                        description: "Gold coin"
                    }
                }
                vars { amount: 0 }
                start {
                    add GoldCoin.count to amount
                }
                """);

        assertTrue(diagnostics.isEmpty(), diagnostics.toString());
    }

    @Test
    void acceptsNonStackableChildOfNonStackableParent() {
        var diagnostics = validate("""
                items {
                    item Weapon {
                        description: "Base weapon"
                    }
                    item Knife is Weapon {
                        description: "Knife"
                    }
                }
                """);

        assertTrue(diagnostics.isEmpty(), diagnostics.toString());
    }

    @Test
    void rejectsStackabilityLossInMultiLevelInheritance() {
        var diagnostics = validate("""
                items {
                    stackable item Coin {
                        description: "Base coin"
                    }
                    stackable item GoldCoin is Coin {
                        description: "Gold coin"
                    }
                    item AncientCoin is GoldCoin {
                        description: "Ancient coin"
                    }
                }
                """);

        assertCodes(diagnostics, "INVALID_STACKABLE_INHERITANCE");
    }

    @Test
    void reportsUnknownParentWithoutStackabilityError() {
        var diagnostics = validate("""
                items {
                    item GoldCoin is MissingParent {
                        description: "Gold coin"
                    }
                }
                """);

        assertCodes(diagnostics, "UNKNOWN_ITEM_PARENT");
    }

    private List<Diagnostic> validate(String declarations) {
        var source = declarations + """
                
                world {
                    scenes {
                        scene entrance {
                            description: "Entrance"
                        }
                    }
                }
                """;

        var program = parser.parseTree(source);
        var definition = parser.build(program);

        return validator.validate(program, definition).diagnostics();
    }

    private void assertCodes(
            List<Diagnostic> diagnostics,
            String... expectedCodes) {

        var actualCodes = diagnostics.stream()
                .map(Diagnostic::code)
                .sorted()
                .toList();

        var expected = List.of(expectedCodes).stream()
                .sorted()
                .toList();

        assertEquals(expected, actualCodes, diagnostics.toString());
    }

    @Test
    void rejectsDeclaredCountOnItem() {
        var source = """
            items {
                stackable item GoldCoin {
                    description: "Coin"
                    count: 5
                }
            }
            world {
                scenes {
                    scene entrance {
                        description: "Entrance"
                    }
                }
            }
            """;

        var parser = new DslParserFacade();
        var definition = parser.parse(source);
        var program = parser.parseTree(source);

        var validator = new GameDefinitionSemanticValidator();
        var result = validator.validate(program, definition);

        assertTrue(
                result.diagnostics().stream()
                        .anyMatch(diagnostic ->
                                diagnostic.code().equals("RESERVED_PROPERTY_NAME")),
                "Declaring the reserved 'count' property must be rejected");
    }

    @Test
    void rejectsDeclaredCountOnActor() {
        var source = """
            actors {
                actor Guard {
                    description: "Guard"
                    count: 5
                }
            }
            world {
                scenes {
                    scene entrance {
                        description: "Entrance"
                    }
                }
            }
            """;

        var parser = new DslParserFacade();
        var definition = parser.parse(source);
        var program = parser.parseTree(source);

        var validator = new GameDefinitionSemanticValidator();
        var result = validator.validate(program, definition);

        assertTrue(
                result.diagnostics().stream()
                        .anyMatch(diagnostic ->
                                diagnostic.code().equals("RESERVED_PROPERTY_NAME")),
                "Declaring 'count' on an actor must be rejected");
    }
    @Test
    void acceptsVirtualCountOnStackableDefinition() {
        var source = """
            items {
                stackable item GoldCoin {
                    description: "Coin"
                }
            }
            rules {
                rule checkCoins {
                    requires {
                        GoldCoin.count is at least 1
                    }
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
            """;

        var parser = new DslParserFacade();
        var definition = parser.parse(source);
        var program = parser.parseTree(source);

        var validator = new GameDefinitionSemanticValidator();
        var result = validator.validate(program, definition);

        assertTrue(
                result.diagnostics().isEmpty(),
                "Virtual count on a stackable item definition must be accepted: "
                        + result.diagnostics());
    }
    @Test
    void acceptsVirtualCountOnStackableNamedInstance() {
        var source = """
            items {
                stackable item GoldCoin {
                    description: "Coin"
                }
                coins as GoldCoin
            }
            rules {
                rule checkCoins {
                    requires {
                        coins.count is at least 1
                    }
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
            """;

        var parser = new DslParserFacade();
        var definition = parser.parse(source);
        var program = parser.parseTree(source);

        var validator = new GameDefinitionSemanticValidator();
        var result = validator.validate(program, definition);

        assertTrue(
                result.diagnostics().isEmpty(),
                "Virtual count on a named stackable item instance must be accepted: "
                        + result.diagnostics());
    }
    @Test
    void rejectsCountOnNonStackableItem() {
        var source = """
            items {
                item Knife {
                    description: "Knife"
                    damage: 3
                }
            }
            rules {
                rule checkKnife {
                    requires {
                        Knife.count is at least 1
                    }
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
            """;

        var parser = new DslParserFacade();
        var definition = parser.parse(source);
        var program = parser.parseTree(source);

        var validator = new GameDefinitionSemanticValidator();
        var result = validator.validate(program, definition);

        assertTrue(
                result.diagnostics().stream()
                        .anyMatch(diagnostic ->
                                diagnostic.code().equals("UNKNOWN_PROPERTY")),
                "Count on a non-stackable item must be rejected: "
                        + result.diagnostics());
    }
    @Test
    void rejectsCountOnActor() {
        var source = """
            actors {
                actor Guard {
                    description: "Guard"
                    hp: 10
                }
            }
            rules {
                rule checkGuard {
                    requires {
                        Guard.count is at least 1
                    }
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
            """;

        var parser = new DslParserFacade();
        var definition = parser.parse(source);
        var program = parser.parseTree(source);

        var validator = new GameDefinitionSemanticValidator();
        var result = validator.validate(program, definition);

        assertTrue(
                result.diagnostics().stream()
                        .anyMatch(diagnostic ->
                                diagnostic.code().equals("UNKNOWN_PROPERTY")),
                "Count on an actor must be rejected: "
                        + result.diagnostics());
    }
    @Test
    void rejectsCountAsNumericWriteTarget() {
        for (String action : new String[] {
                "add 1 to GoldCoin.count",
                "subtract 1 from GoldCoin.count",
                "multiply GoldCoin.count by 2",
                "divide GoldCoin.count by 2"
        }) {
            var source = """
                items {
                    stackable item GoldCoin {
                        description: "Coin"
                    }
                }
                start {
                    %s
                }
                world {
                    scenes {
                        scene entrance {
                            description: "Entrance"
                        }
                    }
                }
                """.formatted(action);

            var parser = new DslParserFacade();
            var definition = parser.parse(source);
            var program = parser.parseTree(source);

            var validator = new GameDefinitionSemanticValidator();
            var result = validator.validate(program, definition);

            assertTrue(
                    result.diagnostics().stream()
                            .anyMatch(diagnostic ->
                                    diagnostic.code().equals(
                                            "INVALID_WRITE_TARGET")),
                    "Virtual count must not be writable: "
                            + action + " -> " + result.diagnostics());
        }
    }

    @Test
    void rejectsNameCollisionAcrossGlobalRootCategories() {
        for (String category : List.of("item", "actor")) {
            var diagnostics = validate("""
                vars {
                    hp: 7
                }
                %s {
                    %s hp {
                        description: "Conflicting definition"
                        value: 9
                    }
                }
                """.formatted(
                    category.equals("item") ? "items" : "actors",
                    category));

            assertCodes(diagnostics, "ROOT_NAME_COLLISION");

            var diagnostic = diagnostics.get(0);

            assertEquals(
                    DiagnosticSeverity.ERROR,
                    diagnostic.severity());

            assertTrue(
                    diagnostic.message().contains("hp"),
                    diagnostic.message());

            assertTrue(
                    diagnostic.message().contains("SCALAR"),
                    diagnostic.message());

            String expectedCategory = category.equals("item")
                    ? "ITEM_DEFINITION"
                    : "ACTOR_DEFINITION";

            assertTrue(
                    diagnostic.message().contains(expectedCategory),
                    diagnostic.message());

            assertTrue(diagnostic.line() > 0);
            assertTrue(diagnostic.character() > 0);
        }
    }

    @Test
    void rejectsCollisionBetweenTopLevelAndStartInstances() {
        var diagnostics = validate("""
            items {
                item Knife {
                    description: "Knife"
                    damage: 3
                }
                shared as Knife
            }
            actors {
                actor Guard {
                    description: "Guard"
                    hp: 10
                }
            }
            start {
                shared as Guard
            }
            """);

        assertCodes(diagnostics, "ROOT_NAME_COLLISION");

        var diagnostic = diagnostics.get(0);

        assertEquals(
                DiagnosticSeverity.ERROR,
                diagnostic.severity());

        assertTrue(
                diagnostic.message().contains("shared"),
                diagnostic.message());

        assertTrue(
                diagnostic.message().contains("ITEM_INSTANCE"),
                diagnostic.message());

        assertTrue(
                diagnostic.message().contains("ACTOR_INSTANCE"),
                diagnostic.message());

        assertTrue(diagnostic.line() > 0);
        assertTrue(diagnostic.character() > 0);
    }

    @Test
    void acceptsGlobalVariableAsNumericWriteTarget() {
        var diagnostics = validate("""
            vars {
                hp: 10
            }
            start {
                add 2 to hp
                subtract 1 from hp
                multiply hp by 3
                divide hp by 2
            }
            """);

        assertTrue(
                diagnostics.isEmpty(),
                diagnostics.toString());
    }

    @Test
    void acceptsItemAndActorPropertiesAsNumericWriteTargets() {
        for (String target : List.of(
                "Knife.damage",
                "blade.damage",
                "Guard.hp",
                "captain.hp")) {

            var diagnostics = validate("""
                items {
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
                start {
                    add 2 to %s
                    subtract 1 from %s
                    multiply %s by 3
                    divide %s by 2
                }
                """.formatted(target, target, target, target));

            assertTrue(
                    diagnostics.isEmpty(),
                    target + " -> " + diagnostics);
        }
    }

    @Test
    void rejectsRuleParameterAsNumericWriteTarget() {
        for (String action : List.of(
                "add 1 to amount",
                "subtract 1 from amount",
                "multiply amount by 2",
                "divide amount by 2")) {

            var diagnostics = validate("""
                rules {
                    rule change(amount) {
                        success {
                            %s
                        }
                        fail {}
                    }
                }
                """.formatted(action));

            assertCodes(diagnostics, "INVALID_WRITE_TARGET");

            assertTrue(
                    diagnostics.get(0).message().contains("amount"),
                    action + " -> " + diagnostics);
        }
    }

    @Test
    void rejectsStateTargetShadowedByRuleParameter() {
        for (String target : List.of(
                "hp",
                "Knife.damage",
                "blade.damage",
                "Guard.hp",
                "captain.hp")) {

            String root = target.contains(".")
                    ? target.substring(0, target.indexOf('.'))
                    : target;

            for (String action : List.of(
                    "add 1 to " + target,
                    "subtract 1 from " + target,
                    "multiply " + target + " by 2",
                    "divide " + target + " by 2")) {

                var diagnostics = validate("""
                    vars { hp: 10 }
                    items {
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
                    rules {
                        rule change(%s) {
                            success {
                                %s
                            }
                            fail {}
                        }
                    }
                    """.formatted(root, action));

                assertCodes(diagnostics, "INVALID_WRITE_TARGET");

                assertTrue(
                        diagnostics.get(0).message().contains(root),
                        action + " -> " + diagnostics);
            }
        }
    }

    @Test
    void rejectsUnknownNumericWriteTarget() {
        var targets = Map.of(
                "missing", "UNKNOWN_VALUE_REFERENCE",
                "Missing.damage", "UNKNOWN_PROPERTY_ROOT",
                "Knife.missing", "UNKNOWN_PROPERTY",
                "Guard.missing", "UNKNOWN_PROPERTY");

        for (var entry : targets.entrySet()) {
            String target = entry.getKey();

            for (String action : List.of(
                    "add 1 to " + target,
                    "subtract 1 from " + target,
                    "multiply " + target + " by 2",
                    "divide " + target + " by 2")) {

                var diagnostics = validate("""
                    items {
                        item Knife {
                            description: "Knife"
                            damage: 3
                        }
                    }
                    actors {
                        actor Guard {
                            description: "Guard"
                            hp: 10
                        }
                    }
                    start {
                        %s
                    }
                    """.formatted(action));

                assertCodes(diagnostics, entry.getValue());

                String root = target.contains(".")
                        ? target.substring(0, target.indexOf('.'))
                        : target;

                assertTrue(
                        diagnostics.get(0).message().contains(root),
                        action + " -> " + diagnostics);

                if (target.contains(".")) {
                    String property =
                            target.substring(target.indexOf('.') + 1);

                    if (entry.getValue().equals("UNKNOWN_PROPERTY")) {
                        assertTrue(
                                diagnostics.get(0).message()
                                        .contains(property),
                                action + " -> " + diagnostics);
                    }
                }
            }
        }
    }

    @Test
    void rejectsWholeObjectAsNumericWriteTarget() {
        for (String target : List.of(
                "Knife", "blade", "Guard", "captain")) {

            for (String action : List.of(
                    "add 1 to " + target,
                    "subtract 1 from " + target,
                    "multiply " + target + " by 2",
                    "divide " + target + " by 2")) {

                var diagnostics = validate("""
                    items {
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
                    start {
                        %s
                    }
                    """.formatted(action));

                assertCodes(diagnostics, "INVALID_WRITE_TARGET");

                assertTrue(
                        diagnostics.get(0).message().contains(target),
                        action + " -> " + diagnostics);
            }
        }
    }

    @Test
    void rejectsCollisionBetweenDefinitionAndNamedInstance() {
        var diagnostics = validate("""
            items {
                item Knife {
                    description: "Knife"
                    damage: 3
                }
            }
            actors {
                actor Guard {
                    description: "Guard"
                    hp: 10
                }
                Knife as Guard
            }
            """);

        assertCodes(diagnostics, "ROOT_NAME_COLLISION");

        var diagnostic = diagnostics.get(0);

        assertTrue(
                diagnostic.message().contains("Knife"),
                diagnostic.message());
        assertTrue(
                diagnostic.message().contains("ITEM_DEFINITION"),
                diagnostic.message());
        assertTrue(
                diagnostic.message().contains("ACTOR_INSTANCE"),
                diagnostic.message());
    }

    @Test
    void rejectsCollisionBetweenItemAndActorInstances() {
        var diagnostics = validate("""
            items {
                item Knife {
                    description: "Knife"
                    damage: 3
                }
                shared as Knife
            }
            actors {
                actor Guard {
                    description: "Guard"
                    hp: 10
                }
                shared as Guard
            }
            """);

        assertCodes(diagnostics, "ROOT_NAME_COLLISION");

        var diagnostic = diagnostics.get(0);

        assertTrue(
                diagnostic.message().contains("shared"),
                diagnostic.message());
        assertTrue(
                diagnostic.message().contains("ITEM_INSTANCE"),
                diagnostic.message());
        assertTrue(
                diagnostic.message().contains("ACTOR_INSTANCE"),
                diagnostic.message());
    }
}