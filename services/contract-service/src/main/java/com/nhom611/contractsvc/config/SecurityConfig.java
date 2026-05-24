package com.nhom611.contractsvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.time.Instant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Bean;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        // Allow public read access to contracts/milestones for now
                        .requestMatchers(HttpMethod.GET, "/api/contracts/**").permitAll()
                    .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                ;

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // Allow frontend dev origin and allow Authorization header for authenticated flows
        cfg.setAllowedOriginPatterns(List.of("http://localhost:5173", "http://localhost:5174", "*"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    // Dev-friendly JwtDecoder: decode token payload without signature verification.
    // NOT FOR PRODUCTION. This allows frontend dev JWTs to be read for local testing.
    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        return token -> {
            try {
                String[] parts = token.split("\\.");
                if (parts.length < 2) throw new JwtException("Invalid JWT token")
;
                String payload = parts[1];
                byte[] decoded = Base64.getUrlDecoder().decode(payload);
                ObjectMapper om = new ObjectMapper();
                Map<String, Object> claims = om.readValue(decoded, new TypeReference<Map<String, Object>>() {});
                Map<String, Object> headers = new HashMap<>();
                headers.put("alg", "none");
                Instant now = Instant.now();
                Jwt jwt = new Jwt(token, now.minusSeconds(60), now.plusSeconds(3600), headers, claims);
                return jwt;
            } catch (Exception ex) {
                throw new JwtException("Failed to decode JWT", ex);
            }
        };
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
