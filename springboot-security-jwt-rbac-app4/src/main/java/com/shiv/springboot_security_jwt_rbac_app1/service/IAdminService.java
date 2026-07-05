package com.shiv.springboot_security_jwt_rbac_app1.service;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.shiv.springboot_security_jwt_rbac_app1.model.Status;

/**
 * Admin-only service contract.
 *
 * Design decisions (production template): - ALL operations use userId (Long) —
 * never username — for consistency, safety against username changes, and URL
 * predictability. - Every method has a matching permission checked at TWO
 * layers: 1. @PreAuthorize in the controller (fast-fail before service is
 * called) 2. programmatic re-check inside service (defense-in-depth) - Admin
 * cannot act on themselves (self-protection guard on every mutation).
 *
 * Permission map (must exist in DB via RoleInitializationService): READ_USER →
 * getAllUsers, getUserById DELETE_USER → deleteUserById UPDATE_USER_STATUS →
 * updateUserStatus TOGGLE_USER_ACCESS → toggleUserAccess FORCE_LOGOUT →
 * forceLogoutUser ASSIGN_<ROLE> → updateUserRole (resolved dynamically per
 * target role)
 */
public interface IAdminService {

	// ── READ ─────────────────────────────────────────────────────────────────
	/** Returns all registered users. Requires READ_USER. */
	ResponseEntity<?> getAllUsers(Pageable pageable);

	/** Returns a single user by ID. Requires READ_USER. */
	ResponseEntity<?> getUserById(Long userId);

	// ── DELETE ───────────────────────────────────────────────────────────────
	/**
	 * Permanently deletes a user and revokes all their tokens. Requires
	 * DELETE_USER.
	 */
	ResponseEntity<?> deleteUserById(Long userId);

	// ── ROLE ─────────────────────────────────────────────────────────────────
	/** Assigns a new role to a user. Requires ASSIGN_<ROLENAME> dynamically. */
	ResponseEntity<?> updateUserRole(Long userId, String roleName);

	// ── STATUS ───────────────────────────────────────────────────────────────
	/**
	 * Updates account status (ACTIVE / INACTIVE / SUSPENDED). Requires
	 * UPDATE_USER_STATUS.
	 */
	ResponseEntity<?> updateUserStatus(Long userId, Status status);

	// ── ACCESS ───────────────────────────────────────────────────────────────
	/** Enables or disables account login access. Requires TOGGLE_USER_ACCESS. */
	ResponseEntity<?> toggleUserAccess(Long userId, boolean enabled);

	// ── FORCE LOGOUT ─────────────────────────────────────────────────────────
	/**
	 * Revokes all active sessions for a user from all devices. Requires
	 * FORCE_LOGOUT.
	 */
	ResponseEntity<?> forceLogoutUser(Long userId);

//	==========================================================================================

	// =========================================================================
	// 👥 USER MANAGEMENT
	// =========================================================================

	ResponseEntity<?> deleteUserPermanently(Long userId);

	// =========================================================================
	// 🔒 ACCESS MANAGEMENT
	// =========================================================================

	ResponseEntity<?> enableUserAccess(Long userId);

	ResponseEntity<?> disableUserAccess(Long userId);

	ResponseEntity<?> lockUserAccount(Long userId);

	ResponseEntity<?> unlockUserAccount(Long userId);

	// =========================================================================
	// 🔐 SECURITY OPERATIONS
	// =========================================================================

	ResponseEntity<?> revokeUserTokens(Long userId);

	ResponseEntity<?> invalidateUserSessions(Long userId);

	// =========================================================================
	// 🛡️ SYSTEM ADMINISTRATION
	// =========================================================================

	ResponseEntity<?> enableMaintenanceMode();

	ResponseEntity<?> disableMaintenanceMode();

	ResponseEntity<?> clearSystemCache();

	ResponseEntity<?> refreshPermissionsCache();

	// =========================================================================
	// 📊 SYSTEM ANALYTICS
	// =========================================================================

	ResponseEntity<?> getSystemStatistics();

	ResponseEntity<?> getSecurityStatistics();

	// ======================== 🧑‍💼 EMPLOYEE REGISTRATION MANAGEMENT
	// ========================
	ResponseEntity<?> getPendingRegistrations(Pageable pageable);

	ResponseEntity<?> approveRegistration(Long userId);

	ResponseEntity<?> rejectRegistration(Long userId, String reason);
}
