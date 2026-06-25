package com.sistemaventas.sistema_ventas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 0. CSRF BLINDADO: Usamos la importación estática de forma directa para activar la protección global
                .csrf(withDefaults())

                // 1. HEADERS: Permite el uso de modales del mismo origen (sameOrigin) sin romper el diseño visual
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                )

                // 2. AUTORIZACIÓN: Control estricto de accesos por rutas y roles
                .authorizeHttpRequests(auth -> auth
                        // Recursos estáticos y pantalla de inicio de sesión accesibles para todos
                        .requestMatchers("/login", "/css/**", "/js/**", "/img/**", "/webjars/**").permitAll()

                        // Gestión de perfil propia accesible por cualquier usuario autenticado
                        .requestMatchers("/usuarios/perfil", "/usuarios/perfil/cambiar-password").authenticated()

                        // Restringimos toda la gestión de usuarios únicamente al rol ADMINISTRADOR
                        .requestMatchers("/usuarios", "/usuarios/**").hasRole("ADMIN")

                        // Cualquier otra sección del sistema (Ventas, Compras, Categorías, Almacén) requiere autenticación
                        .anyRequest().authenticated()
                )

                // 3. LOGIN: Control de acceso por formulario
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/home", true) // Redirección forzosa al Home tras un login exitoso
                        .permitAll()
                )

                // 4. LOGOUT: Destrucción absoluta de la sesión en el cliente y servidor
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )

                // 5. GESTIÓN DE SESIONES: Previene ataques de fijación y limita a 1 sesión activa por cuenta
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .maximumSessions(1)
                );

        return http.build();
    }
}