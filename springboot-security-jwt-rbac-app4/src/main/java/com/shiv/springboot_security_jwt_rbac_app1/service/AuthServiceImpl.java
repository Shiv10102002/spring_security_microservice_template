package com.shiv.springboot_security_jwt_rbac_app1.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shiv.springboot_security_jwt_rbac_app1.entity.User;
import com.shiv.springboot_security_jwt_rbac_app1.entity.UserToken;
import com.shiv.springboot_security_jwt_rbac_app1.model.AuditAction;
import com.shiv.springboot_security_jwt_rbac_app1.model.AuditStatus;
import com.shiv.springboot_security_jwt_rbac_app1.model.JwtRequest;
import com.shiv.springboot_security_jwt_rbac_app1.model.JwtResponse;
import com.shiv.springboot_security_jwt_rbac_app1.model.Status;
import com.shiv.springboot_security_jwt_rbac_app1.repo.UserRepository;
import com.shiv.springboot_security_jwt_rbac_app1.repo.UserTokenRepository;
import com.shiv.springboot_security_jwt_rbac_app1.utility.JwtUtility;
import com.shiv.springboot_security_jwt_rbac_app1.utility.RequestInfoUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * ✅ PRODUCTION AuthServiceImpl — fully userId-based.
 *
 * Design contract (must be consistent across ALL services):
 * ────────────────────────────────────────────────────────── JWT subject =
 * userId (Long as String) SecurityContext principal = userId (Long as String)
 * getCurrentUser() = parse principal → Long → userRepository.findById()
 *
 * This is username-change-safe: User changes email → token still resolves via
 * userId → no forced logout.
 *
 * loadUserByUsername() is ONLY called by Spring Security's
 * AuthenticationManager during login credential check — not used for principal
 * resolution.
 */

@Slf4j
@Service
public class AuthServiceImpl implements IAuthService {

	// ======================== DEPENDENCIES ========================

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserTokenRepository userTokenRepository;

	@Autowired
	private JwtUtility jwtUtility;

	@Autowired
	@Lazy
	private AuthenticationManager authenticationManager;

	@Autowired
	private AuditService auditService;

	@Autowired
	private LoginAttemptService loginAttemptService;

	@Value("${security.account.lock-duration-ms}")
	private long lockDuration;

	@Value("${security.account.max-login-attempts}")
	private int maxLoginAttempts;

	@Value("${jwt.access-token-expiration-ms}")
	private long accessTokenExpiry;

	@Value("${jwt.refresh-token-expiration-ms}")
	private long refreshTokenExpiry;

	// ======================== 🔐 LOGIN ========================
	@Transactional
	@Override
	public JwtResponse authenticateUser(JwtRequest request, HttpServletRequest httpRequest) {

//		initial null checks
		if (request == null) {
			throw new RuntimeException("Login request cannot be null");
		}

		if (request.getUsername() == null || request.getUsername().isBlank()) {
			throw new RuntimeException("Username is required");
		}

		if (request.getPassword() == null || request.getPassword().isBlank()) {
			throw new RuntimeException("Password is required");
		}

		// Step 2: Load full user entity
		User user = getUserByUsername(request.getUsername());

		// ✅ Auto unlock if lock expired
		unlockIfLockExpired(user);

		// ✅ RELOAD USER FROM DB AFTER UNLOCK
		user = getUserByUsername(request.getUsername());

		// ✅ Block if still locked
		if (user.isAccountLocked()) {

			log.warn("LOGIN BLOCKED | ACCOUNT STILL LOCKED | username={}", request.getUsername());

			throw new RuntimeException("Your account is locked. Please try again later.");
		}
		// Step 1: Verify credentials via Spring Security (calls loadUserByUsername
		// internally)
		authenticate(request.getUsername(), request.getPassword());

		// reload latest DB state
		user = getUserByUsername(request.getUsername());

		// Step 3: Block inactive accounts before issuing tokens
		if (user.getStatus() != Status.ACTIVE) {
			throw new RuntimeException("Account is not active. Current status: " + user.getStatus());
		}

		if (!user.isEnabled()) {
			throw new RuntimeException("Your account is disabled. Please contact support.");
		}

		if (!user.isAccountNonLocked()) {
			throw new RuntimeException("Your account is locked due to multiple failed attempts.");
		}

//		if (user.isForcePasswordChange()) {
//			throw new RuntimeException("Password reset required. Please change your password before login.");
//		}

		// Step 4: Revoke existing tokens (single active session per device policy)
		revokeAllActiveTokens(user);

		// Step 5: Issue new token pair — ✅ uses userId, NOT username
		JwtResponse response = generateTokens(user, httpRequest);

		// ✅ ADD THIS BLOCK
		user.setLastLoginAt(Instant.now());
		user.setLastLoginIp(RequestInfoUtil.getClientIp(httpRequest));
		user.setLastLoginDevice(RequestInfoUtil.getDeviceInfo(httpRequest));
		userRepository.saveAndFlush(user);

		log.info("LOGIN SUCCESS | userId={} | role={} | ip={} | device={}", user.getId(), user.getRole().getName(),
				RequestInfoUtil.getClientIp(httpRequest), RequestInfoUtil.getDeviceInfo(httpRequest));

		auditService.log(AuditAction.LOGIN, AuditStatus.SUCCESS, "User logged in successfully", httpRequest);

		return response;
	}

