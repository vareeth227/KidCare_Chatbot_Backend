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

    @Value("${claude.model:google/gemini-flash-1.5}")
    private String model;

    @Value("${claude.api.url:https://openrouter.ai/api/v1/chat/completions}")
    private String apiUrl;

    /** "bearer" para OpenRouter/OpenAI  |  "x-api-key" para Anthropic directo */
    @Value("${claude.api.auth-type:bearer}")
    private String authType;

    public String anonimizar(String texto) {
        if (apiKey == null || apiKey.isBlank()) {
            return texto;
        }
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            if ("x-api-key".equalsIgnoreCase(authType)) {
                // Anthropic directo
                headers.set("x-api-key", apiKey);
                headers.set("anthropic-version", "2023-06-01");
            } else {
                // OpenRouter / OpenAI compatible
                headers.set("Authorization", "Bearer " + apiKey);
            }

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
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String resultado = extraerTexto(response.getBody());
                if (resultado != null) return resultado;
            }
        } catch (Exception e) {
            // Fallback: retorna el texto original si la IA falla
        }
        return texto;
    }

    /**
     * Extrae el texto de la respuesta soportando dos formatos:
     * - OpenAI/OpenRouter: choices[0].message.content
     * - Anthropic:         content[0].text
     */
    @SuppressWarnings("unchecked")
    private String extraerTexto(Map<?, ?> responseBody) {
        // Formato OpenAI / OpenRouter
        Object choices = responseBody.get("choices");
        if (choices instanceof List<?> lista && !lista.isEmpty()) {
            Map<?, ?> choice = (Map<?, ?>) lista.get(0);
            Map<?, ?> msg = (Map<?, ?>) choice.get("message");
            if (msg != null && msg.get("content") != null) {
                return msg.get("content").toString();
            }
        }
        // Formato Anthropic
        Object content = responseBody.get("content");
        if (content instanceof List<?> lista && !lista.isEmpty()) {
            Map<?, ?> block = (Map<?, ?>) lista.get(0);
            if (block.get("text") != null) {
                return block.get("text").toString();
            }
        }
        return null;
    }
}
