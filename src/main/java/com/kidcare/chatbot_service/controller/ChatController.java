package com.kidcare.chatbot_service.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

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
                    try {
                        List<String> preguntas = new ObjectMapper().readValue(
                            texto.trim(), new TypeReference<List<String>>() {});
                        resultado.put("preguntas", preguntas);
                    } catch (Exception parseEx) {
                        resultado.put("preguntas", List.of(texto.trim()));
                    }
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

    @PostMapping("/mensaje")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        String mensaje   = request.getOrDefault("mensaje", "").toString();
        List<String> sintomas = castStringList(request.get("sintomas"));
        List<Map<String, Object>> historial = castHistorial(request.get("historial"));

        Map<String, Object> resultado = new HashMap<>();

        if (apiKey == null || apiKey.isBlank()) {
            resultado.put("respuesta", fallbackPorLongitud(historial.size()));
            return ResponseEntity.ok(resultado);
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            boolean esAnthropic = "x-api-key".equalsIgnoreCase(authType);
            if (esAnthropic) {
                headers.set("x-api-key", apiKey);
                headers.set("anthropic-version", "2023-06-01");
            } else {
                headers.set("Authorization", "Bearer " + apiKey);
            }

            String sistemaPrompt = "Eres un asistente de salud pediátrica de KidCare. " +
                "Tu función es ayudar al cuidador a documentar observaciones de síntomas del menor " +
                "de forma clara y completa para el registro médico. " +
                "Haz preguntas específicas sobre duración, intensidad, frecuencia y factores asociados. " +
                "REGLAS ESTRICTAS: " +
                "1. Nunca emitas diagnósticos ni sugieras enfermedades. " +
                "2. Nunca recomiendes medicamentos ni tratamientos. " +
                "3. Si el cuidador pide un diagnóstico, responde que eso corresponde al médico. " +
                "4. No hagas referencias a datos personales identificables. " +
                "5. Responde siempre en español, con tono empático y preguntas breves. " +
                "Síntomas seleccionados por el cuidador: " +
                (sintomas.isEmpty() ? "no especificados" : String.join(", ", sintomas)) + ".";

            // Construir lista de mensajes a partir del historial
            List<Map<String, Object>> messages = new ArrayList<>();
            for (Map<String, Object> entry : historial) {
                String rol      = entry.getOrDefault("rol", "user").toString();
                String contenido = entry.getOrDefault("contenido", "").toString();
                if (contenido.isBlank()) continue;
                Map<String, Object> msg = new HashMap<>();
                msg.put("role", "assistant".equalsIgnoreCase(rol) ? "assistant" : "user");
                msg.put("content", contenido);
                messages.add(msg);
            }

            // Mensaje actual del usuario
            Map<String, Object> currentMsg = new HashMap<>();
            currentMsg.put("role", "user");
            currentMsg.put("content", mensaje);
            messages.add(currentMsg);

            // Asegurar que el primer mensaje sea "user" (requisito Anthropic)
            if (!messages.isEmpty() && !"user".equals(messages.get(0).get("role"))) {
                messages.remove(0);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("max_tokens", 512);
            body.put("messages", messages);

            if (esAnthropic) {
                // Anthropic: system es parámetro top-level, no va en messages
                body.put("system", sistemaPrompt);
            } else {
                // OpenRouter/OpenAI: system va como primer mensaje
                Map<String, Object> systemMsg = new HashMap<>();
                systemMsg.put("role", "system");
                systemMsg.put("content", sistemaPrompt);
                messages.add(0, systemMsg);
            }

            HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, httpRequest, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String texto = extraerTexto(response.getBody());
                if (texto != null && !texto.isBlank()) {
                    resultado.put("respuesta", texto.trim());
                    return ResponseEntity.ok(resultado);
                }
            }
        } catch (Exception e) {
            // fallback
        }

        resultado.put("respuesta", fallbackPorLongitud(historial.size()));
        return ResponseEntity.ok(resultado);
    }

    private String fallbackPorLongitud(int numMensajes) {
        if (numMensajes < 2) return "¿Desde cuándo nota estos síntomas y con qué frecuencia ocurren?";
        if (numMensajes < 4) return "¿Ha notado cambios en el apetito, el sueño o el comportamiento del menor?";
        if (numMensajes < 6) return "¿Hay algún factor que mejore o empeore los síntomas?";
        return "¿Desea agregar algún detalle adicional para el registro médico?";
    }

    @SuppressWarnings("unchecked")
    private List<String> castStringList(Object obj) {
        if (obj instanceof List<?> lista) {
            return lista.stream().map(Object::toString).collect(Collectors.toList());
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castHistorial(Object obj) {
        if (obj instanceof List<?> lista) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : lista) {
                if (item instanceof Map<?, ?> m) result.add((Map<String, Object>) m);
            }
            return result;
        }
        return List.of();
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
