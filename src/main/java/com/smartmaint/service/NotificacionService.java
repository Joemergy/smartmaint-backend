package com.smartmaint.service;

import com.smartmaint.dto.NotificacionDTO;
import com.smartmaint.model.Notificacion;
import com.smartmaint.model.Tarea;
import com.smartmaint.model.Usuario;
import com.smartmaint.repository.NotificacionRepository;
import com.smartmaint.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificacionDTO> listarPorCorreoUsuario(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo.trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return notificacionRepository.findByUsuario_IdOrderByFechaDesc(usuario.getId())
                .stream()
                .map(NotificacionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long contarNoLeidasPorCorreoUsuario(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo.trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return notificacionRepository.countByUsuario_IdAndLeidoFalse(usuario.getId());
    }

    @Transactional
    public NotificacionDTO marcarComoLeida(Long idNotificacion, String correoUsuario) {
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario.trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Notificacion notificacion = notificacionRepository.findById(idNotificacion)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

        if (!notificacion.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("No tienes permisos sobre esta notificación");
        }

        notificacion.setLeido(true);
        return NotificacionDTO.fromEntity(notificacionRepository.save(notificacion));
    }

    @Transactional
    public void crearNotificacionAsignacion(Tarea tarea) {
        Optional<Usuario> destinatario = resolverDestinatario(tarea);
        destinatario.ifPresent(usuario -> {
            String mensaje = String.format("Se te asignó una nueva tarea #%d: %s", tarea.getId(), safe(tarea.getTitulo()));
            crearNotificacion(usuario, mensaje);
        });
    }

    @Transactional
    public void crearNotificacionCambioEstado(Tarea tarea, String estadoAnterior, String actorCorreo) {
        Optional<Usuario> destinatarioOpt = resolverDestinatario(tarea);
        if (destinatarioOpt.isEmpty()) return;

        Usuario destinatario = destinatarioOpt.get();
        boolean actorEsUsuarioAsignado = actorCorreo != null
                && !actorCorreo.isBlank()
                && actorCorreo.trim().equalsIgnoreCase(destinatario.getCorreo());

        if (actorEsUsuarioAsignado) {
            // El usuario modificó su propia tarea → notificar a los admins de su empresa
            String mensaje = String.format(
                    "El colaborador %s cambió el estado de la tarea #%d '%s' de '%s' a '%s'",
                    safe(destinatario.getNombre()), tarea.getId(), safe(tarea.getTitulo()),
                    safe(estadoAnterior), safe(tarea.getEstado()));
            if (destinatario.getEmpresa() != null) {
                List<Usuario> admins = usuarioRepository
                        .findByEmpresaIdAndRolNombreIgnoreCaseOrderByCreatedAtAsc(
                                destinatario.getEmpresa().getId(), "ADMIN");
                admins.forEach(admin -> crearNotificacion(admin, mensaje));
            }
        } else {
            // El admin (u otro actor) modificó la tarea → notificar al usuario asignado
            String mensaje = String.format(
                    "Tu tarea #%d '%s' pasó de '%s' a '%s'",
                    tarea.getId(), safe(tarea.getTitulo()), safe(estadoAnterior), safe(tarea.getEstado()));
            crearNotificacion(destinatario, mensaje);
        }
    }

    private Optional<Usuario> resolverDestinatario(Tarea tarea) {
        if (tarea.getUsuarioAsignado() != null && tarea.getUsuarioAsignado().getId() != null) {
            return usuarioRepository.findById(tarea.getUsuarioAsignado().getId());
        }

        if (tarea.getCorreoColaborador() != null && !tarea.getCorreoColaborador().isBlank()) {
            return usuarioRepository.findByCorreo(tarea.getCorreoColaborador().trim().toLowerCase());
        }

        if (tarea.getIdColaborador() != null && !tarea.getIdColaborador().isBlank()) {
            return usuarioRepository.findByIdColaborador(tarea.getIdColaborador().trim());
        }

        return Optional.empty();
    }

    private void crearNotificacion(Usuario usuario, String mensaje) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(usuario);
        notificacion.setMensaje(mensaje);
        notificacion.setFecha(LocalDateTime.now());
        notificacion.setLeido(false);
        notificacionRepository.save(notificacion);
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "sin dato";
        }
        return value.trim();
    }
}