	private void unlockIfLockExpired(User user) {

		if (user == null || user.getLockTime() == null) {
			return;
		}

		// ✅ unlock after configured duration
		if (Instant.now().isAfter(user.getLockTime().plusMillis(lockDuration))) {

			user.setAccountLocked(false);

			user.setFailedLoginAttempts(0);

			user.setLockTime(null);

			userRepository.saveAndFlush(user);

			log.info("ACCOUNT AUTO UNLOCKED | userId={}", user.getId());

			auditService.log(AuditAction.ACCOUNT_UNLOCKED, AuditStatus.SUCCESS,
					"Account automatically unlocked after lock duration expired", null);
		}
	}

	// ======================== 🔄 REFRESH TOKEN ========================

	@Transactional
	@Override
	public ResponseEntity<?> refreshToken(String refreshToken, HttpServletRequest request) {

		// Step 1: Look up token in DB
		UserToken token = userTokenRepository.findByRefreshToken(refreshToken).orElseThrow(
				() -> new RuntimeException("Refresh token not found. It may have already been rotated or deleted."));

		// Step 2: Check revoked/expired flags
		if (token.isRevoked() || token.isExpired() || token.isRefreshUsed()) {

			// 🔥 TOKEN REUSE DETECTED → SECURITY BREACH
			revokeAllActiveTokens(token.getUser());

			log.error("TOKEN REUSE DETECTED | userId={} | tokenId={}", token.getUser().getId(), token.getId());

			auditService.log(AuditAction.TOKEN_REUSE_ATTACK, AuditStatus.WARNING, "Refresh token reuse detected",
					request);

			throw new RuntimeException(
					"Security breach detected: Reused or invalid refresh token. All sessions terminated.");
		}

		// Step 3: Validate refresh token using JWT itself
		if (!jwtUtility.isTokenValid(refreshToken)) {

			token.setExpired(true);
			userTokenRepository.save(token);

			throw new RuntimeException("Refresh token expired. Please login again.");
		}

		// Step 4: Build new token pair — ✅ uses userId
		User user = token.getUser();
		Set<String> permissions = user.getRole().getPermissions().stream().map(p -> p.getName())
				.collect(Collectors.toSet());

		// ✅ Pass userId to token generation — not username
		String newAccessToken = jwtUtility.generateAccessToken(user.getId(), user.getRole().getName(), permissions);
		String newRefreshToken = jwtUtility.generateRefreshToken(user.getId());

		// Step 5: Rotate — update same DB row (no new row created)
		// ========================
		// REVOKE OLD TOKEN PAIR
		// ========================

		token.setRevoked(true);
		token.setExpired(true);
		token.setRefreshUsed(true);

		userTokenRepository.save(token);

		// ========================
		// CREATE NEW TOKEN RECORD
		// ========================

		UserToken newToken = new UserToken();

		newToken.setUser(user);

		newToken.setAccessToken(newAccessToken);
		newToken.setRefreshToken(newRefreshToken);

		newToken.setAccessExpiry(Instant.now().plusMillis(accessTokenExpiry));

		newToken.setRefreshExpiry(Instant.now().plusMillis(refreshTokenExpiry));

		newToken.setCreatedAt(Instant.now());

		newToken.setRevoked(false);
		newToken.setExpired(false);

		newToken.setIpAddress(RequestInfoUtil.getClientIp(request));

		newToken.setDeviceInfo(RequestInfoUtil.getDeviceInfo(request));

		userTokenRepository.save(newToken);

		log.info("TOKEN REFRESH | userId={} | newAccessExpiry={} | newRefreshExpiry={}", user.getId(),
				newToken.getAccessExpiry(), newToken.getRefreshExpiry());

		auditService.log(AuditAction.TOKEN_REFRESH, AuditStatus.SUCCESS, "Access token refreshed successfully",
				request);

		return ResponseEntity.ok(new JwtResponse(newAccessToken, newRefreshToken, user.getRole().getName()));
	}

