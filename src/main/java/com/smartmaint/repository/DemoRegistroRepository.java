package com.smartmaint.repository;

import com.smartmaint.model.DemoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface DemoRegistroRepository extends JpaRepository<DemoRegistro, Long> {

    boolean existsByIpSolicitanteAndFechaSolicitudAfter(String ipSolicitante, LocalDateTime fechaSolicitud);
}
