package com.smartmaint.repository;

import com.smartmaint.model.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

public interface TareaRepository extends JpaRepository<Tarea, Long> {

    // Buscar tareas por estado (PENDIENTE, EN_PROCESO, TERMINADO)
    List<Tarea> findByEstado(String estado);

    // Buscar tareas por ID del usuario asignado (relación ManyToOne con Usuario)
    List<Tarea> findByUsuarioAsignado_Id(Long usuarioId);

    // Buscar tareas por empresa (para filtrar por empresa del usuario)
    @Query("SELECT t FROM Tarea t WHERE t.usuarioAsignado IS NOT NULL AND t.usuarioAsignado.empresa.id = :empresaId")
    List<Tarea> findByEmpresaId(@Param("empresaId") Long empresaId);

// ✅ Buscar tareas por rango de fecha de inicio
List<Tarea> findByFechaInicioBetween(LocalDateTime inicio, LocalDateTime fin);

// ✅ Buscar tareas por rango de fecha de cierre
List<Tarea> findByFechaCierreBetween(LocalDateTime inicio, LocalDateTime fin);


    // Buscar tareas por prioridad
    List<Tarea> findByPrioridad(String prioridad);

    // Buscar tareas por categoría
    List<Tarea> findByCategoria(String categoria);

    // 🔧 Nuevo: buscar tareas por correo del usuario asignado
    List<Tarea> findByUsuarioAsignado_Correo(String correo);

    // Buscar tareas por correo del colaborador
    List<Tarea> findByCorreoColaborador(String correoColaborador);

    // Buscar tareas por correo del colaborador dentro de un rango de fechas
    List<Tarea> findByCorreoColaboradorAndFechaInicioBetween(String correoColaborador, LocalDateTime fechaInicio, LocalDateTime fechaFin);
}
