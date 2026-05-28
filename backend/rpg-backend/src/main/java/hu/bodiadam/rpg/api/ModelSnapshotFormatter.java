package hu.bodiadam.rpg.api;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ModelSnapshotFormatter {

    public String format(Object value) {
        return formatValue(value, 0);
    }

    private String formatValue(Object value, int indentLevel) {
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

    private String formatRecord(Object record, int indentLevel) {
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

    private Object readComponent(Object record, RecordComponent component) {
        try {
            return component.getAccessor().invoke(record);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Failed to read record component " + component.getName() + " from " + record.getClass().getName(),
                    exception);
        }
    }

    private String indent(int indentLevel) {
        return "  ".repeat(indentLevel);
    }
}
