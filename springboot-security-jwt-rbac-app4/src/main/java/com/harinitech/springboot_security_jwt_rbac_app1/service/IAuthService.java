package com.harinitech.springboot_security_jwt_rbac_app1.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.harinitech.springboot_security_jwt_rbac_app1.model.JwtRequest;
import com.harinitech.springboot_security_jwt_rbac_app1.model.JwtResponse;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Production auth service contract.
 *
 * Method summary:
 * ┌─────────────────────────────┬──────────────────────────────────────────────────────┐
 * │ Method │ Purpose │
 * ├─────────────────────────────┼──────────────────────────────────────────────────────┤
 * │ authenticateUser │ Login — validate credentials, issue token pair │ │
 * refreshToken │ Rotate access+refresh token pair silently │ │ logout │ Revoke
 * current device session │ │ logoutAllDevices │ Revoke ALL active sessions for
 * current user │ │ validateToken │ Check if access token is still valid &
 * active in DB │ │ getActiveSessions │ List all active sessions (devices) for
 * current user │ │ revokeSessionById │ Revoke one specific session by token ID
 * │
 * └─────────────────────────────┴──────────────────────────────────────────────────────┘
 */
public interface IAuthService extends UserDetailsService {

	// ── LOGIN / TOKEN ─────────────────────────────────────────────────────────
	JwtResponse authenticateUser(JwtRequest request, HttpServletRequest httpRequest);

	ResponseEntity<?> refreshToken(String refreshToken, HttpServletRequest request);

	// ── LOGOUT ────────────────────────────────────────────────────────────────
	ResponseEntity<?> logout(String accessToken, HttpServletRequest httpRequest);

	ResponseEntity<?> logoutAllDevices(HttpServletRequest httpRequest);

	// ── TOKEN VALIDATION ──────────────────────────────────────────────────────
	ResponseEntity<?> validateToken(String token, HttpServletRequest httpRequest);

	// ── SESSION MANAGEMENT ────────────────────────────────────────────────────
	/**
	 * Returns all active (non-revoked, non-expired) sessions for the current user.
	 */
	ResponseEntity<?> getActiveSessions(Pageable pageable);
	/**
	 * Revokes one specific session by its DB token ID — targeted single-device
	 * logout.
	 */
	ResponseEntity<?> revokeSessionById(Long tokenId, HttpServletRequest httpRequest);
}