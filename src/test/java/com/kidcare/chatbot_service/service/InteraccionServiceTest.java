package com.kidcare.chatbot_service.service;

import com.kidcare.chatbot_service.dto.InteraccionRequestDTO;
import com.kidcare.chatbot_service.dto.InteraccionResponseDTO;
import com.kidcare.chatbot_service.model.Interaccion;
import com.kidcare.chatbot_service.repository.InteraccionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteraccionServiceTest {

    @Mock InteraccionRepository interaccionRepository;
    @Mock AnonymizationService anonymizationService;
    @InjectMocks InteraccionService interaccionService;

    private InteraccionRequestDTO dto(int idMenor, String observaciones) {
        InteraccionRequestDTO dto = new InteraccionRequestDTO();
        dto.setIdMenor(idMenor);
        dto.setObservaciones(observaciones);
        dto.setOrigen("CHATBOT");
        dto.setFallback(false);
        return dto;
    }

    // ─── registrar ────────────────────────────────────────────────────────────

    @Test
    void registrar_llama_anonimizar_antes_de_persistir() {
        when(anonymizationService.anonimizar("texto con nombre Juan")).thenReturn("texto anonimizado");
        when(interaccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InteraccionResponseDTO resultado = interaccionService.registrar(dto(1, "texto con nombre Juan"));

        assertThat(resultado.getObservaciones()).isEqualTo("texto anonimizado");
        verify(anonymizationService).anonimizar("texto con nombre Juan");
    }

    @Test
    void registrar_marca_editado_false_y_asigna_idMenor() {
        when(anonymizationService.anonimizar(anyString())).thenReturn("obs");
        when(interaccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InteraccionResponseDTO resultado = interaccionService.registrar(dto(7, "obs"));

        assertThat(resultado.getIdMenor()).isEqualTo(7);
        assertThat(resultado.getEditado()).isFalse();
    }

    @Test
    void registrar_fallback_true_se_persiste_correctamente() {
        InteraccionRequestDTO dto = dto(1, "obs");
        dto.setFallback(true);

        when(anonymizationService.anonimizar(anyString())).thenReturn("obs");
        when(interaccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InteraccionResponseDTO resultado = interaccionService.registrar(dto);

        assertThat(resultado.getFallback()).isTrue();
    }

    // ─── editar ───────────────────────────────────────────────────────────────

    @Test
    void editar_anonimiza_contenido_editado() {
        Interaccion existente = new Interaccion();
        existente.setId("abc123");
        existente.setObservaciones("texto original");

        when(interaccionRepository.findById("abc123")).thenReturn(Optional.of(existente));
        when(anonymizationService.anonimizar("texto editado con PII")).thenReturn("texto editado limpio");
        when(interaccionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InteraccionResponseDTO resultado = interaccionService.editar("abc123", dto(1, "texto editado con PII"));

        assertThat(resultado.getObservaciones()).isEqualTo("texto editado limpio");
        assertThat(resultado.getEditado()).isTrue();
        verify(anonymizationService).anonimizar("texto editado con PII");
    }

    @Test
    void editar_id_no_existe_lanza_excepcion() {
        when(interaccionRepository.findById("noexiste")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interaccionService.editar("noexiste", dto(1, "obs")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrada");
    }

    // ─── eliminar ─────────────────────────────────────────────────────────────

    @Test
    void eliminar_id_valido_llama_deleteById() {
        when(interaccionRepository.existsById("abc123")).thenReturn(true);

        interaccionService.eliminar("abc123");

        verify(interaccionRepository).deleteById("abc123");
    }

    @Test
    void eliminar_id_no_existe_lanza_excepcion() {
        when(interaccionRepository.existsById("noexiste")).thenReturn(false);

        assertThatThrownBy(() -> interaccionService.eliminar("noexiste"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrada");
    }

    // ─── obtenerPorMenor ──────────────────────────────────────────────────────

    @Test
    void obtenerPorMenor_retorna_lista_mapeada() {
        Interaccion i1 = new Interaccion();
        i1.setIdMenor(3);
        i1.setObservaciones("obs1");
        i1.setEditado(false);
        i1.setFallback(false);

        Interaccion i2 = new Interaccion();
        i2.setIdMenor(3);
        i2.setObservaciones("obs2");
        i2.setEditado(true);
        i2.setFallback(false);

        when(interaccionRepository.findByIdMenorOrderByFechaDesc(3)).thenReturn(List.of(i1, i2));

        List<InteraccionResponseDTO> resultado = interaccionService.obtenerPorMenor(3);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getObservaciones()).isEqualTo("obs1");
        assertThat(resultado.get(1).getEditado()).isTrue();
    }
}
