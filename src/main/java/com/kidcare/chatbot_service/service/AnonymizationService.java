package com.kidcare.chatbot_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AnonymizationService {

    @Value("${claude.api.key:}")
    private String apiKey;

    @Value("${claude.model:claude-sonnet-4-6}")
    private String model;

    private static final String CLAUDE_URL = "https://api.anthropic.com/v1/messages";

    public String anonimizar(String texto) {
        if (apiKey == null || apiKey.isBlank()) {
            return texto;
        }
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            String prompt = "Anonimiza el siguiente texto de observación pediátrica. " +
                "Elimina toda información personal identificable (nombres, números de identificación, " +
                "direcciones, teléfonos, ubicaciones específicas). " +
                "Conserva todas las observaciones médicas y de comportamiento. " +
                "Devuelve únicamente el texto anonimizado sin explicaciones.\n\n" + texto;

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("max_tokens", 1024);
            body.put("messages", List.of(message));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(CLAUDE_URL, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<?> content = (List<?>) response.getBody().get("content");
                if (content != null && !content.isEmpty()) {
                    Map<?, ?> firstBlock = (Map<?, ?>) content.get(0);
                    Object textValue = firstBlock.get("text");
                    if (textValue != null) {
                        return textValue.toString();
                    }
                }
            }
        } catch (Exception e) {
            // Fallback: retorna el texto original si Claude falla
        }
        return texto;
    }
}
