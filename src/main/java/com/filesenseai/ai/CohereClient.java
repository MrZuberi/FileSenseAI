package com.filesenseai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class CohereClient {

    private static final String EMBED_URL = "https://api.cohere.com/v2/embed";
    private static final String CHAT_URL = "https://api.cohere.com/v2/chat";
    private static final String EMBED_MODEL = "embed-english-v3.0";
    private static final String CHAT_MODEL = "command-r-plus";
    private static final int BATCH_SIZE = 90;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;

    public CohereClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public List<float[]> embed(List<String> texts, String inputType) {
        List<float[]> results = new ArrayList<>();

        for (int start = 0; start < texts.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, texts.size());
            results.addAll(embedBatch(texts.subList(start, end), inputType));
        }

        return results;
    }

    private List<float[]> embedBatch(List<String> texts, String inputType) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", EMBED_MODEL);
            body.put("input_type", inputType);

            ArrayNode textsArray = body.putArray("texts");
            texts.forEach(textsArray::add);

            ArrayNode embeddingTypes = body.putArray("embedding_types");
            embeddingTypes.add("float");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EMBED_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Cohere embed request failed with status " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode floatEmbeddings = root.path("embeddings").path("float");

            List<float[]> vectors = new ArrayList<>();
            for (JsonNode vectorNode : floatEmbeddings) {
                float[] vector = new float[vectorNode.size()];
                for (int i = 0; i < vectorNode.size(); i++) {
                    vector[i] = (float) vectorNode.get(i).asDouble();
                }
                vectors.add(vector);
            }

            return vectors;
        } catch (Exception exception) {
            throw new RuntimeException("Failed to embed text batch with Cohere", exception);
        }
    }

    public String generateFolderName(String prompt) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", CHAT_MODEL);
            body.put("max_tokens", 20);

            ArrayNode messages = body.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            message.put("content", prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CHAT_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Cohere chat request failed with status " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String rawName = root.path("message").path("content").get(0).path("text").asText();

            return sanitizeFolderName(rawName);
        } catch (Exception exception) {
            return "Miscellaneous";
        }
    }

    private String sanitizeFolderName(String rawName) {
        String cleaned = rawName.trim().replaceAll("[\\r\\n\"'.]+", "");
        cleaned = cleaned.replaceAll("[\\\\/:*?<>|]", "-");

        if (cleaned.length() > 40) {
            cleaned = cleaned.substring(0, 40);
        }

        if (cleaned.isBlank()) {
            cleaned = "Miscellaneous";
        }

        return cleaned;
    }
}