package com.kidcare.chatbot_service.dto;

import lombok.Data;
import java.time.LocalDate;

// DTO que retorna los datos de una interacción registrada
@Data
public class InteraccionResponseDTO {

    // Identificador único de la interacción
    private String id;

    // ID del menor al que pertenece
    private Integer idMenor;

    // Fecha del registro
    private LocalDate fecha;

    // Contenido anonimizado de la interacción
    private String observaciones;

    // Origen: web o móvil
    private String origen;

    // Indica si fue editada
    private Boolean editado;

    // Fecha de edición si aplica
    private LocalDate fechaEdicion;

    // Indica si fue registrada en modo fallback
    private Boolean fallback;
}