package hu.bodiadam.rpg.dsl.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class DslParserFacadeTest {

    private final DslParserFacade parser = new DslParserFacade();

    @Test
    void doesNotMaterializeSyntheticCountProperty() {
        var definition = parser.parse("""
                items {
                    stackable item GoldCoin {
                        description: "Coin"
                        value: 1
                        label: "Gold"
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

        var item = definition.items().definitions().get(0);

        var propertyNames = item.properties().stream()
                .map(property -> property.name())
                .toList();

        assertEquals(
                List.of("value", "label"),
                propertyNames,
                "Only declared properties should be materialized");
    }
}