	// ======================== 🚪 LOGOUT ========================

	@Transactional
	@Override
	public ResponseEntity<?> logout(String accessToken, HttpServletRequest request) {

		User currentUser = getCurrentUser();

		UserToken token = userTokenRepository.findByAccessToken(accessToken)
				.orElseThrow(() -> new RuntimeException("Token not found"));

		if (!token.getUser().getId().equals(currentUser.getId())) {
			throw new RuntimeException("Access denied: Cannot logout another user's session.");
		}

		if (token.isRevoked()) {
			return ResponseEntity.badRequest().body("Session already logged out.");
		}

		token.setRevoked(true);
		token.setExpired(true);

		userTokenRepository.save(token);

		SecurityContextHolder.clearContext();

		log.info("LOGOUT | userId={} | tokenId={}", currentUser.getId(), token.getId());

		auditService.log(AuditAction.LOGOUT, AuditStatus.SUCCESS, "User logged out successfully", null);

		return ResponseEntity.ok("Logged out successfully.");

	}

	@Transactional
	@Override
	public ResponseEntity<?> logoutAllDevices(HttpServletRequest request) {

		User user = getCurrentUser();
		int count = revokeAllActiveTokens(user);
		SecurityContextHolder.clearContext();

		log.info("LOGOUT_ALL | userId={} | sessionsRevoked={}", user.getId(), count);

		auditService.log(AuditAction.LOGOUT_ALL, AuditStatus.SUCCESS, "Logged out from all devices", null);

		return ResponseEntity
				.ok(Map.of("message", "Logged out from all devices successfully.", "sessionsEnded", count));
	}

	// ======================== ✅ TOKEN VALIDATION ========================

	@Override
	public ResponseEntity<?> validateToken(String token, HttpServletRequest request) {

		if (!jwtUtility.isTokenValid(token)) {
			log.warn("INVALID TOKEN VALIDATION attempt");
			auditService.log(AuditAction.INVALID_TOKEN, AuditStatus.WARNING, "Invalid JWT token validation attempt",
					request);
			return ResponseEntity.badRequest()
					.body(Map.of("valid", false, "reason", "Token is invalid or has expired."));
		}

		return userTokenRepository.findByAccessToken(token).map(t -> {

			if (t.isRevoked()) {
				log.warn("TOKEN REVOKED | userId={} | tokenId={}", t.getUser().getId(), t.getId());

				auditService.log(AuditAction.REVOKED_TOKEN_USAGE, AuditStatus.WARNING, "Revoked token usage attempt",
						request);

				return ResponseEntity.badRequest().body(Map.of("valid", false, "reason", "Token has been revoked."));
			}

			if (t.isExpired()) {
				log.warn("TOKEN EXPIRED | userId={} | tokenId={}", t.getUser().getId(), t.getId());
				auditService.log(AuditAction.EXPIRED_TOKEN_USAGE, AuditStatus.WARNING, "Expired token usage attempt",
						request);

				return ResponseEntity.badRequest().body(Map.of("valid", false, "reason", "Token has expired."));
			}

			// ✅ SUCCESS LOG (correct place)
			log.info("TOKEN VALIDATION SUCCESS | userId={} | tokenId={}", t.getUser().getId(), t.getId());

			return ResponseEntity
					.ok(Map.of("valid", true, "userId", t.getUser().getId(), "role", t.getUser().getRole().getName()));

		}).orElseGet(() -> {
			log.warn("TOKEN NOT FOUND in DB");
			return ResponseEntity.badRequest()
					.body(Map.of("valid", false, "reason", "Token not found in active sessions."));
		});
	}

