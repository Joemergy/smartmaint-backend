package com.smartmaint.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping
    public ResponseEntity<?> ping() {
        LocalDateTime ahora = LocalDateTime.now();
        System.out.println("📡 TestController: ping recibido → " + ahora);

        Map<String, Object> estado = Map.of(
            "mensaje", "Smartmaint backend activo",
            "timestamp", ahora,
            "status", "OK"
        );

        return ResponseEntity.ok(estado);
    }
}
