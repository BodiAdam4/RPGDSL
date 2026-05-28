package hu.bodiadam.rpg;

import java.util.List;
import java.util.stream.Collectors;

public final class IntentPromptBuilder {
    private IntentPromptBuilder() {
    }

    public static String build(String userText, List<IntentCandidate> intents) {
        String intentList = intents.stream()
                .map(intent -> "- " + intent.name() + ": " + intent.description())
                .collect(Collectors.joining("\n"));

        return """
                You are an intent classifier for an RPG game.
                Estimate how likely it is that the player wants each allowed intent.
                Return only one JSON object.
                Use the exact intent names as keys.
                Use numbers between 0.0 and 1.0 as values.
                Include every allowed intent exactly once.
                The probabilities do not need to sum to 1.0.
                Do not return markdown, explanation, or any text outside the JSON object.

                Allowed intents:
                %s

                Player message:
                "%s"
                """.formatted(intentList, escape(userText));
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
