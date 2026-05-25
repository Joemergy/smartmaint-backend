package com.smartmaint.service;

import com.smartmaint.model.Rol;
import com.smartmaint.repository.RolRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RolService {

    private final RolRepository rolRepository;

    public RolService(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    /**
     * Buscar rol por nombre (ej: ADMIN, TECNICO)
     */
    public Rol obtenerPorNombre(String nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + nombre));
    }

    /**
     * Listar todos los roles disponibles
     */
    public List<Rol> listarTodos() {
        return rolRepository.findAll();
    }

    /**
     * Crear un nuevo rol
     */
    public Rol crearRol(Rol rol) {
        if (rol.getNombre() == null || rol.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio");
        }
        return rolRepository.save(rol);
    }

    /**
     * Eliminar rol por ID
     */
    public void eliminarRol(Long id) {
        rolRepository.deleteById(id);
    }
}
