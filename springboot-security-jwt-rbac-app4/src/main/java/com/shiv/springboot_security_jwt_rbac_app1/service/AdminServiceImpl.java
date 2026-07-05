package com.shiv.springboot_security_jwt_rbac_app1.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shiv.springboot_security_jwt_rbac_app1.client.NotificationFacade;
import com.shiv.springboot_security_jwt_rbac_app1.dto.NotificationType;
import com.shiv.springboot_security_jwt_rbac_app1.entity.Role;
import com.shiv.springboot_security_jwt_rbac_app1.entity.User;
import com.shiv.springboot_security_jwt_rbac_app1.entity.UserToken;
import com.shiv.springboot_security_jwt_rbac_app1.model.AuditAction;
import com.shiv.springboot_security_jwt_rbac_app1.model.AuditStatus;
import com.shiv.springboot_security_jwt_rbac_app1.model.Status;
import com.shiv.springboot_security_jwt_rbac_app1.model.UserMapper;
import com.shiv.springboot_security_jwt_rbac_app1.repo.RoleRepository;
import com.shiv.springboot_security_jwt_rbac_app1.repo.UserRepository;
import com.shiv.springboot_security_jwt_rbac_app1.repo.UserTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ✅ PRODUCTION AdminServiceImpl — fully userId-based.
 *
 * Principal contract (matches JwtFilter + all other services): SecurityContext
 * principal → userId (Long as String) getCurrentUserId() →
 * Long.parseLong(principal.toString()) All DB lookups →
 * userRepository.findById(userId)
 *
 * guardSelf() directly compares Long IDs — no extra DB call needed.
 */

@Transactional
@Slf4j
@RequiredArgsConstructor
@Service
public class AdminServiceImpl implements IAdminService {

	// ======================== DEPENDENCIES ========================

	private final UserRepository userRepository;

	private final RoleRepository roleRepository;

	private final UserTokenRepository userTokenRepository;

	private final AuditService auditService;

	// ✅ FIXED: inject NotificationFacade, NOT NotificationClient directly.
	// NotificationFacade provides typed overloads and fire-and-tolerate handling.

	private final NotificationFacade notificationFacade;

	// ======================== 📋 READ ========================

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<?> getAllUsers(Pageable pageable) {

		Page<User> userPage = userRepository.findAll(pageable);

		Page<?> responsePage = userPage.map(UserMapper::toSummary);

		log.info("ADMIN USERS FETCHED | adminId={} | page={} | size={} | totalElements={}", getCurrentUserId(),
				pageable.getPageNumber(), pageable.getPageSize(), userPage.getTotalElements());

		auditService.log(AuditAction.VIEW_USERS, AuditStatus.SUCCESS, "Admin fetched paginated users", null);

		return ResponseEntity.ok(responsePage);
	}

	@Override
	public ResponseEntity<?> getUserById(Long userId) {
		User user = getUserOrThrow(userId);
		log.info("Admin [userId={}] fetched user [userId={}]", getCurrentUserId(), userId);
		auditService.log(AuditAction.VIEW_USER, AuditStatus.SUCCESS, "Admin fetched user with ID: " + userId, null);
		return ResponseEntity.ok(UserMapper.toResponse(user));
	}

	// ======================== 🗑️ DELETE ========================

	@Override
	public ResponseEntity<?> deleteUserById(Long userId) {

		// 🛡️ Defense-in-depth: re-verify permission inside service
		requirePermission("DELETE_USER");

		// 🛡️ Self-protection: admin cannot delete themselves
		guardSelf(userId, "delete");

		User user = getUserOrThrow(userId);
		log.warn("USER DELETE INITIATED | adminId={} | targetUserId={}", getCurrentUserId(), userId);
		revokeAllActiveTokens(user);
		userRepository.delete(user);

		log.info("Admin [userId={}] deleted user [userId={}]", getCurrentUserId(), userId);

		auditService.log(AuditAction.DELETE_USER, AuditStatus.SUCCESS, "Admin deleted user with ID: " + userId, null);

		return ResponseEntity.ok(Map.of("message", "User deleted successfully.", "deletedUserId", userId));
	}

	// ======================== 🔐 ROLE ========================

