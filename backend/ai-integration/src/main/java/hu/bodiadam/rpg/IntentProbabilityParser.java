package hu.bodiadam.rpg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IntentProbabilityParser {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private IntentProbabilityParser() {
    }

    public static Map<String, Double> parse(String response, List<IntentCandidate> allowedIntents) {
        Map<String, Double> parsed = readRawMap(response);

        Map<String, Double> normalized = new LinkedHashMap<>();
        for (IntentCandidate intent : allowedIntents) {
            normalized.put(intent.name(), clamp(parsed.getOrDefault(intent.name(), 0.0)));
        }

        return normalized;
    }

    private static double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    private static Map<String, Double> readRawMap(String response) {
        try {
            Map<String, Object> raw = OBJECT_MAPPER.readValue(response, new TypeReference<>() {
            });

            Map<String, Double> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Number number) {
                    result.put(entry.getKey(), number.doubleValue());
                }
            }

            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid model response JSON: " + response, e);
        }
    }
}
