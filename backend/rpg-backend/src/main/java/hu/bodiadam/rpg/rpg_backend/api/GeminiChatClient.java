package hu.bodiadam.rpg.rpg_backend.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeminiChatClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String apiKey;
    private final String model;

    public GeminiChatClient(
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public String generate(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is missing. Set GEMINI_API_KEY before starting a game.");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(prompt)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Gemini request failed with status " + response.statusCode() + ": " + response.body());
            }

            return extractText(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Gemini API call was interrupted.", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to call Gemini API.", exception);
        }
    }

    private String buildRequestBody(String prompt) throws Exception {
        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        ObjectNode content = body.putArray("contents").addObject();
        content.putArray("parts").addObject().put("text", prompt);

        ObjectNode generationConfig = body.putObject("generationConfig");
        generationConfig.put("temperature", 0.3d);
        generationConfig.put("responseMimeType", "text/plain");

        return OBJECT_MAPPER.writeValueAsString(body);
    }

    private String extractText(String responseBody) throws Exception {
        JsonNode textNode = OBJECT_MAPPER.readTree(responseBody)
                .path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text");

        if (textNode.isMissingNode() || textNode.isNull() || textNode.asText().isBlank()) {
            throw new IllegalStateException("Gemini response did not contain text: " + responseBody);
        }

        return textNode.asText();
    }
}