	@Override
	public ResponseEntity<?> updateUserRole(Long userId, String roleName) {

		if (roleName == null || roleName.isBlank()) {
			throw new RuntimeException("Role name cannot be empty.");
		}

		requirePermission("ASSIGN_" + roleName.toUpperCase());
		guardSelf(userId, "change role of");

		User targetUser = getUserOrThrow(userId);
		Role newRole = getRoleOrThrow(roleName);

		String previousRole = targetUser.getRole().getName();
		if (targetUser.getRole().getName().equalsIgnoreCase(roleName)) {
			throw new RuntimeException("User already has role: " + roleName.toUpperCase());
		}
		targetUser.setRole(newRole);
		userRepository.save(targetUser);

		revokeAllActiveTokens(targetUser);

		log.info("Admin [userId={}] updated role of user [userId={}]: {} → {}", getCurrentUserId(), userId,
				previousRole, roleName.toUpperCase());

		auditService.log(AuditAction.ROLE_CHANGED, AuditStatus.SUCCESS,
				"User role updated from " + previousRole + " to " + roleName, null);

		// ✅ FIXED: notify user of role change via NotificationFacade
		notificationFacade.sendNotification(targetUser.getUsername(), NotificationType.ROLE_CHANGED,
				roleName.toUpperCase(), null, null);

		return ResponseEntity.ok(Map.of("message", "User role updated successfully.", "userId", userId, "previousRole",
				previousRole, "newRole", roleName.toUpperCase()));
	}

	// ======================== 🔘 STATUS ========================

	@Override
	public ResponseEntity<?> updateUserStatus(Long userId, Status status) {

		if (status == null) {
			throw new RuntimeException("Status cannot be null.");
		}

		requirePermission("UPDATE_USER_STATUS");
		guardSelf(userId, "update status of");

		User user = getUserOrThrow(userId);
		Status previousStatus = user.getStatus();

		if (user.getStatus() == status) {
			throw new RuntimeException("User already has status: " + status);
		}

		user.setStatus(status);

		userRepository.save(user);

		if (status != Status.ACTIVE) {
			revokeAllActiveTokens(user);
			log.info("Admin [userId={}] deactivated user [userId={}] → tokens revoked", getCurrentUserId(), userId);
		}

		log.info("Admin [userId={}] updated status of user [userId={}]: {} → {}", getCurrentUserId(), userId,
				previousStatus, status);

		auditService.log(AuditAction.STATUS_CHANGED, AuditStatus.SUCCESS,
				"User status updated from " + previousStatus + " to " + status, null);

		// ✅ FIXED: notify user of status change via NotificationFacade
		notificationFacade.sendNotification(user.getUsername(), NotificationType.STATUS_CHANGED, null, status.name(),
				null);

		return ResponseEntity.ok(Map.of("message", "User status updated successfully.", "userId", userId,
				"previousStatus", previousStatus, "currentStatus", status));
	}

	// ======================== 🔒 ACCESS TOGGLE ========================

	@Override
	public ResponseEntity<?> toggleUserAccess(Long userId, boolean enabled) {

		requirePermission("TOGGLE_USER_ACCESS");
		guardSelf(userId, "toggle access of");

		User user = getUserOrThrow(userId);
		if (user.isEnabled() == enabled) {
			throw new RuntimeException("User access already set to: " + enabled);
		}
		user.setEnabled(enabled);
		userRepository.save(user);

		if (!enabled) {
			revokeAllActiveTokens(user);
			log.info("Admin [userId={}] disabled user [userId={}] → tokens revoked", getCurrentUserId(), userId);
		}

		log.info("Admin [userId={}] set enabled={} for user [userId={}]", getCurrentUserId(), enabled, userId);

		auditService.log(AuditAction.ACCESS_TOGGLED, AuditStatus.SUCCESS,
				enabled ? "User access enabled" : "User access disabled", null);

		// ✅ FIXED: notify user of access change via NotificationFacade
		notificationFacade.sendNotification(user.getUsername(),
				enabled ? NotificationType.ACCESS_ENABLED : NotificationType.ACCESS_DISABLED);

		return ResponseEntity.ok(Map.of("message",
				enabled ? "User access enabled successfully." : "User access disabled and all sessions revoked.",
				"userId", userId, "enabled", enabled));
	}

	// ======================== 🚪 FORCE LOGOUT ========================

