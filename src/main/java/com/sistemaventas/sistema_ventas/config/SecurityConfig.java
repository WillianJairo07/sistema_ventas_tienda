package com.sistemaventas.sistema_ventas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Importante: Las contraseñas en la DB deben estar encriptadas con BCrypt
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http

                // 1. PRIMERO: Deshabilitar caché (Debe ir antes de authorizeHttpRequests)
                .headers(headers -> headers
                        .cacheControl(cache -> cache.disable())
                        .frameOptions(frame -> frame.sameOrigin()) // Opcional: para seguridad de marcos
                )
                // 2. SEGUNDO: Autorización
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/img/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 3. TERCERO: Login
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/home", true) // El 'true' fuerza a que siempre vaya a home tras login
                        .permitAll()
                )
                // 4. CUARTO: Logout (Aquí está el refuerzo)
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)    // Mata la sesión en servidor
                        .clearAuthentication(true)      // Borra los datos del usuario actual
                        .deleteCookies("JSESSIONID", "remember-me") // Borra las cookies
                        .permitAll()
                )
                // 5. QUINTO: Control de Sesión (Evita que sesiones viejas revivan)
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession() // Protege contra fijación de sesión
                        .maximumSessions(1) // Solo una sesión a la vez
                );

        return http.build();
    }
}
