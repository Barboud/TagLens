package dev.barboud.taglens.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaggingService {

    private final RestClient geminiClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    public String testConnection() {
        Map<String, Object> body = Map.of(
                "contents", new Object[] {
                        Map.of("parts", new Object[] {
                                Map.of("text", "ماهي عاصمة السعودية؟")
                        })
                },
                "generationConfig", Map.of(
                        "thinkingConfig", Map.of(
                                "thinkingLevel", "minimal"
                        )
                )
        );

        return geminiClient.post()
                .uri("/models/gemini-3.6-flash:generateContent?key=" + apiKey)
                .body(body)
                .retrieve()
                .body(String.class);
    }
}