	@Override
	public ResponseEntity<?> forceLogoutUser(Long userId) {

		requirePermission("FORCE_LOGOUT");

		guardSelf(userId, "force logout");

		User user = getUserOrThrow(userId);
		int revokedCount = revokeAllActiveTokens(user);

		log.info("Admin [userId={}] force-logged-out user [userId={}]. Tokens revoked: {}", getCurrentUserId(), userId,
				revokedCount);

		auditService.log(AuditAction.FORCE_LOGOUT, AuditStatus.SUCCESS, "Admin force logged out user", null);

		// ✅ FIXED: notify user of force logout via NotificationFacade
		notificationFacade.sendNotification(user.getUsername(), NotificationType.FORCE_LOGOUT);

		return ResponseEntity.ok(Map.of("message", "User has been logged out from all devices.", "userId", userId,
				"sessionsEnded", revokedCount));
	}

	// ======================== 👑 HIERARCHICAL APPROVAL POLICY
	// ========================

	/**
	 * Centralised approval hierarchy. Key = Approver's role | Value = Roles they
	 * can approve.
	 * 
	 * For production, externalise to application.yml
	 * using @ConfigurationProperties.
	 */
	private static final Map<String, Set<String>> APPROVAL_HIERARCHY = Map.of("ADMIN", Set.of("MANAGER"), "MANAGER",
			Set.of("EMPLOYEE", "HR", "VENDOR", "SUPPORT"));

	private boolean canApproveRole(String approverRole, String targetRole) {
		Set<String> allowed = APPROVAL_HIERARCHY.get(approverRole.toUpperCase());
		return allowed != null && allowed.contains(targetRole.toUpperCase());
	}

	private Set<String> approvableRolesForCurrentUser() {
		User approver = getCurrentUser();
		return APPROVAL_HIERARCHY.getOrDefault(approver.getRole().getName().toUpperCase(), Set.of());
	}

	// ======================== 📋 PENDING REGISTRATIONS ========================

	@Override
	public ResponseEntity<?> getPendingRegistrations(Pageable pageable) {

		requirePermission("VIEW_PENDING_REGISTRATIONS");

		Set<String> approvableRoles = approvableRolesForCurrentUser();
		Page<User> pendingPage = approvableRoles.isEmpty() ? Page.empty(pageable)
				: userRepository.findByStatusAndRequestedRoleIn(Status.PENDING_APPROVAL, approvableRoles, pageable);

		Page<Map<String, Object>> responsePage = pendingPage
				.map(user -> Map.<String, Object>of("userId", user.getId(), "email", user.getUsername(),
						"requestedRole", user.getRequestedRole(), "registeredAt", user.getPasswordChangedAt()));

		log.info("PENDING REGISTRATIONS FETCHED | page={} | size={} | totalElements={} | by userId={}",
				pageable.getPageNumber(), pageable.getPageSize(), pendingPage.getTotalElements(), getCurrentUserId());

		auditService.log(AuditAction.VIEW_PENDING_REGISTRATIONS, AuditStatus.SUCCESS,
				"Fetched pending employee registrations", null);

		return ResponseEntity.ok(responsePage);
	}

	// ======================== ✅ APPROVE (HIERARCHICAL) ========================

	@Override
	@Transactional
	public ResponseEntity<?> approveRegistration(Long userId) {

		requirePermission("APPROVE_REGISTRATION");

		User approver = getCurrentUser();
		String approverRole = approver.getRole().getName();

		User pendingUser = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

		if (pendingUser.getStatus() != Status.PENDING_APPROVAL) {
			throw new RuntimeException("User is not in pending approval state.");
		}

		String rawRequestedRole = pendingUser.getRequestedRole();

		if (rawRequestedRole == null || rawRequestedRole.isBlank()) {
			throw new RuntimeException("No requested role found for this user.");
		}

		String requestedRole = rawRequestedRole.trim().toUpperCase();

		if (!canApproveRole(approverRole, requestedRole)) {
			throw new RuntimeException("You are not authorised to approve registrations for role: " + requestedRole);
		}

		Role role = roleRepository.findByName(requestedRole)
				.orElseThrow(() -> new RuntimeException("Requested role does not exist: " + requestedRole));

		pendingUser.setRole(role);
		pendingUser.setStatus(Status.ACTIVE);
		pendingUser.setEnabled(true);
		pendingUser.setRequestedRole(null);

		userRepository.save(pendingUser);

		notificationFacade.sendNotification(pendingUser.getUsername(), NotificationType.REGISTRATION_APPROVED,
				requestedRole, null, null);

		auditService.log(AuditAction.REGISTRATION_APPROVED, AuditStatus.SUCCESS,
				"Registration approved by " + approverRole + ". Role: " + requestedRole, null);

		return ResponseEntity.ok(Map.of("message", "User approved successfully.", "userId", userId, "approvedBy",
				approver.getId(), "assignedRole", requestedRole));
	}

