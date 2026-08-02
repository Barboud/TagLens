package dev.barboud.taglens.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.thirdparty.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaggingService {

    private final RestClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String apiKey;

    public String generateTagsRaw(byte[] imageBytes, String contentType) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        String prompt = """
        You are an expert AI image analysis system. Your job is to analyze the provided image and generate a valid JSON object containing tags separated into three distinct categories: "objects", "tags", and "colors".
        ### Guidelines & Rules:
        1. **objects**:
           - List physical, tangible items present in the image (e.g., "tree", "car", "cloud", "sun", "boat").
           - Keep names clear and concise in lowercase English.
        2. **tags**:
           - List non-object descriptors describing the scene, setting, time of day, weather, mood, style, or atmosphere (e.g., "sunset", "tropical", "silhouette", "reflection on water").
        3. **colors**:
           - Identify up to 3 of the most prominent colors in the image.
           - You MUST ONLY select from the 11 basic colors in English: ["black", "blue", "brown", "gray", "green", "orange", "pink", "purple", "red", "white", "yellow"].
        ### Output Requirements:
        - Respond strictly with valid JSON.
        - Do NOT wrap the output in markdown extra text or explanations.
        - Follow this exact schema format:
        {
          "objects": ["string"],
          "tags": ["string"],
          "colors": ["string"]
        }
        """;

        Map<String, Object> body = Map.of(
                "contents", new Object[] {
                        Map.of("parts", new Object[] {
                                Map.of("text", prompt),
                                Map.of("inline_data", Map.of(
                                        "mime_type", contentType,
                                        "data", base64Image
                                ))
                        })
                }
        );

        return geminiClient.post()
                .uri("/models/gemini-3.6-flash:generateContent?key=" + apiKey)
                .body(body)
                .retrieve()
                .body(String.class);
    }

    public JsonNode generateTags(byte[] imageBytes, String contentType) {
        String rawResponse = generateTagsRaw(imageBytes, contentType);

        JsonNode root = objectMapper.readTree(rawResponse);
        String text = root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();

        text = text.replaceAll("```json|```", "").trim();

        return objectMapper.readTree(text);
    }

}