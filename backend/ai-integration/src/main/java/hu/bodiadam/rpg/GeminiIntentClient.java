package hu.bodiadam.rpg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class GeminiIntentClient {
    private static final String DEFAULT_MODEL = "gemini-2.5-flash-lite";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    public GeminiIntentClient(String apiKey) {
        this(apiKey, System.getenv().getOrDefault("GEMINI_MODEL", DEFAULT_MODEL));
    }

    public GeminiIntentClient(String apiKey, String model) {
        this.httpClient = HttpClient.newHttpClient();
        this.apiKey = apiKey;
        this.model = model;
    }

    public Map<String, Double> classify(String userText, List<IntentCandidate> intents)
            throws IOException, InterruptedException {
        String prompt = IntentPromptBuilder.build(userText, intents);
        String requestBody = buildRequestBody(prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Gemini API request failed with status "
                    + response.statusCode() + ": " + response.body());
        }

        String modelJson = extractStructuredJson(response.body());
        return IntentProbabilityParser.parse(modelJson, intents);
    }

    private static String buildRequestBody(String prompt) throws IOException {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();

        ArrayNode contents = root.putArray("contents");
        ObjectNode userMessage = contents.addObject();
        ArrayNode parts = userMessage.putArray("parts");
        parts.addObject().put("text", prompt);

        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("temperature", 0.1);

        return OBJECT_MAPPER.writeValueAsString(root);
    }

    private static String extractStructuredJson(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");

        if (textNode.isMissingNode() || textNode.isNull() || textNode.asText().isBlank()) {
            throw new IllegalStateException("Gemini response did not contain structured text: " + responseBody);
        }

        return textNode.asText();
    }
}
