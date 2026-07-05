package com.shiv.springboot_security_jwt_rbac_app1.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shiv.springboot_security_jwt_rbac_app1.model.ApiResponse;
import com.shiv.springboot_security_jwt_rbac_app1.service.IAdminService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

	private final IAdminService adminService;

	// =========================================================================
	// 👥 USER MANAGEMENT
	// =========================================================================

	@GetMapping("/users")
	@PreAuthorize("hasAuthority('READ_USER')")
	public ResponseEntity<ApiResponse<?>> getAllUsers(Pageable pageable) {

		log.info("ADMIN API | GET USERS | page={} | size={} | sort={}", pageable.getPageNumber(),
				pageable.getPageSize(), pageable.getSort());

		return ResponseEntity
				.ok(ApiResponse.success("Users fetched successfully", adminService.getAllUsers(pageable).getBody()));
	}

	// =========================================================================
	// 🔐 SECURITY OPERATIONS
	// =========================================================================

	@PostMapping("/users/{id}/force-logout")
	@PreAuthorize("hasAuthority('FORCE_LOGOUT')")
	public ResponseEntity<ApiResponse<?>> forceLogoutUser(@PathVariable Long id) {

		log.warn("ADMIN API | FORCE LOGOUT | targetUserId={}", id);

		return ResponseEntity
				.ok(ApiResponse.success("User force logout successful", adminService.forceLogoutUser(id).getBody()));
	}

	@PostMapping("/users/{id}/revoke-tokens")
	@PreAuthorize("hasAuthority('REVOKE_TOKEN')")
	public ResponseEntity<ApiResponse<?>> revokeUserTokens(@PathVariable Long id) {

		log.warn("ADMIN API | REVOKE TOKENS | targetUserId={}", id);

		return ResponseEntity.ok(
				ApiResponse.success("User tokens revoked successfully", adminService.revokeUserTokens(id).getBody()));
	}

	@PostMapping("/users/{id}/invalidate-sessions")
	@PreAuthorize("hasAuthority('SESSION_REVOKE')")
	public ResponseEntity<ApiResponse<?>> invalidateUserSessions(@PathVariable Long id) {

		log.warn("ADMIN API | INVALIDATE SESSIONS | targetUserId={}", id);

		return ResponseEntity.ok(ApiResponse.success("User sessions invalidated successfully",
				adminService.invalidateUserSessions(id).getBody()));
	}

	// =========================================================================
	// 🔒 ACCOUNT SECURITY CONTROL
	// =========================================================================

	@PutMapping("/users/{id}/lock")
	@PreAuthorize("hasAuthority('ACCOUNT_LOCK')")
	public ResponseEntity<ApiResponse<?>> lockUserAccount(@PathVariable Long id) {

		log.warn("ADMIN API | LOCK ACCOUNT | targetUserId={}", id);

		return ResponseEntity.ok(
				ApiResponse.success("User account locked successfully", adminService.lockUserAccount(id).getBody()));
	}

	@PutMapping("/users/{id}/unlock")
	@PreAuthorize("hasAuthority('ACCOUNT_UNLOCK')")
	public ResponseEntity<ApiResponse<?>> unlockUserAccount(@PathVariable Long id) {

		log.warn("ADMIN API | UNLOCK ACCOUNT | targetUserId={}", id);

		return ResponseEntity.ok(ApiResponse.success("User account unlocked successfully",
				adminService.unlockUserAccount(id).getBody()));
	}

	@PutMapping("/users/{id}/enable")
	@PreAuthorize("hasAuthority('TOGGLE_USER_ACCESS')")
	public ResponseEntity<ApiResponse<?>> enableUserAccess(@PathVariable Long id) {

		log.warn("ADMIN API | ENABLE ACCESS | targetUserId={}", id);

		return ResponseEntity.ok(
				ApiResponse.success("User access enabled successfully", adminService.enableUserAccess(id).getBody()));
	}

	@PutMapping("/users/{id}/disable")
	@PreAuthorize("hasAuthority('TOGGLE_USER_ACCESS')")
	public ResponseEntity<ApiResponse<?>> disableUserAccess(@PathVariable Long id) {

		log.warn("ADMIN API | DISABLE ACCESS | targetUserId={}", id);

		return ResponseEntity.ok(
				ApiResponse.success("User access disabled successfully", adminService.disableUserAccess(id).getBody()));
	}

	// =========================================================================
	// 🛡️ SYSTEM ADMINISTRATION
	// =========================================================================

	@PutMapping("/system/maintenance-mode/enable")
	@PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
	public ResponseEntity<ApiResponse<?>> enableMaintenanceMode() {

		log.warn("ADMIN API | ENABLE MAINTENANCE MODE");

		return ResponseEntity.ok(ApiResponse.success("Maintenance mode enabled successfully",
				adminService.enableMaintenanceMode().getBody()));
	}

	@PutMapping("/system/maintenance-mode/disable")
	@PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
	public ResponseEntity<ApiResponse<?>> disableMaintenanceMode() {

		log.warn("ADMIN API | DISABLE MAINTENANCE MODE");

		return ResponseEntity.ok(ApiResponse.success("Maintenance mode disabled successfully",
				adminService.disableMaintenanceMode().getBody()));
	}

	@PostMapping("/system/cache/clear")
	@PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
	public ResponseEntity<ApiResponse<?>> clearSystemCache() {

		log.warn("ADMIN API | CLEAR SYSTEM CACHE");

		return ResponseEntity.ok(
				ApiResponse.success("System cache cleared successfully", adminService.clearSystemCache().getBody()));
	}

	@PostMapping("/system/permissions/refresh")
	@PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
	public ResponseEntity<ApiResponse<?>> refreshPermissionsCache() {

		log.warn("ADMIN API | REFRESH PERMISSIONS CACHE");

		return ResponseEntity.ok(ApiResponse.success("Permissions cache refreshed successfully",
				adminService.refreshPermissionsCache().getBody()));
	}

	// =========================================================================
	// 📊 SYSTEM ANALYTICS
	// =========================================================================

	@GetMapping("/statistics/system")
	@PreAuthorize("hasAuthority('VIEW_SYSTEM_STATISTICS')")
	public ResponseEntity<ApiResponse<?>> getSystemStatistics() {

		log.info("ADMIN API | SYSTEM STATISTICS");

		return ResponseEntity.ok(ApiResponse.success("System statistics fetched successfully",
				adminService.getSystemStatistics().getBody()));
	}

	@GetMapping("/statistics/security")
	@PreAuthorize("hasAuthority('VIEW_SECURITY_STATISTICS')")
	public ResponseEntity<ApiResponse<?>> getSecurityStatistics() {

		log.info("ADMIN API | SECURITY STATISTICS");

		return ResponseEntity.ok(ApiResponse.success("Security statistics fetched successfully",
				adminService.getSecurityStatistics().getBody()));
	}

	// =========================================================================
	// 🗑️ CRITICAL OPERATIONS
	// =========================================================================

	@DeleteMapping("/users/{id}/permanent")
	@PreAuthorize("hasAuthority('DELETE_USER')")
	public ResponseEntity<ApiResponse<?>> deleteUserPermanently(@PathVariable Long id) {

		log.error("ADMIN API | PERMANENT DELETE | targetUserId={}", id);

		return ResponseEntity.ok(ApiResponse.success("User permanently deleted successfully",
				adminService.deleteUserPermanently(id).getBody()));
	}

	@GetMapping("/pending-registrations")
	@PreAuthorize("hasAuthority('VIEW_PENDING_REGISTRATIONS')")
	public ResponseEntity<ApiResponse<?>> getPendingRegistrations(Pageable pageable) {

		log.info("ADMIN API | FETCH PENDING REGISTRATIONS | page={} | size={} | sort={}", pageable.getPageNumber(),
				pageable.getPageSize(), pageable.getSort());

		return ResponseEntity.ok(ApiResponse.success("Pending registrations fetched",
				adminService.getPendingRegistrations(pageable).getBody()));
	}

	@PostMapping("/registrations/{id}/approve")
	@PreAuthorize("hasAuthority('APPROVE_REGISTRATION')")
	public ResponseEntity<ApiResponse<?>> approveRegistration(@PathVariable Long id) {
		log.warn("ADMIN API | APPROVE REGISTRATION | userId={}", id);
		return ResponseEntity
				.ok(ApiResponse.success("Registration approved", adminService.approveRegistration(id).getBody()));
	}

	@PostMapping("/registrations/{id}/reject")
	@PreAuthorize("hasAuthority('REJECT_REGISTRATION')")
	public ResponseEntity<ApiResponse<?>> rejectRegistration(@PathVariable Long id,
			@RequestParam(defaultValue = "Rejected by administrator") String reason) {
		log.warn("ADMIN API | REJECT REGISTRATION | userId={} | reason={}", id, reason);
		return ResponseEntity.ok(
				ApiResponse.success("Registration rejected", adminService.rejectRegistration(id, reason).getBody()));
	}
}
