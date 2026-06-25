package com.kidcare.chatbot_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidcare.chatbot_service.security.JwtFilter;
import com.kidcare.chatbot_service.security.JwtUtil;
import com.kidcare.chatbot_service.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, JwtFilter.class})
@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ─── POST /api/chat/preguntas ─────────────────────────────────────────────

    @Test
    @WithMockUser(username = "tutor@test.com", roles = {"TUTOR"})
    void generarPreguntas_SinApiKey_RetornaFallback() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contexto", "Tos con flema y estornudos");

        mockMvc.perform(post("/api/chat/preguntas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preguntas").isArray())
                .andExpect(jsonPath("$.preguntas[0]").value("¿Cuáles son los síntomas principales que observas?"))
                .andExpect(jsonPath("$.modo").value("fallback"));
    }

    @Test
    void generarPreguntas_SinAutenticacion_Retorna403o401() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contexto", "Fiebre alta");

        mockMvc.perform(post("/api/chat/preguntas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden());
    }

    // ─── POST /api/chat/mensaje ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "tutor@test.com", roles = {"TUTOR"})
    void chat_SinApiKey_RetornaRespuestaFallback() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("mensaje", "Tiene tos");
        requestBody.put("sintomas", List.of("TOS", "FIEBRE"));
        requestBody.put("historial", List.of(
                Map.of("rol", "user", "contenido", "Hola chatbot"),
                Map.of("rol", "assistant", "contenido", "¿En qué puedo ayudarte?")
        ));

        mockMvc.perform(post("/api/chat/mensaje")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").value("¿Ha notado cambios en el apetito, el sueño o el comportamiento del menor?"));
    }

    @Test
    void chat_SinAutenticacion_Retorna403o401() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("mensaje", "Hola");

        mockMvc.perform(post("/api/chat/mensaje")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden());
    }
}