	// ======================== 📱 SESSION MANAGEMENT ========================

	@Override
	public ResponseEntity<?> getActiveSessions(Pageable pageable) {

		User user = getCurrentUser();

		Page<UserToken> sessionPage = userTokenRepository.findAllByUserAndRevokedFalseAndExpiredFalse(user, pageable);

		List<Map<String, Object>> sessions = sessionPage.getContent().stream()
				.map(t -> Map.<String, Object>of("sessionId", t.getId(), "deviceInfo",
						t.getDeviceInfo() != null ? t.getDeviceInfo() : "Unknown device",

						"ipAddress", t.getIpAddress() != null ? t.getIpAddress() : "Unknown IP",

						"accessExpiry", t.getAccessExpiry(), "refreshExpiry", t.getRefreshExpiry()))
				.collect(Collectors.toList());

		auditService.log(AuditAction.VIEW_ACTIVE_SESSIONS, AuditStatus.SUCCESS, "Fetched active sessions", null);

		return ResponseEntity.ok(Map.of(

				"userId", user.getId(),

				"sessions", sessions,

				"currentPage", sessionPage.getNumber(),

				"pageSize", sessionPage.getSize(),

				"totalElements", sessionPage.getTotalElements(),

				"totalPages", sessionPage.getTotalPages(),

				"hasNext", sessionPage.hasNext(),

				"hasPrevious", sessionPage.hasPrevious(),

				"isFirst", sessionPage.isFirst(),

				"isLast", sessionPage.isLast()));
	}

	@Transactional
	@Override
	public ResponseEntity<?> revokeSessionById(Long tokenId, HttpServletRequest request) {

		User currentUser = getCurrentUser();

		UserToken token = userTokenRepository.findById(tokenId)
				.orElseThrow(() -> new RuntimeException("Session not found with ID: " + tokenId));

		// Security: user can only revoke their own sessions
		if (!token.getUser().getId().equals(currentUser.getId())) {
			throw new RuntimeException("Access denied. You can only revoke your own sessions.");
		}

		if (token.isRevoked()) {
			return ResponseEntity.badRequest().body("Session is already revoked.");
		}

		token.setRevoked(true);
		token.setExpired(true);
		userTokenRepository.save(token);

		log.info("SESSION_REVOKE | userId={} | sessionId={}", currentUser.getId(), tokenId);

		auditService.log(AuditAction.SESSION_REVOKED, AuditStatus.SUCCESS, "Session revoked successfully", null);

		return ResponseEntity.ok(Map.of("message", "Session revoked successfully.", "sessionId", tokenId));
	}

	// ======================== 👤 SPRING SECURITY ========================

