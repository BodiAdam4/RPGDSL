package hu.bodiadam.rpg;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        List<IntentCandidate> intents = List.of(
                new IntentCandidate("talk", "Speak with the guard."),
                new IntentCandidate("bribe", "Offer money for passage."),
                new IntentCandidate("attack", "Start a fight."),
                new IntentCandidate("leave", "Walk away from the scene.")
        );

        String userMessage = "I want to get past the guard, but I don't want to fight. Maybe I can give him some money?";
        String apiKey = LocalSecrets.require("gemini.api.key", "GEMINI_API_KEY");

        GeminiIntentClient client = new GeminiIntentClient(apiKey);
        Map<String, Double> probabilities = client.classify(userMessage, intents);

        System.out.println("=== Parsed probabilities ===");
        probabilities.forEach((intent, probability) -> System.out.println(intent + " -> " + probability));
    }
}
