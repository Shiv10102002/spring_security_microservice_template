package com.harinitech.springboot_security_jwt_rbac_app1.controller;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.harinitech.springboot_security_jwt_rbac_app1.model.ApiResponse;
import com.harinitech.springboot_security_jwt_rbac_app1.model.JwtRequest;
import com.harinitech.springboot_security_jwt_rbac_app1.service.IAuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final IAuthService authService;

	// ======================== 🔐 LOGIN ========================

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<?>> login(@Valid @RequestBody JwtRequest request,
			HttpServletRequest httpRequest) {

		log.info("AUTH API | Login attempt | username={}", request.getUsername());

		return ResponseEntity
				.ok(ApiResponse.success("Login successful", authService.authenticateUser(request, httpRequest)));
	}

	// ======================== 🔄 REFRESH ========================

	@PostMapping("/refresh-token")
	public ResponseEntity<ApiResponse<?>> refreshToken(@RequestBody Map<String, String> request,
			HttpServletRequest httpRequest) {

		return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully",
				authService.refreshToken(request.get("refreshToken"), httpRequest).getBody()));
	}

	// ======================== ✅ VALIDATE ========================

	@PostMapping("/validate-token")
	public ResponseEntity<ApiResponse<?>> validateToken(@RequestBody Map<String, String> request,
			HttpServletRequest httpRequest) {

		return ResponseEntity.ok(ApiResponse.success("Token validation completed",
				authService.validateToken(request.get("token"), httpRequest).getBody()));
	}

	// ======================== 🚪 LOGOUT ========================

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<?>> logout(HttpServletRequest request) {

		String authHeader = request.getHeader("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			throw new RuntimeException("Authorization token missing.");
		}

		String token = authHeader.substring(7);

		return ResponseEntity
				.ok(ApiResponse.success("Logout successful", authService.logout(token, request).getBody()));
	}

	// ======================== 📱 ACTIVE SESSIONS ========================

	@GetMapping("/sessions")
	public ResponseEntity<ApiResponse<?>> getActiveSessions(Pageable pageable) {

		log.info("AUTH API | ACTIVE SESSIONS | page={} | size={}", pageable.getPageNumber(), pageable.getPageSize());

		return ResponseEntity.ok(ApiResponse.success("Active sessions fetched successfully",
				authService.getActiveSessions(pageable).getBody()));
	}
	// ======================== ❌ REVOKE SESSION ========================

	@PreAuthorize("isAuthenticated()")
	@DeleteMapping("/sessions/{id}")
	public ResponseEntity<ApiResponse<?>> revokeSession(@PathVariable Long id, HttpServletRequest request) {

		return ResponseEntity.ok(ApiResponse.success("Session revoked successfully",
				authService.revokeSessionById(id, request).getBody()));
	}

	// ======================== 🚪 LOGOUT ALL ========================

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/logout-all")
	public ResponseEntity<ApiResponse<?>> logoutAllDevices(HttpServletRequest request) {

		return ResponseEntity.ok(ApiResponse.success("Logged out from all devices successfully",
				authService.logoutAllDevices(request).getBody()));
	}
}