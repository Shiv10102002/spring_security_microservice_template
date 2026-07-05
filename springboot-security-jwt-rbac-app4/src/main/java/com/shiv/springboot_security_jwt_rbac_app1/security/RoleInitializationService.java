package com.shiv.springboot_security_jwt_rbac_app1.security;

import java.util.List;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import com.shiv.springboot_security_jwt_rbac_app1.entity.Permission;
import com.shiv.springboot_security_jwt_rbac_app1.entity.Role;
import com.shiv.springboot_security_jwt_rbac_app1.repo.PermissionRepository;
import com.shiv.springboot_security_jwt_rbac_app1.repo.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Order(1)
public class RoleInitializationService implements CommandLineRunner {

	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;

	@Override
	public void run(String... args) {

		List<String> allPermissions = List.of("READ_USER", "CREATE_USER", "UPDATE_USER", "DELETE_USER", "VIEW_USERS",
				"UPDATE_USER_STATUS", "TOGGLE_USER_ACCESS", "ACCOUNT_LOCK", "ACCOUNT_UNLOCK", "FORCE_LOGOUT",
				"REVOKE_TOKEN", "SESSION_REVOKE", "ASSIGN_ADMIN", "ASSIGN_MANAGER", "ASSIGN_EMPLOYEE", "ASSIGN_HR",
				"ASSIGN_VENDOR", "ASSIGN_SUPPORT", "APPROVE_REGISTRATION", "REJECT_REGISTRATION",
				"VIEW_PENDING_REGISTRATIONS", "VIEW_AUDIT_LOGS", "VIEW_SECURITY_EVENTS", "VIEW_AUDIT_DASHBOARD",
				"VIEW_SECURITY_STATISTICS", "VIEW_SYSTEM_STATISTICS", "SYSTEM_ADMIN", "HELLO_USERS");

		allPermissions.forEach(name -> permissionRepository.findByName(name).orElseGet(() -> {
			Permission permission = new Permission();
			permission.setName(name);
			log.info("Permission created: {}", name);
			return permissionRepository.save(permission);
		}));

		Permission readUser = get("READ_USER");
		Permission createUser = get("CREATE_USER");
		Permission updateUser = get("UPDATE_USER");
		Permission deleteUser = get("DELETE_USER");
		Permission viewUsers = get("VIEW_USERS");

		Permission updateUserStatus = get("UPDATE_USER_STATUS");
		Permission toggleUserAccess = get("TOGGLE_USER_ACCESS");
		Permission accountLock = get("ACCOUNT_LOCK");
		Permission accountUnlock = get("ACCOUNT_UNLOCK");

		Permission forceLogout = get("FORCE_LOGOUT");
		Permission revokeToken = get("REVOKE_TOKEN");
		Permission sessionRevoke = get("SESSION_REVOKE");

		Permission assignAdmin = get("ASSIGN_ADMIN");
		Permission assignManager = get("ASSIGN_MANAGER");
		Permission assignEmployee = get("ASSIGN_EMPLOYEE");
		Permission assignHR = get("ASSIGN_HR");
		Permission assignVendor = get("ASSIGN_VENDOR");
		Permission assignSupport = get("ASSIGN_SUPPORT");

		Permission approveRegistration = get("APPROVE_REGISTRATION");
		Permission rejectRegistration = get("REJECT_REGISTRATION");
		Permission viewPendingRegistrations = get("VIEW_PENDING_REGISTRATIONS");

		Permission viewAuditLogs = get("VIEW_AUDIT_LOGS");
		Permission viewSecurityEvents = get("VIEW_SECURITY_EVENTS");
		Permission viewAuditDashboard = get("VIEW_AUDIT_DASHBOARD");
		Permission viewSecurityStatistics = get("VIEW_SECURITY_STATISTICS");
		Permission viewSystemStatistics = get("VIEW_SYSTEM_STATISTICS");

		Permission systemAdmin = get("SYSTEM_ADMIN");
		Permission helloUsers = get("HELLO_USERS");

		createOrUpdateRole("ADMIN",
				Set.of(readUser, createUser, updateUser, deleteUser, updateUserStatus, toggleUserAccess, forceLogout,
						assignAdmin, assignManager, assignEmployee, assignHR, assignVendor, assignSupport, viewUsers,
						viewAuditLogs, viewSecurityEvents, viewAuditDashboard, systemAdmin, viewSecurityStatistics,
						viewSystemStatistics, accountLock, accountUnlock, revokeToken, sessionRevoke,
						approveRegistration, rejectRegistration, viewPendingRegistrations, helloUsers));

		createOrUpdateRole("MANAGER",
				Set.of(readUser, createUser, updateUser, assignEmployee, assignHR, assignVendor, assignSupport,
						approveRegistration, rejectRegistration, viewPendingRegistrations, viewUsers, helloUsers));

		createOrUpdateRole("HR", Set.of(readUser, helloUsers));
		createOrUpdateRole("EMPLOYEE", Set.of(helloUsers));
		createOrUpdateRole("SUPPORT", Set.of(readUser, helloUsers));
		createOrUpdateRole("USER", Set.of(helloUsers));
		createOrUpdateRole("VENDOR", Set.of());
	}

	private Permission get(String name) {
		return permissionRepository.findByName(name)
				.orElseThrow(() -> new RuntimeException("Permission not found after init: " + name));
	}

	private void createOrUpdateRole(String roleName, Set<Permission> permissions) {
		Role role = roleRepository.findByName(roleName).orElseGet(() -> {
			Role newRole = new Role();
			newRole.setName(roleName);
			return newRole;
		});
		role.setPermissions(permissions);
		roleRepository.save(role);
		log.info("Role synced: {} with {} permissions", roleName, permissions.size());
	}
}