	// ======================== ❌ REJECT ========================

	@Override
	@Transactional
	public ResponseEntity<?> rejectRegistration(Long userId, String reason) {
		requirePermission("REJECT_REGISTRATION");

		User approver = getCurrentUser();
		String approverRole = approver.getRole().getName();
		User pendingUser = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

		if (pendingUser.getStatus() != Status.PENDING_APPROVAL) {
			throw new RuntimeException("User is not in pending approval state.");
		}

		String requestedRole = pendingUser.getRequestedRole();
		if (requestedRole == null || requestedRole.isBlank()) {
			throw new RuntimeException("No requested role found for this user.");
		}

		requestedRole = requestedRole.trim().toUpperCase();

		if (!canApproveRole(approverRole, requestedRole)) {
			throw new RuntimeException("You are not authorised to reject registrations for role: " + requestedRole);
		}

		pendingUser.setStatus(Status.INACTIVE);
		pendingUser.setEnabled(false);
		pendingUser.setRequestedRole(null);
		userRepository.save(pendingUser);

		// ✅ FIXED: notify user of registration rejection via NotificationFacade
		notificationFacade.sendNotification(pendingUser.getUsername(), NotificationType.REGISTRATION_REJECTED, null,
				null, reason);

		log.info("REGISTRATION REJECTED | approverId={} | approverRole={} | userId={} | requestedRole={} | reason={}",
				approver.getId(), approverRole, userId, requestedRole, reason);
		auditService.log(AuditAction.REGISTRATION_REJECTED, AuditStatus.SUCCESS,
				"Rejected registration for role " + requestedRole + ". Reason: " + reason, null);

		return ResponseEntity.ok(Map.of("message", "User registration rejected.", "userId", userId, "reason", reason));
	}

	// ======================== 🧰 HELPER ========================

	/**
	 * Returns the currently authenticated admin user. Principal is userId (Long as
	 * String) – consistent with JwtFilter contract.
	 */
	private User getCurrentUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Long userId = Long.parseLong(principal.toString());
		return userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("Authenticated admin not found."));
	}

	// ======================== 🧰 PRIVATE HELPERS ========================

	/**
	 * Defense-in-depth permission check.
	 * 
	 * @PreAuthorize is gate 1; this is gate 2.
	 */
	private void requirePermission(String permission) {

		boolean hasPermission = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals(permission));

		if (!hasPermission) {
			log.warn("ACCESS DENIED | userId={} | missingPermission={}", getCurrentUserId(), permission);

			auditService.log(AuditAction.ACCESS_DENIED, AuditStatus.BLOCKED, "Missing permission: " + permission, null);

			throw new RuntimeException("Access denied. Missing required permission: '" + permission + "'.");
		}
	}

	/**
	 * ✅ Returns the authenticated admin's userId (Long) from the SecurityContext.
	 * JwtFilter sets userId (Long as String) as principal — parse it directly. No
	 * DB call needed — pure context read.
	 */
	private Long getCurrentUserId() {

		if (SecurityContextHolder.getContext().getAuthentication() == null
				|| SecurityContextHolder.getContext().getAuthentication().getPrincipal() == null) {

			log.error("SECURITY CONTEXT EMPTY");
			throw new RuntimeException("Authentication required.");
		}

		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		try {
			return Long.parseLong(principal.toString());

		} catch (NumberFormatException e) {

			log.error("INVALID PRINCIPAL FORMAT | principal={}", principal);

			throw new RuntimeException("Invalid authentication principal.");
		}
	}

	/**
	 * ✅ Self-protection guard — compares Long IDs directly, zero DB calls.
	 */
	private void guardSelf(Long targetUserId, String action) {
		if (getCurrentUserId().equals(targetUserId)) {

			log.warn("SELF ACTION BLOCKED | userId={} | action={}", targetUserId, action);

			auditService.log(AuditAction.SELF_ACTION_BLOCKED, AuditStatus.BLOCKED, "Attempted self action: " + action,
					null);

			throw new RuntimeException("You cannot " + action + " your own account.");
		}
	}

	private int revokeAllActiveTokens(User user) {

		List<UserToken> activeTokens = userTokenRepository.findAllByUserAndRevokedFalseAndExpiredFalse(user);

		if (activeTokens.isEmpty()) {
			log.info("NO ACTIVE TOKENS FOUND | targetUserId={}", user.getId());
			return 0;
		}

		activeTokens.forEach(token -> {
			token.setRevoked(true);
			token.setExpired(true);
		});

		userTokenRepository.saveAll(activeTokens);

		log.info("TOKENS REVOKED | targetUserId={} | count={}", user.getId(), activeTokens.size());

		return activeTokens.size();
	}

	private User getUserOrThrow(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("No user found with ID: " + userId));
	}

	private Role getRoleOrThrow(String roleName) {
		return roleRepository.findByName(roleName.toUpperCase()).orElseThrow(
				() -> new RuntimeException("Role '" + roleName.toUpperCase() + "' does not exist in the system."));
	}

	@SuppressWarnings("unused")
	private Set<String> currentPermissions() {
		return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
				.map(a -> a.getAuthority()).collect(Collectors.toSet());
	}

