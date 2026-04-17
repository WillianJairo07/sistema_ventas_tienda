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
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 0. CSRF: Ignorar solo rutas específicas de APIs o guardados rápidos sin token
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/clientes/guardar-rapido")
                )

                // 1. HEADERS: Mantenemos frameOptions para permitir modales/iframes del mismo origen
                .headers(headers -> headers
                                .frameOptions(frame -> frame.sameOrigin())
                )

                // 2. AUTORIZACIÓN: Rutas públicas y protegidas
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/img/**", "/webjars/**").permitAll()
                        .requestMatchers("/clientes/guardar-rapido").authenticated()
                        .anyRequest().authenticated()
                )

                // 3. LOGIN: Configuración de entrada
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/home", true) // 'true' obliga a ir al home tras loguear
                        .permitAll()
                )

                // 4. LOGOUT: Limpieza completa de sesión
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )

                // 5. SESIÓN: Evita ataques de fijación de sesión y limita a 1 sesión activa
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .maximumSessions(1)
                );

        return http.build();
    }
}