package dev.barboud.taglens.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/register").permitAll()                 // login & register: public
                        .anyRequest().authenticated()                            // everything else: must be logged in
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use the BCrypt hashing algorithm
        return new BCryptPasswordEncoder();
    }
}
