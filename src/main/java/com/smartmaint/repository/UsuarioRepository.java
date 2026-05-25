package com.smartmaint.repository;

import com.smartmaint.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("SELECT u FROM Usuario u WHERE LOWER(u.correo) = LOWER(:correo)")
    Optional<Usuario> findByCorreo(@Param("correo") String correo);

    @Query("SELECT u FROM Usuario u WHERE LOWER(u.idColaborador) = LOWER(:idColaborador)")
    Optional<Usuario> findByIdColaborador(@Param("idColaborador") String idColaborador);

    List<Usuario> findByDemoTrue();
    List<Usuario> findByDemoTrueAndExpiraEnBefore(LocalDateTime fecha);

    List<Usuario> findByEmpresaId(Long empresaId);

    List<Usuario> findByEmpresaIdAndRolNombreIgnoreCaseOrderByCreatedAtAsc(Long empresaId, String rolNombre);
}
