package com.smartmaint.config;

import com.smartmaint.util.JwtFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log =
            LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return http

                .authorizeHttpRequests(auth -> auth

                        // AUTH
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/recuperar-contrasena").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()

                        // EMPRESAS
                        .requestMatchers(HttpMethod.POST, "/api/empresas").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/empresas/planes/compra").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/empresas/activar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/empresas/validar-id/**").permitAll()

                        // DEMOS
                        .requestMatchers(HttpMethod.POST, "/api/demo/solicitar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/demo/listar")
                        .hasAnyRole("ADMIN", "SUPERADMIN")

                        // SISTEMA
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/test-db").permitAll()

                        // ARCHIVOS
                        .requestMatchers(HttpMethod.GET, "/api/archivos/**").permitAll()

                        // TAREAS USUARIO
                        .requestMatchers(HttpMethod.GET, "/api/tareas/mias").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tareas/mias/compat").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tareas/asignadas").authenticated()

                        // TAREAS ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/tareas/admin")
                        .hasAnyRole("ADMIN", "SUPERADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/tareas/*/notas").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/tareas/*/nota").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/tareas/*/notas").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tareas/*").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/tareas/*/estado").authenticated()

                        // NOTIFICACIONES
                        .requestMatchers(HttpMethod.GET, "/api/notificaciones/mias").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/notificaciones/*/leer").authenticated()

                        // CONTRASEÑA
                        .requestMatchers(HttpMethod.PUT,
                                "/api/auth/cambiar-contrasena-inicial")
                        .authenticated()

                        // ADMIN / SUPERADMIN
                        .requestMatchers("/api/usuarios/**")
                        .hasAnyRole("ADMIN", "SUPERADMIN")

                        .requestMatchers("/api/equipos/**")
                        .hasAnyRole("ADMIN", "SUPERADMIN")

                        .requestMatchers("/api/tareas/**")
                        .hasAnyRole("ADMIN", "SUPERADMIN")

                        .requestMatchers("/api/roles/**")
                        .hasAnyRole("ADMIN", "SUPERADMIN")

                        // EMPRESA / PERFIL
                        .requestMatchers("/api/empresas/**").authenticated()
                        .requestMatchers("/api/perfil").authenticated()

                        .anyRequest().authenticated()
                )

                .csrf(csrf -> csrf.disable())

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                SecurityExceptionHandlers.authenticationEntryPoint()
                        )
                        .accessDeniedHandler(
                                SecurityExceptionHandlers.accessDeniedHandler()
                        )
                )

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of(

                // LOCAL
                "http://localhost:3000",
                "http://127.0.0.1:5500",
                "http://localhost:5500",

                // VERCEL PRODUCCIÓN
                "https://smartmaint-frontend-last.vercel.app",

                // previews futuras de vercel
                "https://*.vercel.app"
        ));

        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "PATCH"
        ));

        config.setExposedHeaders(List.of(
                "Authorization",
                "Content-Type"
        ));

        config.setAllowCredentials(true);

        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        log.info(
                "CORS configurado correctamente para SmartMaint frontend y entorno local"
        );

        return source;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {

        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) corsConfigurationSource();

        CorsFilter corsFilter = new CorsFilter(source);

        FilterRegistrationBean<CorsFilter> registration =
                new FilterRegistrationBean<>(corsFilter);

        registration.setOrder(0);

        registration.addUrlPatterns("/*");

        return registration;
    }
}