	/**
	 * Called ONLY by Spring Security AuthenticationManager during login. NOT used
	 * for principal resolution after authentication. Principal is always userId —
	 * resolved via getCurrentUser().
	 */
	@Override
	public UserDetails loadUserByUsername(String username) {

		User user = userRepository.findByUsername(normalize(username)).orElseThrow(() -> {
			log.warn("USER NOT FOUND DURING AUTHENTICATION | username={}", username);
			return new UsernameNotFoundException("No account found for username: " + username);
		});

		Set<SimpleGrantedAuthority> authorities = new HashSet<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));
		user.getRole().getPermissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p.getName())));

		return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
				authorities);
	}

	// ======================== 🧰 PRIVATE HELPERS ========================

	private void authenticate(String username, String password) {

		try {

			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

			userRepository.findByUsername(normalize(username))
					.ifPresent(user -> loginAttemptService.resetFailedAttempts(user, username));

		} catch (BadCredentialsException e) {

			User user = userRepository.findByUsername(normalize(username)).orElse(null);

			if (user != null) {

				if (user.isAccountLocked()) {

					log.warn("LOGIN BLOCKED | ACCOUNT ALREADY LOCKED | username={}", username);

					auditService.log(AuditAction.LOGIN, AuditStatus.BLOCKED, "Login blocked because account is locked",
							null);

					throw new LockedException("Account already locked");
				}

				loginAttemptService.recordFailedAttempt(user, username);

			} else {

				log.warn("LOGIN FAILED | UNKNOWN USERNAME | username={}", username);

				auditService.log(AuditAction.LOGIN, AuditStatus.FAILED, "Login attempt with unknown username", null);
			}

			throw new BadCredentialsException("Invalid username or password.");

		} catch (DisabledException e) {

			log.warn("LOGIN BLOCKED | ACCOUNT DISABLED | username={}", username);

			auditService.log(AuditAction.LOGIN, AuditStatus.BLOCKED, "Login blocked because account is disabled", null);

			throw new RuntimeException("Your account is disabled.");

		} catch (LockedException e) {

			log.warn("LOGIN BLOCKED | ACCOUNT LOCKED | username={}", username);

			auditService.log(AuditAction.LOGIN, AuditStatus.BLOCKED, "Login blocked because account is locked", null);

			throw new RuntimeException("Your account is locked.");
		}
	}

	/**
	 * ✅ Generates tokens using userId (NOT username). Username changes will never
	 * invalidate tokens.
	 */
	private JwtResponse generateTokens(User user, HttpServletRequest httpRequest) {

		Set<String> permissions = user.getRole().getPermissions().stream().map(p -> p.getName())
				.collect(Collectors.toSet());

		// ✅ userId passed — not username
		String accessToken = jwtUtility.generateAccessToken(user.getId(), user.getRole().getName(), permissions);
		String refreshToken = jwtUtility.generateRefreshToken(user.getId());

		UserToken userToken = new UserToken();
		userToken.setUser(user);
		userToken.setAccessToken(accessToken);
		userToken.setRefreshToken(refreshToken);
		userToken.setAccessExpiry(Instant.now().plusMillis(accessTokenExpiry));
		userToken.setRefreshExpiry(Instant.now().plusMillis(refreshTokenExpiry));
		userToken.setCreatedAt(Instant.now());
		userToken.setRevoked(false);
		userToken.setExpired(false);
		userToken.setIpAddress(RequestInfoUtil.getClientIp(httpRequest));
		userToken.setDeviceInfo(RequestInfoUtil.getDeviceInfo(httpRequest));
		userTokenRepository.save(userToken);

		log.info("TOKEN CREATED | userId={} | ip={} | device={}", user.getId(), userToken.getIpAddress(),
				userToken.getDeviceInfo());

		return new JwtResponse(accessToken, refreshToken, user.getRole().getName());
	}

	private int revokeAllActiveTokens(User user) {
		List<UserToken> active = userTokenRepository.findAllByUserAndRevokedFalseAndExpiredFalse(user);
		if (active.isEmpty())
			return 0;
		active.forEach(t -> {
			t.setRevoked(true);
			t.setExpired(true);
		});
		userTokenRepository.saveAll(active);

		log.info("REVOKING ALL ACTIVE TOKENS | userId={} | count={}", user.getId(), active.size());

		return active.size();
	}

	private User getUserByUsername(String username) {
		return userRepository.findByUsername(normalize(username))
				.orElseThrow(() -> new RuntimeException("No account found for username: " + username));
	}

	/**
	 * ✅ Resolves current user from SecurityContext principal. Principal is userId
	 * (Long as String) — set by JwtFilter. Fetches via findById — immune to
	 * username changes.
	 */
	private User getCurrentUser() {

		if (SecurityContextHolder.getContext().getAuthentication() == null
				|| !SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
				|| SecurityContextHolder.getContext().getAuthentication().getPrincipal() == null) {

			log.error("SECURITY CONTEXT EMPTY");
			throw new RuntimeException("Authentication required");
		}

		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		Long userId;
		try {
			userId = Long.parseLong(principal.toString());
		} catch (NumberFormatException e) {
			log.error("INVALID PRINCIPAL FORMAT: {}", principal);
			throw new RuntimeException("Invalid authentication token");
		}

		return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Authenticated user not found"));
	}

	private String normalize(String value) {

		if (value == null || value.isBlank()) {
			return null;
		}

		return value.trim().toLowerCase();
	}
}