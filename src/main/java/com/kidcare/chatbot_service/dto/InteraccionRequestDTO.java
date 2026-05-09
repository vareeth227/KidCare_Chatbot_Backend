package com.kidcare.chatbot_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// DTO que recibe los datos para registrar una interacción
@Data
public class InteraccionRequestDTO {

    // ID del menor al que pertenece la interacción
    @NotNull(message = "El id del menor es obligatorio")
    private Integer idMenor;

    // Contenido anonimizado de la interacción
    @NotBlank(message = "Las observaciones son obligatorias")
    private String observaciones;

    // Origen del registro: web o móvil
    @NotBlank(message = "El origen es obligatorio")
    private String origen;

    // Indica si fue registrada en modo fallback
    private Boolean fallback = false;
}