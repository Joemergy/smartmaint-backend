package com.smartmaint.repository;

import com.smartmaint.model.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    List<Equipo> findByEmpresaId(Long empresaId);

    Optional<Equipo> findByEmpresaIdAndNombreIgnoreCase(Long empresaId, String nombre);
}
