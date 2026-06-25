package com.kidcare.chatbot_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidcare.chatbot_service.dto.InteraccionRequestDTO;
import com.kidcare.chatbot_service.dto.InteraccionResponseDTO;
import com.kidcare.chatbot_service.security.JwtUtil;
import com.kidcare.chatbot_service.service.InteraccionService;
import com.kidcare.chatbot_service.security.JwtFilter;
import com.kidcare.chatbot_service.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfig.class, JwtFilter.class})
@WebMvcTest(InteraccionController.class)
class InteraccionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InteraccionService interaccionService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ─── POST /api/interacciones ──────────────────────────────────────────────

    @Test
    @WithMockUser(username = "tutor@test.com", roles = {"TUTOR"})
    void registrar_ConDatosValidos_Retorna200Ok() throws Exception {
        InteraccionRequestDTO requestDTO = new InteraccionRequestDTO();
        requestDTO.setIdMenor(1);
        requestDTO.setObservaciones("Menor con fiebre");
        requestDTO.setOrigen("CHATBOT");
        requestDTO.setFallback(false);

        InteraccionResponseDTO responseDTO = new InteraccionResponseDTO();
        responseDTO.setId("int-123");
        responseDTO.setIdMenor(1);
        responseDTO.setObservaciones("Menor con fiebre");
        responseDTO.setOrigen("CHATBOT");
        responseDTO.setFallback(false);
        responseDTO.setEditado(false);
        responseDTO.setFecha(LocalDate.now());

        when(interaccionService.registrar(any(InteraccionRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/interacciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("int-123"))
                .andExpect(jsonPath("$.idMenor").value(1))
                .andExpect(jsonPath("$.observaciones").value("Menor con fiebre"))
                .andExpect(jsonPath("$.origen").value("CHATBOT"))
                .andExpect(jsonPath("$.fallback").value(false));

        verify(interaccionService).registrar(any(InteraccionRequestDTO.class));
    }

    @Test
    @WithMockUser(username = "tutor@test.com", roles = {"TUTOR"})
    void registrar_ConDatosInvalidos_Retorna400BadRequest() throws Exception {
        InteraccionRequestDTO requestDTO = new InteraccionRequestDTO();
        // Faltan idMenor, observaciones y origen, que son obligatorios

        mockMvc.perform(post("/api/interacciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registrar_SinAutenticacion_Retorna403o401() throws Exception {
        InteraccionRequestDTO requestDTO = new InteraccionRequestDTO();
        requestDTO.setIdMenor(1);
        requestDTO.setObservaciones("Menor con fiebre");
        requestDTO.setOrigen("CHATBOT");

        mockMvc.perform(post("/api/interacciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden()); // O 401 dependiendo de la config de Spring Security
    }

    // ─── GET /api/interacciones/menor/{idMenor} ─────────────────────────────────

    @Test
    @WithMockUser(username = "tutor@test.com", roles = {"TUTOR"})
    void listarPorMenor_RetornaListaInteracciones() throws Exception {
        InteraccionResponseDTO r1 = new InteraccionResponseDTO();
        r1.setId("int-1");
        r1.setIdMenor(1);
        r1.setObservaciones("Fiebre de 38 grados");
        r1.setOrigen("CHATBOT");

        when(interaccionService.obtenerPorMenor(1)).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/interacciones/menor/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("int-1"))
                .andExpect(jsonPath("$[0].observaciones").value("Fiebre de 38 grados"));

        verify(interaccionService).obtenerPorMenor(1);
    }

    // ─── PUT /api/interacciones/{id} ───────────────────────────────────────────

    @Test
    @WithMockUser(username = "tutor@test.com", roles = {"TUTOR"})
    void editar_ConDatosValidos_Retorna200Ok() throws Exception {
        InteraccionRequestDTO requestDTO = new InteraccionRequestDTO();
        requestDTO.setIdMenor(1);
        requestDTO.setObservaciones("Observación modificada");
        requestDTO.setOrigen("CHATBOT");

        InteraccionResponseDTO responseDTO = new InteraccionResponseDTO();
        responseDTO.setId("int-123");
        responseDTO.setIdMenor(1);
        responseDTO.setObservaciones("Observación modificada");
        responseDTO.setOrigen("CHATBOT");
        responseDTO.setEditado(true);

        when(interaccionService.editar(eq("int-123"), any(InteraccionRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/api/interacciones/int-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.observaciones").value("Observación modificada"))
                .andExpect(jsonPath("$.editado").value(true));

        verify(interaccionService).editar(eq("int-123"), any(InteraccionRequestDTO.class));
    }

    // ─── DELETE /api/interacciones/{id} ────────────────────────────────────────

    @Test
    @WithMockUser(username = "tutor@test.com", roles = {"TUTOR"})
    void eliminar_RetornaMensajeExito() throws Exception {
        mockMvc.perform(delete("/api/interacciones/int-123"))
                .andExpect(status().isOk())
                .andExpect(content().string("Interacción eliminada correctamente"));

        verify(interaccionService).eliminar("int-123");
    }

    // ─── GET /api/interacciones/interno/menor/{idMenor} ─────────────────────────

    @Test
    void listarInterno_SinAutenticacion_Retorna200Ok() throws Exception {
        InteraccionResponseDTO r1 = new InteraccionResponseDTO();
        r1.setId("int-1");
        r1.setIdMenor(1);
        r1.setObservaciones("Endpoint interno");

        when(interaccionService.obtenerPorMenorFiltrado(eq(1), any())).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/interacciones/interno/menor/1?ids=int-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("int-1"))
                .andExpect(jsonPath("$[0].observaciones").value("Endpoint interno"));

        verify(interaccionService).obtenerPorMenorFiltrado(eq(1), any());
    }
}
