package com.smartmaint.config;

import com.smartmaint.util.JwtFilter;
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
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

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
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/recuperar-contrasena").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/empresas").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/empresas/planes/compra").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/empresas/activar").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/empresas/validar-id/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/demo/solicitar").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/demo/listar").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/test-db").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/archivos/**").permitAll()

                // Usuarios autenticados pueden ver y actualizar sus propias tareas
                .requestMatchers(HttpMethod.GET, "/api/tareas/mias").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/tareas/mias/compat").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/tareas/asignadas").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/tareas/admin").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers(HttpMethod.GET, "/api/tareas/*/notas").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/tareas/*/nota").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/tareas/*/notas").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/tareas/*").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/tareas/*/estado").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/notificaciones/mias").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/notificaciones/*/leer").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/auth/cambiar-contrasena-inicial").authenticated()

                // Solo ADMIN/SUPERADMIN pueden hacer el resto
                .requestMatchers("/api/usuarios/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers("/api/equipos/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers("/api/tareas/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers("/api/roles/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers("/api/empresas/**").authenticated()
                .requestMatchers("/api/perfil").authenticated()
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(SecurityExceptionHandlers.authenticationEntryPoint())
                .accessDeniedHandler(SecurityExceptionHandlers.accessDeniedHandler())
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",
            "http://127.0.0.1:5500",
            "http://localhost:5500"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) corsConfigurationSource();
        CorsFilter corsFilter = new CorsFilter(source);
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(corsFilter);
        registration.setOrder(0); // very high precedence
        registration.addUrlPatterns("/*");
        return registration;
    }
}