package hu.bodiadam.rpg.dsl.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import hu.bodiadam.rpg.dsl.model.GameDefinition;
import java.lang.reflect.RecordComponent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class DslParserFacadeSnapshotTest {

    private static final Path EXAMPLES_DIRECTORY = Path.of("src/test/resources/examples");
    private static final Path SNAPSHOT_DIRECTORY = Path.of("target/generated-test-snapshots/examples");

    private final DslParserFacade parser = new DslParserFacade();

    @Test
    void parsesAllExamplesAndWritesEachModelToSeparateSnapshotFile() throws IOException {
        Files.createDirectories(SNAPSHOT_DIRECTORY);

        List<Path> exampleFiles;
        try (Stream<Path> paths = Files.list(EXAMPLES_DIRECTORY)) {
            exampleFiles = paths
                    .filter(path -> path.getFileName().toString().endsWith(".dsl"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }

        assertFalse(exampleFiles.isEmpty(), "No DSL example files were found in " + EXAMPLES_DIRECTORY);

        for (Path exampleFile : exampleFiles) {
            GameDefinition definition = parser.parse(exampleFile);
            String snapshotContent = formatValue(definition, 0);

            Path snapshotFile = SNAPSHOT_DIRECTORY.resolve(snapshotFileNameFor(exampleFile));
            Files.createDirectories(snapshotFile.getParent());
            Files.writeString(snapshotFile, snapshotContent);

            assertEquals(snapshotContent, Files.readString(snapshotFile),
                    "Snapshot content mismatch for " + exampleFile.getFileName());
        }
    }

    private static String snapshotFileNameFor(Path exampleFile) {
        String fileName = exampleFile.getFileName().toString();
        int extensionStart = fileName.lastIndexOf('.');
        String baseName = extensionStart >= 0 ? fileName.substring(0, extensionStart) : fileName;
        return baseName + ".txt";
    }

    private static String formatValue(Object value, int indentLevel) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return "\"" + stringValue + "\"";
        }
        if (value instanceof Optional<?> optionalValue) {
            return optionalValue
                    .map(inner -> "Optional[\n"
                            + indent(indentLevel + 1) + formatValue(inner, indentLevel + 1) + "\n"
                            + indent(indentLevel) + "]")
                    .orElse("Optional.empty");
        }
        if (value instanceof List<?> listValue) {
            if (listValue.isEmpty()) {
                return "[]";
            }

            StringBuilder builder = new StringBuilder("[\n");
            for (int index = 0; index < listValue.size(); index++) {
                builder.append(indent(indentLevel + 1))
                        .append(formatValue(listValue.get(index), indentLevel + 1));
                if (index < listValue.size() - 1) {
                    builder.append(",");
                }
                builder.append("\n");
            }
            builder.append(indent(indentLevel)).append("]");
            return builder.toString();
        }
        if (value.getClass().isRecord()) {
            return formatRecord(value, indentLevel);
        }
        return String.valueOf(value);
    }

    private static String formatRecord(Object record, int indentLevel) {
        RecordComponent[] components = record.getClass().getRecordComponents();
        if (components.length == 0) {
            return record.getClass().getSimpleName() + "()";
        }

        StringBuilder builder = new StringBuilder(record.getClass().getSimpleName()).append("(\n");
        for (int index = 0; index < components.length; index++) {
            RecordComponent component = components[index];
            Object componentValue = readComponent(record, component);
            builder.append(indent(indentLevel + 1))
                    .append(component.getName())
                    .append("=")
                    .append(formatValue(componentValue, indentLevel + 1));
            if (index < components.length - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append(indent(indentLevel)).append(")");
        return builder.toString();
    }

    private static Object readComponent(Object record, RecordComponent component) {
        try {
            return component.getAccessor().invoke(record);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Failed to read record component " + component.getName() + " from " + record.getClass().getName(),
                    exception);
        }
    }

    private static String indent(int indentLevel) {
        return "  ".repeat(indentLevel);
    }
}
