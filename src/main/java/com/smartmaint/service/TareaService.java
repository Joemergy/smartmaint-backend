package com.smartmaint.service;

import com.smartmaint.dto.NotaTareaDTO;
import com.smartmaint.dto.TareaDTO;
import com.smartmaint.model.NotaTarea;
import com.smartmaint.model.Tarea;
import com.smartmaint.model.TareaAuditLog;
import com.smartmaint.model.Usuario;
import com.smartmaint.repository.NotaTareaRepository;
import com.smartmaint.repository.TareaAuditLogRepository;
import com.smartmaint.repository.TareaRepository;
import com.smartmaint.repository.UsuarioRepository;
import com.smartmaint.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TareaService {

    private static final Logger logger = LoggerFactory.getLogger(TareaService.class);

    @Autowired
    private TareaRepository tareaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotaTareaRepository notaTareaRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private TareaAuditLogRepository tareaAuditLogRepository;

    // Estados válidos para las tareas
    private static final List<String> ESTADOS_VALIDOS = List.of(
            "Pendiente", "En proceso", "Completado", "Cancelado", "Archivada", "Archivado"
    );

        private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Valida el estado ignorando mayúsculas/minúsculas y espacios extra
     */
    public boolean esEstadoValido(String estado) {
        if (estado == null) return false;
        return ESTADOS_VALIDOS.stream()
                .anyMatch(valido -> valido.equalsIgnoreCase(estado.trim()));
    }

    /**
     * Listar todas las tareas (ADMIN)
     */
    @Transactional(readOnly = true)
    public List<TareaDTO> listarTodas() {
        List<Tarea> tareas = tareaRepository.findAll();
        logger.info("📦 Tareas encontradas: {}", tareas.size());
        return tareas.stream().map(TareaDTO::fromEntity).collect(Collectors.toList());
    }

    /**
     * Listar tareas por empresa (para demo accounts)
     */
    @Transactional(readOnly = true)
    public List<TareaDTO> listarPorEmpresa(Long empresaId) {
        List<Tarea> tareas = tareaRepository.findByEmpresaId(empresaId);
        logger.info("📦 Tareas encontradas para empresa {}: {}", empresaId, tareas.size());
        return tareas.stream().map(TareaDTO::fromEntity).collect(Collectors.toList());
    }

    /**
     * Obtener tarea por ID
     */
    @Transactional(readOnly = true)
    public TareaDTO obtenerPorId(Long id) {
        return tareaRepository.findById(id).map(TareaDTO::fromEntity).orElse(null);
    }

    /**
     * Crear nueva tarea
     */
    @Transactional
    public TareaDTO crearTarea(Tarea tarea) {
        if (tarea == null) {
            logger.warn("⚠️ Intento de crear tarea nula");
            throw new IllegalArgumentException("Payload de tarea vacío");
        }

        tarea.setTitulo(InputSanitizer.normalizeText(tarea.getTitulo(), 100));
        tarea.setDescripcion(InputSanitizer.normalizeText(tarea.getDescripcion()));
        tarea.setEstado(InputSanitizer.normalizeText(tarea.getEstado(), 20));
        tarea.setNombreMaquina(InputSanitizer.normalizeText(tarea.getNombreMaquina(), 50));
        tarea.setNombreColaborador(InputSanitizer.normalizeText(tarea.getNombreColaborador(), 100));
        tarea.setIdColaborador(InputSanitizer.normalizeText(tarea.getIdColaborador(), 50));
        tarea.setCorreoColaborador(InputSanitizer.normalizeEmail(tarea.getCorreoColaborador()));
        tarea.setUbicacion(InputSanitizer.normalizeText(tarea.getUbicacion(), 50));
        tarea.setCategoria(InputSanitizer.normalizeText(tarea.getCategoria(), 30));
        tarea.setNotaTecnica(InputSanitizer.normalizeText(tarea.getNotaTecnica()));
        tarea.setObservaciones(InputSanitizer.normalizeText(tarea.getObservaciones()));

        // Validaciones mínimas obligatorias
        if (tarea.getNombreMaquina() == null || tarea.getNombreMaquina().isBlank()) {
            throw new IllegalArgumentException("El nombre de la máquina es obligatorio");
        }
        if (tarea.getEstado() == null || tarea.getEstado().isBlank()) {
            throw new IllegalArgumentException("El estado es obligatorio");
        }
        if (!esEstadoValido(tarea.getEstado())) {
            throw new IllegalArgumentException("Estado inválido. Debe ser Pendiente, En proceso, Completado o Archivada");
        }
        if (Objects.isNull(tarea.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");
        }
        if (!tarea.getCorreoColaborador().isBlank() && !EMAIL_PATTERN.matcher(tarea.getCorreoColaborador()).matches()) {
            throw new IllegalArgumentException("El correo del colaborador tiene formato invalido");
        }

        // Validación de prioridad si viene informada
        if (tarea.getPrioridad() != null &&
                !List.of("BAJA", "MEDIA", "ALTA").contains(tarea.getPrioridad().toUpperCase().trim())) {
            throw new IllegalArgumentException("La prioridad debe ser BAJA, MEDIA o ALTA");
        }

        logger.debug("📋 Tarea a guardar: {}", tarea);

        try {
            Tarea creada = tareaRepository.save(tarea);
            registrarAuditoria(creada.getId(), null, "CREAR", null, creada.getEstado(), "Tarea creada");

            // No bloquear la creacion de tareas por un fallo secundario de notificaciones.
            try {
                notificacionService.crearNotificacionAsignacion(creada);
            } catch (Exception notificationEx) {
                logger.warn("⚠️ Tarea {} creada, pero fallo al crear notificacion de asignacion", creada.getId(), notificationEx);
            }

            logger.info("✅ Tarea creada con ID: {}", creada.getId());
            return TareaDTO.fromEntity(creada);
        } catch (Exception ex) {
            logger.error("❌ Error al persistir tarea en la base de datos", ex);
            throw ex;
        }
    }

    /**
     * Actualizar estado de una tarea
     */
    @Transactional
    public TareaDTO actualizarEstado(Long id, String nuevoEstado, String actorCorreo) {
        nuevoEstado = InputSanitizer.normalizeText(nuevoEstado, 20);
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            throw new IllegalArgumentException("El nuevo estado no puede estar vacío");
        }
        if (!esEstadoValido(nuevoEstado)) {
            throw new IllegalArgumentException("Estado inválido. Use: Pendiente, En proceso, Completado o Archivada");
        }

        Tarea tarea = tareaRepository.findById(id).orElse(null);
        if (tarea == null) {
            logger.error("❌ Tarea no encontrada con ID: {}", id);
            return null;
        }

        String estadoAnterior = tarea.getEstado();
        tarea.setEstado(nuevoEstado.trim());
        Tarea actualizada = tareaRepository.save(tarea);
        registrarAuditoria(actualizada.getId(), InputSanitizer.normalizeEmail(actorCorreo), "CAMBIAR_ESTADO", estadoAnterior, actualizada.getEstado(), "Cambio de estado");
        notificacionService.crearNotificacionCambioEstado(actualizada, estadoAnterior, actorCorreo);
        logger.info("🔄 Estado actualizado para tarea ID {} → {}", id, nuevoEstado);

        return TareaDTO.fromEntity(actualizada);
    }

    /**
     * Agregar/reemplazar solo la observación (usuario autenticado) — compatibilidad legada
     */
    @Transactional
    public TareaDTO agregarNota(Long id, String observaciones) {
        Tarea tarea = tareaRepository.findById(id).orElse(null);
        if (tarea == null) return null;
        tarea.setObservaciones(InputSanitizer.normalizeText(observaciones));
        return TareaDTO.fromEntity(tareaRepository.save(tarea));
    }

    /**
     * Listar todas las notas de una tarea (ordenadas cronológicamente)
     */
    @Transactional(readOnly = true)
    public List<NotaTareaDTO> getNotasTarea(Long tareaId) {
        return notaTareaRepository.findByTareaIdOrderByFechaCreacionAsc(tareaId)
                .stream().map(NotaTareaDTO::fromEntity).collect(Collectors.toList());
    }

    /**
     * Crear una nueva nota en una tarea
     */
    @Transactional
    public NotaTareaDTO crearNota(Long tareaId, String texto, String autorNombre, String autorCorreo) {
        Tarea tarea = tareaRepository.findById(tareaId).orElse(null);
        if (tarea == null) return null;
        NotaTarea nota = new NotaTarea();
        nota.setTarea(tarea);
        nota.setTexto(InputSanitizer.normalizeText(texto));
        nota.setAutorNombre(InputSanitizer.normalizeText(autorNombre, 150));
        nota.setFechaCreacion(LocalDateTime.now());
        if (autorCorreo != null) {
            usuarioRepository.findByCorreo(InputSanitizer.normalizeEmail(autorCorreo)).ifPresent(nota::setAutor);
        }
        return NotaTareaDTO.fromEntity(notaTareaRepository.save(nota));
    }

    /**
     * Actualizar todos los campos de una tarea (ADMIN)
     */
    @Transactional
    public TareaDTO actualizarTarea(Long id, Tarea datos, java.util.List<String> nuevosArchivos) {
        Tarea tarea = tareaRepository.findById(id).orElse(null);
        if (tarea == null) return null;
        String estadoAnterior = tarea.getEstado();
        if (datos.getTitulo() != null && !datos.getTitulo().isBlank()) tarea.setTitulo(InputSanitizer.normalizeText(datos.getTitulo(), 100));
        if (datos.getDescripcion() != null) tarea.setDescripcion(InputSanitizer.normalizeText(datos.getDescripcion()));
        if (datos.getEstado() != null && esEstadoValido(datos.getEstado())) tarea.setEstado(InputSanitizer.normalizeText(datos.getEstado(), 20));
        if (datos.getFechaInicio() != null) tarea.setFechaInicio(datos.getFechaInicio());
        if (datos.getFechaCierre() != null) tarea.setFechaCierre(datos.getFechaCierre());
        if (datos.getIdColaborador() != null) tarea.setIdColaborador(InputSanitizer.normalizeText(datos.getIdColaborador(), 50));
        if (datos.getNombreColaborador() != null) tarea.setNombreColaborador(InputSanitizer.normalizeText(datos.getNombreColaborador(), 100));
        if (datos.getCorreoColaborador() != null) tarea.setCorreoColaborador(InputSanitizer.normalizeEmail(datos.getCorreoColaborador()));
        if (datos.getIdMaquina() != null) tarea.setIdMaquina(InputSanitizer.normalizeText(datos.getIdMaquina(), 50));
        if (datos.getNombreMaquina() != null && !datos.getNombreMaquina().isBlank()) tarea.setNombreMaquina(InputSanitizer.normalizeText(datos.getNombreMaquina(), 50));
        if (datos.getUbicacion() != null) tarea.setUbicacion(InputSanitizer.normalizeText(datos.getUbicacion(), 50));
        if (datos.getCategoria() != null) tarea.setCategoria(InputSanitizer.normalizeText(datos.getCategoria(), 30));
        if (datos.getNotaTecnica() != null) tarea.setNotaTecnica(InputSanitizer.normalizeText(datos.getNotaTecnica()));
        if (datos.getObservaciones() != null) tarea.setObservaciones(InputSanitizer.normalizeText(datos.getObservaciones()));
        tarea.setGrupal(datos.isGrupal());
        if (nuevosArchivos != null && !nuevosArchivos.isEmpty()) {
            java.util.List<String> existentes = tarea.getArchivos() != null ? new java.util.ArrayList<>(tarea.getArchivos()) : new java.util.ArrayList<>();
            existentes.addAll(nuevosArchivos);
            tarea.setArchivos(existentes);
        }
        Tarea actualizada = tareaRepository.save(tarea);
        registrarAuditoria(actualizada.getId(), null, "ACTUALIZAR", estadoAnterior, actualizada.getEstado(), "Actualizacion de campos de tarea");
        return TareaDTO.fromEntity(actualizada);
    }
    @Transactional(readOnly = true)
    public List<TareaDTO> listarPorEstado(String estado) {
        return tareaRepository.findByEstado(estado.trim())
                .stream().map(TareaDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TareaDTO> listarPorPrioridad(String prioridad) {
        return tareaRepository.findByPrioridad(prioridad.trim().toUpperCase())
                .stream().map(TareaDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TareaDTO> listarPorCategoria(String categoria) {
        return tareaRepository.findByCategoria(categoria.trim())
                .stream().map(TareaDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TareaDTO> listarPorFechaInicioEntre(LocalDateTime inicio, LocalDateTime fin) {
        return tareaRepository.findByFechaInicioBetween(inicio, fin)
                .stream().map(TareaDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TareaDTO> listarPorFechaCierreEntre(LocalDateTime inicio, LocalDateTime fin) {
        return tareaRepository.findByFechaCierreBetween(inicio, fin)
                .stream().map(TareaDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TareaDTO> listarPorCorreoColaborador(String correo) {
        return tareaRepository.findByCorreoColaborador(InputSanitizer.normalizeEmail(correo))
                .stream().map(TareaDTO::fromEntity).collect(Collectors.toList());
    }

    public Usuario obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Usuario no encontrado con ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<TareaDTO> listarPorUsuarioCorreo(String correo) {
        return tareaRepository.findByUsuarioAsignado_Correo(InputSanitizer.normalizeEmail(correo))
                .stream().map(TareaDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TareaDTO> listarPorUsuarioId(Long usuarioId) {
        return tareaRepository.findByUsuarioAsignado_Id(usuarioId)
                .stream().map(TareaDTO::fromEntity).collect(Collectors.toList());
    }

    /**
     * Eliminar una tarea por ID
     */
    @Transactional
    public boolean eliminarTarea(Long id) {
        Tarea tarea = tareaRepository.findById(id).orElse(null);
        if (tarea == null) {
            logger.error("❌ Tarea no encontrada con ID: {}", id);
            return false;
        }
        try {
            tareaRepository.delete(tarea);
            logger.info("✅ Tarea eliminada con ID: {}", id);
            return true;
        } catch (Exception e) {
            logger.error("❌ Error al eliminar tarea con ID: {}", id, e);
            throw e;
        }
    }

    /**
     * Listar todas las tareas dentro de un rango de fechas
     */
    @Transactional(readOnly = true)
    public List<TareaDTO> listarPorRangoFechas(LocalDateTime inicio, LocalDateTime fin) {
        return tareaRepository.findByFechaInicioBetween(inicio, fin)
                .stream().map(TareaDTO::fromEntity).collect(Collectors.toList());
    }

    /**
     * Listar tareas de un colaborador dentro de un rango de fechas
     */
    @Transactional(readOnly = true)
    public List<TareaDTO> listarPorCorreoYRangoFechas(String correo, LocalDateTime inicio, LocalDateTime fin) {
        List<Tarea> tareas = tareaRepository.findByCorreoColaboradorAndFechaInicioBetween(InputSanitizer.normalizeEmail(correo), inicio, fin);
        return tareas.stream().map(TareaDTO::fromEntity).collect(Collectors.toList());
    }

    private void registrarAuditoria(Long tareaId, String actorCorreo, String accion, String estadoAnterior, String estadoNuevo, String detalle) {
        try {
            TareaAuditLog log = new TareaAuditLog();
            log.setTareaId(tareaId);
            log.setActorCorreo(InputSanitizer.normalizeEmail(actorCorreo));
            log.setAccion(accion);
            log.setEstadoAnterior(InputSanitizer.normalizeText(estadoAnterior, 30));
            log.setEstadoNuevo(InputSanitizer.normalizeText(estadoNuevo, 30));
            log.setDetalle(InputSanitizer.normalizeText(detalle, 500));
            tareaAuditLogRepository.save(log);
        } catch (Exception ex) {
            logger.warn("No se pudo registrar auditoria para tarea {}", tareaId, ex);
        }
    }
}
