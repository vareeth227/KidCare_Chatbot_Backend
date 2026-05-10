package com.kidcare.chatbot_service.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Value("${claude.api.key:}")
    private String apiKey;

    @Value("${claude.model:claude-sonnet-4-6}")
    private String model;

    private static final String CLAUDE_URL = "https://api.anthropic.com/v1/messages";

    @PostMapping("/preguntas")
    public ResponseEntity<Map<String, Object>> generarPreguntas(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String contexto = request.getOrDefault("contexto", "").toString();
        Map<String, Object> resultado = new HashMap<>();

        if (apiKey == null || apiKey.isBlank()) {
            resultado.put("preguntas", List.of(
                "¿Cuáles son los síntomas principales que observas?",
                "¿Desde cuándo presenta estos síntomas?",
                "¿Ha tenido fiebre? ¿De cuánta temperatura?",
                "¿Ha habido cambios en su alimentación o sueño?"
            ));
            resultado.put("modo", "fallback");
            return ResponseEntity.ok(resultado);
        }

        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            String prompt = "Eres un asistente de salud pediátrica. " +
                "Genera entre 4 y 6 preguntas estructuradas en español para ayudar a un cuidador " +
                "a documentar los síntomas actuales del menor. " +
                "Contexto: " + contexto + ". " +
                "Devuelve únicamente un array JSON de preguntas, sin explicaciones.";

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("max_tokens", 512);
            body.put("messages", List.of(message));

            HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(CLAUDE_URL, httpRequest, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<?> content = (List<?>) response.getBody().get("content");
                if (content != null && !content.isEmpty()) {
                    Map<?, ?> firstBlock = (Map<?, ?>) content.get(0);
                    resultado.put("preguntas", firstBlock.get("text").toString().trim());
                    resultado.put("modo", "ia");
                    return ResponseEntity.ok(resultado);
                }
            }
        } catch (Exception e) {
            // fallback
        }

        resultado.put("preguntas", List.of(
            "¿Cuáles son los síntomas principales que observas?",
            "¿Desde cuándo presenta estos síntomas?",
            "¿Ha tenido fiebre?",
            "¿Ha habido cambios en su alimentación o sueño?"
        ));
        resultado.put("modo", "fallback");
        return ResponseEntity.ok(resultado);
    }
}
