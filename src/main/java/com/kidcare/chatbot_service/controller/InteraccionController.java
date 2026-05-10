package com.kidcare.chatbot_service.controller;

import com.kidcare.chatbot_service.dto.InteraccionRequestDTO;
import com.kidcare.chatbot_service.dto.InteraccionResponseDTO;
import com.kidcare.chatbot_service.service.InteraccionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Controlador que expone los endpoints de gestión de interacciones
@RestController
@RequestMapping("/api/interacciones")
public class InteraccionController {

    @Autowired
    private InteraccionService interaccionService;

    // POST /api/interacciones — registra una nueva interacción
    @PostMapping
    public ResponseEntity<InteraccionResponseDTO> registrar(@Valid @RequestBody InteraccionRequestDTO dto) {
        return ResponseEntity.ok(interaccionService.registrar(dto));
    }

    // GET /api/interacciones/menor/{idMenor} — obtiene todas las interacciones de
    // un menor
    @GetMapping("/menor/{idMenor}")
    public ResponseEntity<List<InteraccionResponseDTO>> listarPorMenor(@PathVariable Integer idMenor) {
        return ResponseEntity.ok(interaccionService.obtenerPorMenor(idMenor));
    }

    // PUT /api/interacciones/{id} — edita una interacción existente
    @PutMapping("/{id}")
    public ResponseEntity<InteraccionResponseDTO> editar(@PathVariable String id,
            @Valid @RequestBody InteraccionRequestDTO dto) {
        return ResponseEntity.ok(interaccionService.editar(id, dto));
    }

    // DELETE /api/interacciones/{id} — elimina una interacción
    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminar(@PathVariable String id) {
        interaccionService.eliminar(id);
        return ResponseEntity.ok("Interacción eliminada correctamente");
    }

    // GET /api/interacciones/interno/menor/{idMenor} — endpoint interno sin JWT para inter-service
    @GetMapping("/interno/menor/{idMenor}")
    public ResponseEntity<List<InteraccionResponseDTO>> listarInterno(@PathVariable Integer idMenor) {
        return ResponseEntity.ok(interaccionService.obtenerPorMenor(idMenor));
    }
}