package com.smartmaint.repository;

import com.smartmaint.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findByCorreo(String correo);

    boolean existsByNombre(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    Optional<Empresa> findByNombre(String nombre);

    Optional<Empresa> findByNombreIgnoreCase(String nombre);

    Optional<Empresa> findByToken(String token);
}
