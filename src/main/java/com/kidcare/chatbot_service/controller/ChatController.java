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

    @Value("${claude.model:google/gemini-flash-1.5}")
    private String model;

    @Value("${claude.api.url:https://openrouter.ai/api/v1/chat/completions}")
    private String apiUrl;

    /** "bearer" para OpenRouter/OpenAI  |  "x-api-key" para Anthropic directo */
    @Value("${claude.api.auth-type:bearer}")
    private String authType;

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

            if ("x-api-key".equalsIgnoreCase(authType)) {
                // Anthropic directo
                headers.set("x-api-key", apiKey);
                headers.set("anthropic-version", "2023-06-01");
            } else {
                // OpenRouter / OpenAI compatible
                headers.set("Authorization", "Bearer " + apiKey);
            }

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
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, httpRequest, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String texto = extraerTexto(response.getBody());
                if (texto != null) {
                    resultado.put("preguntas", texto.trim());
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

    /**
     * Extrae el texto de la respuesta soportando dos formatos:
     * - OpenAI/OpenRouter: choices[0].message.content
     * - Anthropic:         content[0].text
     */
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