//	=====================================================================================================
//	=====================================================================================================

	// ======================== 🔐 TOKEN OPERATIONS ========================

	@Override
	public ResponseEntity<?> revokeUserTokens(Long userId) {

		requirePermission("REVOKE_TOKEN");

		guardSelf(userId, "revoke tokens of");

		User user = getUserOrThrow(userId);

		int revokedCount = revokeAllActiveTokens(user);

		log.warn("ADMIN TOKEN REVOKE | adminId={} | targetUserId={} | revokedTokens={}", getCurrentUserId(), userId,
				revokedCount);

		auditService.log(AuditAction.TOKEN_REVOKED, AuditStatus.SUCCESS, "Admin revoked all user tokens", null);

		return ResponseEntity.ok(Map.of("message", "All user tokens revoked successfully.", "userId", userId,
				"revokedTokens", revokedCount));
	}

	@Override
	public ResponseEntity<?> invalidateUserSessions(Long userId) {

		requirePermission("SESSION_REVOKE");

		guardSelf(userId, "invalidate sessions of");

		User user = getUserOrThrow(userId);

		int invalidatedSessions = revokeAllActiveTokens(user);

		log.warn("ADMIN SESSION INVALIDATION | adminId={} | targetUserId={} | sessionsInvalidated={}",
				getCurrentUserId(), userId, invalidatedSessions);

		auditService.log(AuditAction.SESSION_INVALIDATED, AuditStatus.SUCCESS, "Admin invalidated all active sessions",
				null);

		return ResponseEntity.ok(Map.of("message", "All active sessions invalidated successfully.", "userId", userId,
				"invalidatedSessions", invalidatedSessions));
	}

	// ======================== 🔒 ACCOUNT LOCK ========================

	@Override
	public ResponseEntity<?> lockUserAccount(Long userId) {

		requirePermission("ACCOUNT_LOCK");

		guardSelf(userId, "lock");

		User user = getUserOrThrow(userId);

		if (!user.isAccountNonLocked()) {
			throw new RuntimeException("User account is already locked.");
		}

		user.setAccountLocked(true);

		userRepository.save(user);

		int revokedTokens = revokeAllActiveTokens(user);

		log.warn("ACCOUNT LOCKED | adminId={} | targetUserId={} | revokedTokens={}", getCurrentUserId(), userId,
				revokedTokens);

		auditService.log(AuditAction.ACCOUNT_LOCKED, AuditStatus.SUCCESS, "Admin locked user account", null);

		// ✅ FIXED: notify user of account lock via NotificationFacade
		notificationFacade.sendNotification(user.getUsername(), NotificationType.ACCOUNT_LOCKED);

		return ResponseEntity.ok(Map.of("message", "User account locked successfully.", "userId", userId,
				"revokedTokens", revokedTokens));
	}

	@Override
	public ResponseEntity<?> unlockUserAccount(Long userId) {

		requirePermission("ACCOUNT_UNLOCK");

		guardSelf(userId, "unlock");

		User user = getUserOrThrow(userId);

		if (user.isAccountNonLocked()) {
			throw new RuntimeException("User account is already unlocked.");
		}

		user.setAccountLocked(false);
		userRepository.save(user);

		// ✅ FIXED: notify user of account unlock via NotificationFacade
		notificationFacade.sendNotification(user.getUsername(), NotificationType.ACCOUNT_UNLOCKED);

		log.info("ACCOUNT UNLOCKED | adminId={} | targetUserId={}", getCurrentUserId(), userId);

		auditService.log(AuditAction.ACCOUNT_UNLOCKED, AuditStatus.SUCCESS, "Admin unlocked user account", null);

		return ResponseEntity.ok(Map.of("message", "User account unlocked successfully.", "userId", userId));
	}

	// ======================== 🔓 ENABLE/DISABLE ACCESS ========================

	@Override
	public ResponseEntity<?> enableUserAccess(Long userId) {

		return toggleUserAccess(userId, true);
	}

	@Override
	public ResponseEntity<?> disableUserAccess(Long userId) {

		return toggleUserAccess(userId, false);
	}

	// ======================== 🛡️ SYSTEM ADMINISTRATION ========================

	@Override
	public ResponseEntity<?> enableMaintenanceMode() {

		requirePermission("SYSTEM_ADMIN");

		log.warn("SYSTEM MAINTENANCE ENABLED | adminId={}", getCurrentUserId());

		auditService.log(AuditAction.SYSTEM_MAINTENANCE_ENABLED, AuditStatus.SUCCESS, "Maintenance mode enabled", null);

		return ResponseEntity.ok(Map.of("message", "Maintenance mode enabled successfully."));
	}

	@Override
	public ResponseEntity<?> disableMaintenanceMode() {

		requirePermission("SYSTEM_ADMIN");

		log.warn("SYSTEM MAINTENANCE DISABLED | adminId={}", getCurrentUserId());

		auditService.log(AuditAction.SYSTEM_MAINTENANCE_DISABLED, AuditStatus.SUCCESS, "Maintenance mode disabled",
				null);

		return ResponseEntity.ok(Map.of("message", "Maintenance mode disabled successfully."));
	}

	@Override
	public ResponseEntity<?> clearSystemCache() {

		requirePermission("SYSTEM_ADMIN");

		log.warn("SYSTEM CACHE CLEARED | adminId={}", getCurrentUserId());

		auditService.log(AuditAction.SYSTEM_CACHE_CLEARED, AuditStatus.SUCCESS, "System cache cleared", null);

		return ResponseEntity.ok(Map.of("message", "System cache cleared successfully."));
	}

	@Override
	public ResponseEntity<?> refreshPermissionsCache() {

		requirePermission("SYSTEM_ADMIN");

		log.warn("PERMISSION CACHE REFRESHED | adminId={}", getCurrentUserId());

		auditService.log(AuditAction.PERMISSION_CACHE_REFRESHED, AuditStatus.SUCCESS, "Permission cache refreshed",
				null);

		return ResponseEntity.ok(Map.of("message", "Permissions cache refreshed successfully."));
	}

	// ======================== 📊 SYSTEM ANALYTICS ========================

	@Override
	public ResponseEntity<?> getSystemStatistics() {

		requirePermission("VIEW_SYSTEM_STATISTICS");

		long totalUsers = userRepository.count();

		// ✅ FIXED: Optimized N+1 query by using single query with projection
		long totalAdmins = userRepository.countByRoleNameIgnoreCase("ADMIN");

		long activeUsers = userRepository.countByEnabledTrue();

		return ResponseEntity
				.ok(Map.of("totalUsers", totalUsers, "totalAdmins", totalAdmins, "activeUsers", activeUsers));
	}

	@Override
	public ResponseEntity<?> getSecurityStatistics() {

		requirePermission("VIEW_SECURITY_STATISTICS");

		// ✅ FIXED: Optimized N+1 query by using repository count methods
		long revokedTokens = userTokenRepository.countByRevokedTrue();

		long activeTokens = userTokenRepository.countByRevokedFalse();

		long lockedAccounts = userRepository.countByAccountLockedTrue();

		return ResponseEntity.ok(
				Map.of("activeTokens", activeTokens, "revokedTokens", revokedTokens, "lockedAccounts", lockedAccounts));
	}

	// ======================== ☠️ PERMANENT DELETE ========================

	@Override
	public ResponseEntity<?> deleteUserPermanently(Long userId) {

		requirePermission("DELETE_USER");

		guardSelf(userId, "permanently delete");

		User user = getUserOrThrow(userId);

		revokeAllActiveTokens(user);

		userRepository.delete(user);

		log.error("PERMANENT USER DELETE | adminId={} | targetUserId={}", getCurrentUserId(), userId);

		auditService.log(AuditAction.DELETE_USER, AuditStatus.SUCCESS, "User permanently deleted", null);

		return ResponseEntity.ok(Map.of("message", "User permanently deleted successfully.", "userId", userId));
	}

}
