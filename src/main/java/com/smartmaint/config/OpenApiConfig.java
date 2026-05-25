package com.smartmaint.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartMaintOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("SmartMaint API")
                .description("API REST para gestión de mantenimiento, tareas, usuarios, roles, notificaciones y demos institucionales.")
                .version("v1")
                .contact(new Contact()
                    .name("SmartMaint")
                    .email("smartmaint.co@outlook.com"))
                .license(new License()
                    .name("Uso interno SmartMaint")));
    }
}