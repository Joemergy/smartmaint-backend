package com.smartmaint.repository;

import com.smartmaint.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    // Buscamos rol por su nombre de forma case-insensitive
    Optional<Rol> findByNombreIgnoreCase(String nombre);

    default Optional<Rol> findByNombre(String nombre) {
        return findByNombreIgnoreCase(nombre);
    }

    // Ejemplo de consulta adicional:
    // List<Rol> findByCreatedAtBefore(LocalDateTime fecha);
}
