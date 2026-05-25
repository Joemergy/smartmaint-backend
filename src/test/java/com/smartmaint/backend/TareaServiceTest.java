package com.smartmaint.backend;

import com.smartmaint.dto.TareaDTO;
import com.smartmaint.model.Tarea;
import com.smartmaint.repository.NotaTareaRepository;
import com.smartmaint.repository.TareaAuditLogRepository;
import com.smartmaint.repository.TareaRepository;
import com.smartmaint.repository.UsuarioRepository;
import com.smartmaint.service.NotificacionService;
import com.smartmaint.service.TareaService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TareaServiceTest {

    @Mock
    private TareaRepository tareaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NotaTareaRepository notaTareaRepository;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private TareaAuditLogRepository tareaAuditLogRepository;

    @InjectMocks
    private TareaService tareaService;

    @Test
    void crearTarea_debeGuardarCorrectamente() {
        Tarea tarea = new Tarea();
        tarea.setNombreMaquina("Compresor");
        tarea.setEstado("Pendiente");
        tarea.setFechaInicio(LocalDateTime.now());
        tarea.setPrioridad("ALTA");
        tarea.setCorreoColaborador("  tecnico@empresa.com  ");

        when(tareaRepository.save(tarea)).thenReturn(tarea);

        TareaDTO resultado = tareaService.crearTarea(tarea);

        assertNotNull(resultado);
        assertEquals("Compresor", resultado.getNombreMaquina());
        assertEquals("Pendiente", resultado.getEstado());
        assertEquals("tecnico@empresa.com", tarea.getCorreoColaborador());
        verify(tareaRepository).save(tarea);
        verify(tareaAuditLogRepository).save(any());
    }

    @Test
    void crearTarea_conCorreoInvalido_debeFallar() {
        Tarea tarea = new Tarea();
        tarea.setNombreMaquina("Compresor");
        tarea.setEstado("Pendiente");
        tarea.setFechaInicio(LocalDateTime.now());
        tarea.setPrioridad("ALTA");
        tarea.setCorreoColaborador("correo-invalido");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> tareaService.crearTarea(tarea));

        assertTrue(ex.getMessage().contains("correo"));
        verify(tareaRepository, never()).save(any());
    }

    @Test
    void actualizarEstado_debeActualizarYAuditar() {
        Tarea tarea = new Tarea();
        tarea.setId(10L);
        tarea.setNombreMaquina("Compresor");
        tarea.setEstado("Pendiente");
        tarea.setFechaInicio(LocalDateTime.now());

        when(tareaRepository.findById(10L)).thenReturn(Optional.of(tarea));
        when(tareaRepository.save(tarea)).thenReturn(tarea);

        TareaDTO resultado = tareaService.actualizarEstado(10L, "Completado", "ADMIN@EMPRESA.COM");

        assertEquals("Completado", resultado.getEstado());
        verify(tareaRepository).save(tarea);
        verify(notificacionService).crearNotificacionCambioEstado(eq(tarea), eq("Pendiente"), eq("ADMIN@EMPRESA.COM"));
        verify(tareaAuditLogRepository).save(any());
    }
}
