package com.smartmaint.controller;

import com.smartmaint.api.ApiResponses;
import com.smartmaint.dto.CreateNotaRequest;
import com.smartmaint.dto.NotaTareaDTO;
import com.smartmaint.dto.TareaDTO;
import com.smartmaint.dto.UpdateEstadoRequest;
import com.smartmaint.model.Tarea;
import com.smartmaint.model.Usuario;
import com.smartmaint.service.TareaService;
import com.smartmaint.util.InputSanitizer;
import com.smartmaint.util.JwtUtil;
import com.smartmaint.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/tareas")
public class TareaController {

    private static final Logger logger = LoggerFactory.getLogger(TareaController.class);

    @Autowired
    private TareaService tareaService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Listar todas las tareas (ADMIN)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<?> listarTodas(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        try {
            String correoUsuario = null;
            Long empresaId = null;

            // Extraer correo del token para obtener empresa del usuario
            if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                String token = authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7) : authorizationHeader;
                correoUsuario = jwtUtil.extraerCorreoDesdeToken(token);

                if (correoUsuario != null && !correoUsuario.isBlank()) {
                    Usuario usuario = usuarioRepository.findByCorreo(correoUsuario).orElse(null);
                    if (usuario != null && usuario.getEmpresa() != null) {
                        empresaId = usuario.getEmpresa().getId();
                        logger.info("🔍 Filtrando tareas por empresa ID: {} para usuario: {}", empresaId, correoUsuario);
                    }
                }
            }

            List<TareaDTO> tareasDTO;
            if (empresaId != null) {
                tareasDTO = tareaService.listarPorEmpresa(empresaId);
            } else {
                logger.warn("⚠️ No se pudo determinar la empresa del usuario, listando todas las tareas");
                tareasDTO = tareaService.listarTodas();
            }

