package com.harinitech.springboot_security_jwt_rbac_app1.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.harinitech.springboot_security_jwt_rbac_app1.filter.JwtFilter;
import com.harinitech.springboot_security_jwt_rbac_app1.service.IAuthService;

@Configuration
@EnableMethodSecurity // enables @PreAuthorize / @PostAuthorize on methods
public class SecurityConfig {

	// ── DEPENDENCIES ──────────────────────────────────────────────────────────

	private final JwtFilter jwtFilter;
	private final IAuthService authService; // @Lazy — breaks circular dependency
	private final PasswordEncoder passwordEncoder;

	/**
	 * Explicit constructor with @Lazy on IAuthService. IAuthService →
	 * AuthenticationManager → SecurityConfig → IAuthService (cycle).
	 * 
	 * @Lazy breaks the cycle by deferring proxy creation until first use.
	 */
	public SecurityConfig(JwtFilter jwtFilter, @Lazy IAuthService authService, PasswordEncoder passwordEncoder) {
		this.jwtFilter = jwtFilter;
		this.authService = authService;
		this.passwordEncoder = passwordEncoder;
	}

	// ── PUBLIC ENDPOINT LISTS (single place to maintain) ─────────────────────

	/**
	 * Auth endpoints that require NO token. login, refresh-token, validate-token
	 * are public by design. logout / logout-all / sessions are intentionally NOT
	 * here — they need a token.
	 */
	private static final String[] PUBLIC_AUTH_MATCHERS = { "/api/v1/auth/login", "/api/v1/auth/refresh-token",
			"/api/v1/auth/validate-token" };

	/**
	 * User endpoints that require NO token (self-service flows before login).
	 */
	/**
	 * User endpoints that require NO token (self-service flows before login).
	 */
	private static final String[] PUBLIC_USER_MATCHERS = { "/api/v1/users/send-registration-otp",
			"/api/v1/users/register", "/api/v1/users/employee-register", "/api/v1/users/forgot-password",
			"/api/v1/users/reset-password" };

	/**
	 * Swagger / OpenAPI UI endpoints.
	 */
	private static final String[] SWAGGER_MATCHERS = { "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
			"/webjars/**" };

	// ── CORS ALLOWED ORIGINS (update per environment) ─────────────────────────

	// ✅ FIXED: Made CORS origins configurable via application.properties
	// Fallback to localhost for development, should be overridden in production
	@Value("${allowed.origins:http://localhost:4200,http://localhost:3000}")
	private List<String> corsAllowedOrigins;

	// ════════════════════════════════════════════════════════════════════════════
	// SECURITY FILTER CHAIN
	// ════════════════════════════════════════════════════════════════════════════

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http
				// ── CSRF: disabled — stateless JWT API does not use cookies ──────
				.csrf(AbstractHttpConfigurer::disable)

				// ── CORS: centralised config (replaces @CrossOrigin on controllers)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))

				// ── SESSION: stateless — no HttpSession created or used ──────────
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// ── SECURITY HEADERS ─────────────────────────────────────────────
				.headers(headers -> headers.contentTypeOptions(contentType -> {
				}) // X-Content-Type-Options: nosniff
						.frameOptions(frame -> frame.deny()) // X-Frame-Options: DENY
						.referrerPolicy(
								referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))

				// ── ENDPOINT ACCESS RULES ────────────────────────────────────────
				.authorizeHttpRequests(auth -> auth

						// ✅ Truly public endpoints — no token required
						.requestMatchers(PUBLIC_AUTH_MATCHERS).permitAll().requestMatchers(PUBLIC_USER_MATCHERS)
						.permitAll().requestMatchers(SWAGGER_MATCHERS).permitAll().requestMatchers("/actuator/health")
						.permitAll().requestMatchers("/actuator/**").hasRole("ADMIN")

						// ✅ Preflight OPTIONS requests — always allow (for CORS)
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

						// ✅ Everything else requires a valid JWT
						.anyRequest().authenticated())

				// ── AUTH PROVIDER + JWT FILTER ───────────────────────────────────
				.authenticationProvider(authenticationProvider())
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	// ════════════════════════════════════════════════════════════════════════════
	// CORS CONFIGURATION
	// ════════════════════════════════════════════════════════════════════════════

	/**
	 * Centralised CORS config — replaces @CrossOrigin on individual controllers.
	 *
	 * TEMPLATE NOTE: update CORS_ALLOWED_ORIGINS list above for your frontend URLs.
	 * In production, never use "*" for allowedOrigins when allowCredentials is
	 * true.
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		// ✅ FIXED: Use configurable CORS origins from application.properties
		config.setAllowedOrigins(corsAllowedOrigins);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
		config.setExposedHeaders(List.of("Authorization")); // allow frontend to read Authorization header
		config.setAllowCredentials(true);
		config.setMaxAge(3600L); // preflight cache: 1 hour

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	// ════════════════════════════════════════════════════════════════════════════
	// AUTH PROVIDER
	// ════════════════════════════════════════════════════════════════════════════

	/**
	 * DaoAuthenticationProvider wires UserDetailsService + PasswordEncoder. Called
	 * only during login credential verification — not for JWT validation.
	 */
	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(authService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	// ════════════════════════════════════════════════════════════════════════════
	// AUTH MANAGER
	// ════════════════════════════════════════════════════════════════════════════

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	// ════════════════════════════════════════════════════════════════════════════
	// AUDITOR
	// ════════════════════════════════════════════════════════════════════════════

	/**
	 * JPA Auditing — records createdBy / updatedBy as userId (String). Consistent
	 * with the JWT principal contract (userId as principal).
	 */
	@Bean
	public AuditorAware<Long> auditorProvider() {
		return new AuditorAwareImpl();
	}
}