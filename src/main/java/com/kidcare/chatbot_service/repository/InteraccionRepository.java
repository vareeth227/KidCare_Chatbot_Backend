package com.kidcare.chatbot_service.repository;

import com.kidcare.chatbot_service.model.Interaccion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Repositorio que maneja el acceso a datos de la colección Interaccion en MongoDB
@Repository
public interface InteraccionRepository extends MongoRepository<Interaccion, String> {

    // Obtiene todas las interacciones de un menor ordenadas por fecha
    List<Interaccion> findByIdMenorOrderByFechaDesc(Integer idMenor);

    // Obtiene todas las interacciones de un historial específico
    List<Interaccion> findByIdHistorial(Integer idHistorial);
}