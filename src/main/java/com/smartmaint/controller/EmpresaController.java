package com.smartmaint.controller;

import com.smartmaint.api.ApiResponses;
import com.smartmaint.dto.EmpresaDTO;
import com.smartmaint.dto.PlanCompraRequestDTO;
import com.smartmaint.service.EmpresaService;
import com.smartmaint.repository.EmpresaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private static final Logger logger = LoggerFactory.getLogger(EmpresaController.class);

    @Autowired
    private EmpresaService empresaService;

    @Autowired
    private EmpresaRepository empresaRepository;

    /**
     * POST /api/empresas/planes/compra
     * Registrar compra de suscripción y crear SuperAdmin
     */
    @PostMapping("/planes/compra")
    public ResponseEntity<?> registrarCompra(@Valid @RequestBody PlanCompraRequestDTO body) {
        try {
            logger.info("📥 Compra recibida: {}", body);
            return ApiResponses.ok(empresaService.registrarCompra(body));
        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ Validación de compra fallida: {}", e.getMessage());
            return ApiResponses.badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Error al registrar compra: {}", e.getMessage(), e);
            return ApiResponses.internalError("Error interno al registrar compra");
        }
    }

    /**
     * GET /api/empresas/activar?token=XYZ123
     * Validar token de activación
     */
    @GetMapping("/activar")
    public ResponseEntity<?> activarEmpresa(@RequestParam String token) {
        try {
            boolean activada = empresaService.activarEmpresa(token);
            if (!activada) {
                return ApiResponses.notFound("Token inválido o empresa no encontrada");
            }
            return ApiResponses.ok(Map.of("mensaje", "Empresa activada correctamente", "token", token));
        } catch (Exception e) {
            logger.error("❌ Error al activar empresa: {}", e.getMessage(), e);
            return ApiResponses.internalError("Error interno al activar empresa");
        }
    }

    /**
     * POST /api/empresas
     * Completar registro de empresa y crear primer ADMIN
     */
    @PostMapping
    public ResponseEntity<?> registrarEmpresa(@Valid @RequestBody EmpresaDTO dto) {
        try {
            logger.info("📥 Datos recibidos desde frontend: {}", dto);
            return ApiResponses.created(empresaService.registrarEmpresa(dto));
        } catch (Exception e) {
            logger.error("❌ Error interno: {}", e.getMessage(), e);
            return ApiResponses.internalError("Error interno en el servidor");
        }
    }

    /**
     * GET /api/empresas
     * Listar todas las empresas (solo datos de empresa, sin usuarios)
     */
    @GetMapping
    public ResponseEntity<List<EmpresaDTO>> listarEmpresas() {
        return ResponseEntity.ok(empresaService.listarEmpresas());
    }

    /**
     * GET /api/empresas/validar-id/{id_empresa}
     * Validar si un ID de empresa existe en la BD
     */
    @GetMapping("/validar-id/{id_empresa}")
    public ResponseEntity<?> validarIdEmpresa(@PathVariable Long id_empresa) {
        boolean existe = empresaRepository.existsById(id_empresa);
        logger.info("🔍 Validación de ID: {} → existe: {}", id_empresa, existe);
        return ApiResponses.ok(Map.of("idEmpresa", id_empresa, "disponible", existe));
    }
}
