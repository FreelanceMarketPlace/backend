package com.nhom611.jobsvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
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

						// Public browsing
						.requestMatchers(HttpMethod.GET, "/jobs/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/offers/**").permitAll()

						// Employer-protected actions
						.requestMatchers(HttpMethod.GET, "/employer/jobs/**").hasRole("EMPLOYER")
						.requestMatchers(HttpMethod.POST, "/jobs").hasRole("EMPLOYER")
						.requestMatchers(HttpMethod.PATCH, "/jobs/**").hasRole("EMPLOYER")
						.requestMatchers(HttpMethod.POST, "/jobs/*/close").hasRole("EMPLOYER")

						.anyRequest().authenticated()
				)
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
				);

		return http.build();
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwt -> {
			Object roleClaim = jwt.getClaims().get("role");
			if (roleClaim instanceof String role && !role.isBlank()) {
				return List.of(new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase()));
			}
			return List.of();
		});
		return converter;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(AuthProperties properties) {
		List<String> allowedOrigins = properties.getCors().getAllowedOrigins();
		if (allowedOrigins == null || allowedOrigins.isEmpty()) {
			allowedOrigins = List.of("http://localhost:5173");
		}

		CorsConfiguration cfg = new CorsConfiguration();
		cfg.setAllowedOrigins(allowedOrigins);
		cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"));
		cfg.setAllowCredentials(true);
		cfg.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", cfg);
		return source;
	}
}

