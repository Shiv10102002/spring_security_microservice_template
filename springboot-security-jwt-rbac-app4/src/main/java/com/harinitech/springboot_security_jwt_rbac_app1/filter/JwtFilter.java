package com.harinitech.springboot_security_jwt_rbac_app1.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.harinitech.springboot_security_jwt_rbac_app1.entity.User;
import com.harinitech.springboot_security_jwt_rbac_app1.entity.UserToken;
import com.harinitech.springboot_security_jwt_rbac_app1.repo.UserRepository;
import com.harinitech.springboot_security_jwt_rbac_app1.repo.UserTokenRepository;
import com.harinitech.springboot_security_jwt_rbac_app1.utility.JwtUtility;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {

	private final JwtUtility jwtUtility;
	private final UserTokenRepository userTokenRepository;
	private final UserRepository userRepository;

	public JwtFilter(JwtUtility jwtUtility, UserTokenRepository userTokenRepository, UserRepository userRepository) {
		this.jwtUtility = jwtUtility;
		this.userTokenRepository = userTokenRepository;
		this.userRepository = userRepository;
	}

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		final String authHeader = request.getHeader("Authorization");

		// 1. No token → continue chain
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		final String token = authHeader.substring(7);

		try {

			// 2. Clear previous context (important in thread reuse)
			SecurityContextHolder.clearContext();

			// 3. JWT validation (cryptographic check)
			if (!jwtUtility.isTokenValid(token)) {

				userTokenRepository.findByAccessToken(token).ifPresent(dbToken -> {

					if (!dbToken.isExpired()) {

						dbToken.setExpired(true);

						userTokenRepository.save(dbToken);

						log.info("TOKEN AUTO-EXPIRED | tokenId={}", dbToken.getId());
					}
				});

				SecurityContextHolder.clearContext();

				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

				return;
			}

			// ✅ PRODUCTION SECURITY
			// ONLY ACCESS TOKENS CAN ACCESS APIs

			String tokenType = jwtUtility.extractTokenType(token);

			if (!"ACCESS".equals(tokenType)) {

				log.warn("NON-ACCESS TOKEN USED FOR API ACCESS");

				SecurityContextHolder.clearContext();

				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

				return;
			}

			// 4. DB validation (session control)
			UserToken dbToken = userTokenRepository.findByAccessToken(token).orElse(null);

			if (dbToken == null || dbToken.isRevoked() || dbToken.isExpired()) {
				SecurityContextHolder.clearContext();
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			// 5. DB expiry check (critical fix for timing mismatch)
			if (dbToken.getAccessExpiry() != null && dbToken.getAccessExpiry().isBefore(java.time.Instant.now())) {

				dbToken.setExpired(true);
				userTokenRepository.save(dbToken);

				SecurityContextHolder.clearContext();
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

			// 6. Extract identity from JWT
			Long userId = jwtUtility.extractUserId(token);
			String role = jwtUtility.extractRole(token);
			List<String> permissions = jwtUtility.extractPermissions(token);

			// strict validation
			if (userId == null || role == null) {
				SecurityContextHolder.clearContext();
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}

//			here i added a new latest logic related to user can access only if user creates a new password and login only.
			// ================= FORCE PASSWORD CHANGE CHECK =================

			User user = userRepository.findById(userId).orElse(null);

			if (user == null) {

				SecurityContextHolder.clearContext();

				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

				return;
			}

			String requestPath = request.getRequestURI();

			boolean allowedPath = requestPath.startsWith("/api/v1/users/change-password")
					|| requestPath.startsWith("/api/v1/auth/logout")
					|| requestPath.startsWith("/api/v1/auth/logout-all");

			if (user.isForcePasswordChange() && !allowedPath) {

				log.warn("FORCE PASSWORD CHANGE REQUIRED | userId={}", userId);

				SecurityContextHolder.clearContext();

				response.setStatus(HttpServletResponse.SC_FORBIDDEN);

				response.setContentType("application/json");

				response.getWriter().write("""
						{
						  "message":"Password change required",
						  "forcePasswordChange":true
						}
						""");

				return;
			}

			// 7. Build authorities
			List<SimpleGrantedAuthority> authorities = new ArrayList<>();
			authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));

			if (permissions != null && !permissions.isEmpty()) {
				permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
			}

			// 8. Set authentication
			UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userId.toString(),
					null, authorities);

			authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authToken);

			log.info("JWT AUTH SUCCESS | userId={} | role={}", userId, role);

		} catch (Exception ex) {

			SecurityContextHolder.clearContext();

			log.error("JWT PROCESSING FAILED | error={}", ex.getMessage());

			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		filterChain.doFilter(request, response);
	}

	/*
	 * 1. JWT validation 2. tokenType validation 3. DB token validation 4. expiry
	 * validation 5. extract userId 6. strict validation 7. force password change
	 * validation ✅ NEW 8. build authorities 9. set authentication
	 */
}