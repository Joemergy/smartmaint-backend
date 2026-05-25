package com.smartmaint.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/archivos")
public class ArchivoController {

    private static final Logger logger = LoggerFactory.getLogger(ArchivoController.class);
    private static final Path UPLOAD_DIR = Paths.get("uploads", "tareas");

    /**
     * Sirve un archivo adjunto de tarea por su nombre único.
     * El nombre incluye un UUID prefix que evita colisiones y enumeración.
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        // Prevenir path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            logger.warn("⚠️ Intento de path traversal bloqueado: {}", filename);
            return ResponseEntity.badRequest().build();
        }

        try {
            Path filePath = UPLOAD_DIR.resolve(filename).normalize();

            // Verificar que el archivo realmente está dentro del directorio de uploads
            if (!filePath.startsWith(UPLOAD_DIR.toAbsolutePath())) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new FileSystemResource(filePath);
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception e) {
            logger.error("❌ Error al servir archivo: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
