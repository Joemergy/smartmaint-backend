package com.smartmaint.service;

import com.smartmaint.dto.EquipoCreateDTO;
import com.smartmaint.dto.EquipoDTO;
import com.smartmaint.model.Equipo;
import com.smartmaint.model.Usuario;
import com.smartmaint.repository.EquipoRepository;
import com.smartmaint.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EquipoService {

    private final EquipoRepository equipoRepository;
    private final UsuarioRepository usuarioRepository;

    public EquipoService(EquipoRepository equipoRepository, UsuarioRepository usuarioRepository) {
        this.equipoRepository = equipoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public EquipoDTO crearEquipo(EquipoCreateDTO dto, String correoUsuario) {
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado o sin empresa asignada"));

        if (usuario.getEmpresa() == null) {
            throw new IllegalArgumentException("Usuario sin empresa asignada");
        }
        if (dto.getNombre() == null || dto.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del equipo es obligatorio");
        }

        equipoRepository.findByEmpresaIdAndNombreIgnoreCase(usuario.getEmpresa().getId(), dto.getNombre().trim())
                .ifPresent(e -> { throw new IllegalArgumentException("Ya existe un equipo con ese nombre en tu empresa"); });

        Equipo equipo = new Equipo();
        equipo.setNombre(dto.getNombre().trim());
        equipo.setTipo(dto.getTipo());
        equipo.setUbicacion(dto.getUbicacion());
        equipo.setCategoria(dto.getCategoria());
        equipo.setDescripcion(dto.getDescripcion());
        equipo.setEmpresa(usuario.getEmpresa());

        equipo = equipoRepository.save(equipo);
        return EquipoDTO.fromEntity(equipo);
    }

    public List<EquipoDTO> listarEquipos(String correoUsuario) {
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado o sin empresa asignada"));

        if (usuario.getEmpresa() == null) {
            throw new IllegalArgumentException("Usuario sin empresa asignada");
        }

        return equipoRepository.findByEmpresaId(usuario.getEmpresa().getId())
                .stream()
                .map(EquipoDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
