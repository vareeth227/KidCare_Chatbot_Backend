package com.kidcare.chatbot_service.service;

import com.kidcare.chatbot_service.dto.InteraccionRequestDTO;
import com.kidcare.chatbot_service.dto.InteraccionResponseDTO;
import com.kidcare.chatbot_service.model.Interaccion;
import com.kidcare.chatbot_service.repository.InteraccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

// Servicio que maneja el registro y gestión de interacciones
@Service
public class InteraccionService {

    @Autowired
    private InteraccionRepository interaccionRepository;

    @Autowired
    private AnonymizationService anonymizationService;

    // Registra una nueva interacción en MongoDB
    public InteraccionResponseDTO registrar(InteraccionRequestDTO dto) {
        String obsAnonimizadas = anonymizationService.anonimizar(dto.getObservaciones());

        Interaccion interaccion = new Interaccion();
        interaccion.setIdMenor(dto.getIdMenor());
        interaccion.setObservaciones(obsAnonimizadas);
        interaccion.setOrigen(dto.getOrigen());
        interaccion.setFecha(LocalDate.now());
        interaccion.setEditado(false);
        interaccion.setFallback(dto.getFallback() != null && dto.getFallback());
        interaccion.setIdHistorial(dto.getIdHistorial());
        interaccionRepository.save(interaccion);

        return mapToDTO(interaccion);
    }

    // Obtiene todas las interacciones de un menor ordenadas por fecha
    public List<InteraccionResponseDTO> obtenerPorMenor(Integer idMenor) {
        return interaccionRepository.findByIdMenorOrderByFechaDesc(idMenor)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Obtiene interacciones de un menor filtrando por IDs específicos (uso médico)
    public List<InteraccionResponseDTO> obtenerPorMenorFiltrado(Integer idMenor, List<String> ids) {
        if (ids == null || ids.isEmpty()) return obtenerPorMenor(idMenor);
        return interaccionRepository.findByIdMenorAndIdIn(idMenor, ids)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Edita el contenido de una interacción existente
    public InteraccionResponseDTO editar(String id, InteraccionRequestDTO dto) {

        Interaccion interaccion = interaccionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Interacción no encontrada"));

        interaccion.setObservaciones(anonymizationService.anonimizar(dto.getObservaciones()));
        interaccion.setEditado(true);
        interaccion.setFechaEdicion(LocalDate.now());
        interaccionRepository.save(interaccion);

        return mapToDTO(interaccion);
    }

    // Elimina una interacción por su id
    public void eliminar(String id) {
        if (!interaccionRepository.existsById(id)) {
            throw new RuntimeException("Interacción no encontrada");
        }
        interaccionRepository.deleteById(id);
    }

    // Convierte una entidad Interaccion a InteraccionResponseDTO
    private InteraccionResponseDTO mapToDTO(Interaccion interaccion) {
        InteraccionResponseDTO dto = new InteraccionResponseDTO();
        dto.setId(interaccion.getId());
        dto.setIdMenor(interaccion.getIdMenor());
        dto.setFecha(interaccion.getFecha());
        dto.setObservaciones(interaccion.getObservaciones());
        dto.setOrigen(interaccion.getOrigen());
        dto.setEditado(interaccion.getEditado());
        dto.setFechaEdicion(interaccion.getFechaEdicion());
        dto.setFallback(interaccion.getFallback());
        return dto;
    }
}