            return ApiResponses.ok(tareasDTO);
        } catch (Exception e) {
            logger.error("❌ Error al listar tareas", e);
            return ApiResponses.internalError("Error interno al listar tareas");
        }
    }

    /**
     * Alias de compatibilidad para frontend que consulta /admin.
     * Con soporte para filtro de rango de fechas
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<?> listarTodasAdminAlias(
            @RequestParam(value = "fechaInicio", required = false) String fechaInicio,
            @RequestParam(value = "fechaFin", required = false) String fechaFin,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            String correoUsuario = null;
            Long empresaId = null;

            // Extraer correo del token para obtener empresa del usuario
            if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                String token = authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7) : authorizationHeader;
                correoUsuario = jwtUtil.extraerCorreoDesdeToken(token);

                if (correoUsuario != null && !correoUsuario.isBlank()) {
                    Usuario usuario = usuarioRepository.findByCorreo(correoUsuario).orElse(null);
                    if (usuario != null && usuario.getEmpresa() != null) {
                        empresaId = usuario.getEmpresa().getId();
                        logger.info("🔍 Filtrando tareas por empresa ID: {} para usuario: {}", empresaId, correoUsuario);
                    }
                }
            }

            // Si hay filtro de fechas, aplicarlo
            if (fechaInicio != null && !fechaInicio.isBlank() && fechaFin != null && !fechaFin.isBlank()) {
                try {
                    LocalDateTime inicio = LocalDateTime.parse(fechaInicio + "T00:00:00");
                    LocalDateTime fin = LocalDateTime.parse(fechaFin + "T23:59:59");
                    List<TareaDTO> tareasDTO = tareaService.listarPorRangoFechas(inicio, fin);
                    // Filtrar por empresa si se determinó
                    if (empresaId != null) {
                        tareasDTO = tareasDTO.stream()
                            .filter(t -> {
                                // Necesitamos verificar la empresa de la tarea
                                // Por ahora, filtramos después de obtener todas las tareas
                                // Esto es menos eficiente pero funciona como solución temporal
                                return true; // TODO: Implementar filtrado por empresa en listarPorRangoFechas
                            })
                            .collect(java.util.stream.Collectors.toList());
                    }
                    return ApiResponses.ok(tareasDTO);
                } catch (DateTimeParseException ex) {
                    return ApiResponses.badRequest("Formato de fecha inválido. Usa yyyy-MM-dd");
                }
            } else {
                // Sin filtro de fechas, listar todas filtradas por empresa
                List<TareaDTO> tareasDTO;
                if (empresaId != null) {
                    tareasDTO = tareaService.listarPorEmpresa(empresaId);
                } else {
                    logger.warn("⚠️ No se pudo determinar la empresa del usuario, listando todas las tareas");
                    tareasDTO = tareaService.listarTodas();
                }
                return ApiResponses.ok(tareasDTO);
            }
        } catch (Exception e) {
            logger.error("❌ Error al listar tareas del admin", e);
            return ApiResponses.internalError("Error interno al listar tareas");
        }
    }

    /**
     * Obtener tarea por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            TareaDTO tareaDTO = tareaService.obtenerPorId(id);
            if (tareaDTO == null) {
                return ApiResponses.notFound("Tarea no encontrada");
            }
            return ApiResponses.ok(tareaDTO);
        } catch (Exception e) {
            logger.error("❌ Error al obtener tarea por ID {}", id, e);
            return ApiResponses.internalError("Error interno al obtener tarea");
        }
    }

    /**
     * Crear nueva tarea (ADMIN)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<?> crearTarea(
            @RequestParam("titulo") String titulo,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("fechaInicio") String fechaInicio,
            @RequestParam("nombreMaquina") String nombreMaquina,
            @RequestParam("estado") String estado,
            @RequestParam(value = "idColaborador", required = false) String idColaborador,
            @RequestParam(value = "nombreColaborador", required = false) String nombreColaborador,
            @RequestParam(value = "correoColaborador", required = false) String correoColaborador,
            @RequestParam(value = "idMaquina", required = false) String idMaquina,
            @RequestParam(value = "ubicacion", required = false) String ubicacion,
            @RequestParam(value = "categoria", required = false) String categoria,
            @RequestParam(value = "notaTecnica", required = false) String notaTecnica,
            @RequestParam(value = "grupal", required = false) Boolean grupal,
            @RequestParam(value = "observaciones", required = false) String observaciones,
            @RequestPart(value = "archivos", required = false) MultipartFile[] archivos
    ) {
        try {
            Tarea tarea = new Tarea();
            tarea.setTitulo(titulo);
            tarea.setDescripcion(descripcion);

            try {
                logger.debug("📅 Fecha recibida: {}", fechaInicio);
                tarea.setFechaInicio(LocalDateTime.parse(fechaInicio));
            } catch (DateTimeParseException ex) {
                logger.error("❌ Error al parsear fechaInicio: {}", fechaInicio, ex);
                return ApiResponses.badRequest("Formato de fecha inválido. Usa yyyy-MM-dd'T'HH:mm:ss");
            }

            tarea.setNombreMaquina(nombreMaquina);
            tarea.setEstado(estado);
            tarea.setIdColaborador(idColaborador);
            tarea.setNombreColaborador(nombreColaborador);
            tarea.setCorreoColaborador(correoColaborador);
            tarea.setIdMaquina(idMaquina);
            tarea.setUbicacion(ubicacion);
            tarea.setCategoria(categoria);
            tarea.setNotaTecnica(notaTecnica);
            tarea.setGrupal(grupal != null ? grupal : false);
            tarea.setObservaciones(observaciones);

            if (archivos != null) {
                List<String> nombres = new ArrayList<>();
                Path uploadDir = Paths.get("uploads", "tareas");
                try {
                    Files.createDirectories(uploadDir);
                } catch (IOException ex) {
                    logger.error("❌ No se pudo crear el directorio de uploads", ex);
                    return ApiResponses.internalError("Error al preparar almacenamiento de archivos");
                }
                for (MultipartFile archivo : archivos) {
                    if (!archivo.isEmpty()) {
                        String nombreOriginal = StringUtils.cleanPath(
                            archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo"
                        );
                        String nombreUnico = UUID.randomUUID().toString() + "_" + nombreOriginal;
                        Path destino = uploadDir.resolve(nombreUnico);
                        try {
                            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
                            nombres.add(nombreUnico);
                        } catch (IOException ex) {
                            logger.error("❌ Error al guardar archivo: {}", nombreOriginal, ex);
                        }
                    }
                }
                tarea.setArchivos(nombres);
            }

            TareaDTO creada = tareaService.crearTarea(tarea);
            return ApiResponses.created(creada);
        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ Error de validacion al crear tarea: {}", e.getMessage());
            return ApiResponses.badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Error al crear tarea", e);
            return ApiResponses.internalError("Error interno al crear tarea");
        }
    }

    /**
     * Actualizar estado de una tarea (autenticado)
     */
    @PutMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEstadoRequest body,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            String nuevoEstado = InputSanitizer.normalizeText(body.getEstado(), 20);
            if (nuevoEstado == null || nuevoEstado.isBlank()) {
                return ApiResponses.badRequest("El estado no puede estar vacío");
            }
            String actorCorreo = null;
            if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                String token = authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7) : authorizationHeader;
                actorCorreo = jwtUtil.extraerCorreoDesdeToken(token);
            }

            TareaDTO actualizada = tareaService.actualizarEstado(id, nuevoEstado, actorCorreo);
            if (actualizada == null) {
                return ApiResponses.notFound("Tarea no encontrada");
            }
            return ApiResponses.ok(actualizada);
        } catch (Exception e) {
            logger.error("❌ Error al actualizar estado de la tarea {}", id, e);
            return ApiResponses.internalError("Error interno al actualizar estado");
        }
    }

    /**
     * Agregar nota/observacion (usuario autenticado, solo campo observaciones) — legado
     */
    @PatchMapping("/{id}/nota")
    public ResponseEntity<?> agregarNota(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            if (authorizationHeader == null || authorizationHeader.isBlank()) {
                return ApiResponses.unauthorized("Authorization header ausente");
            }
            String observaciones = body.getOrDefault("observaciones", "");
            TareaDTO actualizada = tareaService.agregarNota(id, observaciones);
            if (actualizada == null) {
                return ApiResponses.notFound("Tarea no encontrada");
            }
            return ApiResponses.ok(actualizada);
        } catch (Exception e) {
            logger.error("❌ Error al agregar nota a tarea {}", id, e);
            return ApiResponses.internalError("Error interno al agregar nota");
        }
    }

    /**
     * Listar notas de una tarea
     */
    @GetMapping("/{id}/notas")
    public ResponseEntity<?> listarNotas(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            if (authorizationHeader == null || authorizationHeader.isBlank()) {
                return ApiResponses.unauthorized("Authorization header ausente");
            }
            return ApiResponses.ok(tareaService.getNotasTarea(id));
        } catch (Exception e) {
            logger.error("❌ Error al listar notas de tarea {}", id, e);
            return ApiResponses.internalError("Error interno al listar notas");
        }
    }

    /**
     * Crear nueva nota en una tarea (usuario autenticado)
     */
    @PostMapping("/{id}/notas")
    public ResponseEntity<?> crearNota(
            @PathVariable Long id,
            @Valid @RequestBody CreateNotaRequest body,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            if (authorizationHeader == null || authorizationHeader.isBlank()) {
                return ApiResponses.unauthorized("Authorization header ausente");
            }
            String token = authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7) : authorizationHeader;
            String autorCorreo = jwtUtil.extraerCorreoDesdeToken(token);
            String texto = InputSanitizer.normalizeText(body.getTexto(), 4000);
            if (texto.isEmpty()) {
                return ApiResponses.badRequest("El texto de la nota no puede estar vacío");
            }
            // Obtener nombre completo del autor desde el token o la BD
            String autorNombre = InputSanitizer.normalizeText(body.getAutorNombre(), 150);
            if (autorNombre.isBlank()) {
                autorNombre = autorCorreo;
            }
            NotaTareaDTO nota = tareaService.crearNota(id, texto, autorNombre, autorCorreo);
            if (nota == null) {
                return ApiResponses.notFound("Tarea no encontrada");
            }
            return ApiResponses.ok(nota);
        } catch (Exception e) {
            logger.error("❌ Error al crear nota en tarea {}", id, e);
            return ApiResponses.internalError("Error interno al crear nota");
        }
    }

    /**
     * Actualizar todos los campos de una tarea (ADMIN).
     * Se admite PUT y POST para compatibilidad con clientes/proxies que bloquean PUT multipart.
     */
    @RequestMapping(value = "/{id}", method = {RequestMethod.PUT, RequestMethod.POST}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<?> actualizarTarea(
            @PathVariable Long id,
            @RequestParam(value = "titulo", required = false) String titulo,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "fechaInicio", required = false) String fechaInicio,
            @RequestParam(value = "entregaEstimada", required = false) String entregaEstimada,
            @RequestParam(value = "nombreMaquina", required = false) String nombreMaquina,
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "idColaborador", required = false) String idColaborador,
            @RequestParam(value = "nombreColaborador", required = false) String nombreColaborador,
            @RequestParam(value = "correoColaborador", required = false) String correoColaborador,
            @RequestParam(value = "idMaquina", required = false) String idMaquina,
            @RequestParam(value = "ubicacion", required = false) String ubicacion,
            @RequestParam(value = "categoria", required = false) String categoria,
            @RequestParam(value = "notaTecnica", required = false) String notaTecnica,
            @RequestParam(value = "grupal", required = false) Boolean grupal,
            @RequestParam(value = "observaciones", required = false) String observaciones,
            @RequestPart(value = "archivos", required = false) MultipartFile[] archivos
    ) {
        try {
            Tarea datos = new Tarea();
            datos.setTitulo(titulo);
            datos.setDescripcion(descripcion);
            datos.setNombreMaquina(nombreMaquina);
            datos.setEstado(estado);
            datos.setIdColaborador(idColaborador);
            datos.setNombreColaborador(nombreColaborador);
            datos.setCorreoColaborador(correoColaborador);
            datos.setIdMaquina(idMaquina);
            datos.setUbicacion(ubicacion);
            datos.setCategoria(categoria);
            datos.setNotaTecnica(notaTecnica);
            datos.setGrupal(grupal != null ? grupal : false);
            datos.setObservaciones(observaciones);
            if (fechaInicio != null && !fechaInicio.isBlank()) {
                try { datos.setFechaInicio(LocalDateTime.parse(fechaInicio)); } catch (DateTimeParseException ex) {
                    return ApiResponses.badRequest("Formato de fechaInicio inválido");
                }
            }
            if (entregaEstimada != null && !entregaEstimada.isBlank()) {
                try { datos.setFechaCierre(LocalDateTime.parse(entregaEstimada)); } catch (DateTimeParseException ex) {
                    return ApiResponses.badRequest("Formato de entregaEstimada inválido");
                }
            }
            List<String> nuevosNombres = new ArrayList<>();
            if (archivos != null) {
                Path uploadDir = Paths.get("uploads", "tareas");
                Files.createDirectories(uploadDir);
                for (MultipartFile archivo : archivos) {
                    if (!archivo.isEmpty()) {
                        String nombreOriginal = StringUtils.cleanPath(
                            archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo"
                        );
                        String nombreUnico = UUID.randomUUID().toString() + "_" + nombreOriginal;
                        Files.copy(archivo.getInputStream(), uploadDir.resolve(nombreUnico), StandardCopyOption.REPLACE_EXISTING);
                        nuevosNombres.add(nombreUnico);
                    }
                }
            }
            TareaDTO actualizada = tareaService.actualizarTarea(id, datos, nuevosNombres);
            if (actualizada == null) {
                return ApiResponses.notFound("Tarea no encontrada");
            }
            return ApiResponses.ok(actualizada);
        } catch (Exception e) {
            logger.error("❌ Error al actualizar tarea {}", id, e);
            return ApiResponses.internalError("Error interno al actualizar tarea");
        }
    }

    /**
     * Archivar tarea (ADMIN)
     */
    @PutMapping("/{id}/archivar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<?> archivarTarea(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            String actorCorreo = null;
            if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                String token = authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7) : authorizationHeader;
                actorCorreo = jwtUtil.extraerCorreoDesdeToken(token);
            }

            TareaDTO archivada = tareaService.actualizarEstado(id, "Archivada", actorCorreo);
            if (archivada == null) {
                return ApiResponses.notFound("Tarea no encontrada");
            }
            return ApiResponses.ok(archivada);
        } catch (Exception e) {
            logger.error("❌ Error al archivar tarea {}", id, e);
            return ApiResponses.internalError("Error interno al archivar tarea");
        }
    }

    /**
     * Listar tareas del usuario autenticado
     */
    @GetMapping("/mias/compat")
    public ResponseEntity<?> listarMisTareasCompat(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        try {
            if (authorizationHeader == null || authorizationHeader.isBlank()) {
                return ApiResponses.unauthorized("Authorization header ausente");
            }
            String token = authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7) : authorizationHeader;
            String correo = jwtUtil.extraerCorreoDesdeToken(token);
            if (correo == null || correo.isBlank()) {
                return ApiResponses.unauthorized("Token inválido o sin correo");
            }
            List<TareaDTO> tareasDTO = tareaService.listarPorCorreoColaborador(correo);
            return ApiResponses.ok(tareasDTO);
        } catch (Exception e) {
            logger.error("❌ Error al listar tareas del usuario autenticado", e);
            return ApiResponses.internalError("Error interno al listar mis tareas");
        }
    }

    /**
     * Alias de compatibilidad con clientes antiguos.
     */
    @GetMapping("/asignadas")
    public ResponseEntity<?> listarTareasAsignadasCompat() {
        try {
            return listarTareasEnRango(null, null, null);
        } catch (Exception e) {
            logger.error("❌ Error al listar tareas asignadas", e);
            return ApiResponses.internalError("Error interno");
        }
    }

    /**
     * Eliminar una tarea por ID (ADMIN/SUPERADMIN o usuario autenticado para sus propias tareas)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarTarea(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            // Obtener tarea para verificar permisos
            TareaDTO tarea = tareaService.obtenerPorId(id);
            if (tarea == null) {
                return ApiResponses.notFound("Tarea no encontrada");
            }

            // Verificar permisos
            if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                String token = authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7) : authorizationHeader;
                String correoUsuario = jwtUtil.extraerCorreoDesdeToken(token);
                String rolUsuario = jwtUtil.extraerRolDesdeToken(token);
                
                logger.info("🔍 Intento de eliminar tarea {} - Usuario: {}, Rol: {}", id, correoUsuario, rolUsuario);
                
                if (correoUsuario != null && !correoUsuario.isBlank()) {
                    // Verificar si es ADMIN o SUPERADMIN
                    if (rolUsuario != null && (rolUsuario.equals("ADMIN") || rolUsuario.equals("SUPERADMIN"))) {
                        logger.info("✅ Usuario {} con rol {} puede eliminar tarea {}", correoUsuario, rolUsuario, id);
                        // Admin puede eliminar cualquier tarea
                        boolean eliminada = tareaService.eliminarTarea(id);
                        if (!eliminada) {
                            return ApiResponses.notFound("Tarea no encontrada");
                        }
                        return ApiResponses.ok(Map.of("mensaje", "Tarea eliminada correctamente"));
                    }
                    
                    // Si no es admin, verificar que sea el colaborador de la tarea
                    String correoColaborador = tarea.getCorreoColaborador();
                    logger.info("🔍 Verificando si usuario {} es colaborador de la tarea (colaborador: {})", correoUsuario, correoColaborador);
                    if (correoColaborador != null && correoColaborador.equalsIgnoreCase(correoUsuario)) {
                        logger.info("✅ Usuario {} es colaborador de la tarea {}", correoUsuario, id);
                        // Usuario puede eliminar su propia tarea
                        boolean eliminada = tareaService.eliminarTarea(id);
                        if (!eliminada) {
                            return ApiResponses.notFound("Tarea no encontrada");
                        }
                        return ApiResponses.ok(Map.of("mensaje", "Tarea eliminada correctamente"));
                    }
                }
                
                logger.warn("⚠️ Usuario {} con rol {} no tiene permiso para eliminar tarea {}", correoUsuario, rolUsuario, id);
            }

            // Si llegamos aquí, el usuario no tiene permisos
            return ApiResponses.forbidden("No tienes permiso para eliminar esta tarea");
        } catch (Exception e) {
            logger.error("❌ Error al eliminar tarea {}", id, e);
            return ApiResponses.internalError("Error interno al eliminar tarea");
        }
    }

    /**
     * Listar tareas del usuario autenticado con filtro de rango de fechas
     * También sirve como compatibilidad para /mias sin parámetros
     */
    @GetMapping("/mias")
    public ResponseEntity<?> listarTareasEnRango(
            @RequestParam(value = "fechaInicio", required = false) String fechaInicio,
            @RequestParam(value = "fechaFin", required = false) String fechaFin,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            if (authorizationHeader == null || authorizationHeader.isBlank()) {
                return ApiResponses.unauthorized("Authorization header ausente");
            }
            String token = authorizationHeader.startsWith("Bearer ") ? authorizationHeader.substring(7) : authorizationHeader;
            String correo = jwtUtil.extraerCorreoDesdeToken(token);
            if (correo == null || correo.isBlank()) {
                return ApiResponses.unauthorized("Token inválido o sin correo");
            }

            // Si hay filtro de fechas, aplicarlo
            if (fechaInicio != null && !fechaInicio.isBlank() && fechaFin != null && !fechaFin.isBlank()) {
                try {
                    LocalDateTime inicio = LocalDateTime.parse(fechaInicio + "T00:00:00");
                    LocalDateTime fin = LocalDateTime.parse(fechaFin + "T23:59:59");
                    List<TareaDTO> tareasDTO = tareaService.listarPorCorreoYRangoFechas(correo, inicio, fin);
                    return ApiResponses.ok(tareasDTO);
                } catch (DateTimeParseException ex) {
                    return ApiResponses.badRequest("Formato de fecha inválido. Usa yyyy-MM-dd");
                }
            } else {
                // Sin filtro de fechas, listar todas
                List<TareaDTO> tareasDTO = tareaService.listarPorCorreoColaborador(correo);
                return ApiResponses.ok(tareasDTO);
            }
        } catch (Exception e) {
            logger.error("❌ Error al listar tareas del usuario autenticado", e);
            return ApiResponses.internalError("Error interno al listar mis tareas");
        }
    }
}
