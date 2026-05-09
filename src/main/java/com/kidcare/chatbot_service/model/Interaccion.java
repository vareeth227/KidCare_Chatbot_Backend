package com.kidcare.chatbot_service.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

// Documento MongoDB que almacena cada interacción registrada sobre un menor
@Data
@Document(collection = "INTERACCION")
public class Interaccion {

    // Identificador único generado automáticamente por MongoDB
    @Id
    private String id;

    // Referencia lógica al menor en db_users
    private Integer idMenor;

    // Referencia lógica al historial en db_historial
    private Integer idHistorial;

    // Fecha y hora del registro de la interacción
    private LocalDate fecha;

    // Contenido anonimizado de la interacción
    private String observaciones;

    // Origen del registro: web o móvil
    private String origen;

    // Indica si la interacción fue editada posteriormente
    private Boolean editado = false;

    // Fecha de la última edición realizada
    private LocalDate fechaEdicion;

    // Indica si fue registrada en modo manual por fallo de Claude API
    private Boolean fallback = false;
}