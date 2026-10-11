package hu.bodiadam.rpg.dsl.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hu.bodiadam.rpg.dsl.parser.DslParserFacade;
import java.util.List;
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
}