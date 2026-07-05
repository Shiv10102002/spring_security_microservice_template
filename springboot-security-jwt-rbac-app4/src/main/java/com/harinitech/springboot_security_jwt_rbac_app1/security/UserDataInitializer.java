package com.harinitech.springboot_security_jwt_rbac_app1.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.harinitech.springboot_security_jwt_rbac_app1.entity.Role;
import com.harinitech.springboot_security_jwt_rbac_app1.entity.User;
import com.harinitech.springboot_security_jwt_rbac_app1.model.Status;
import com.harinitech.springboot_security_jwt_rbac_app1.repo.RoleRepository;
import com.harinitech.springboot_security_jwt_rbac_app1.repo.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Order(2)
@ConditionalOnProperty(name = "app.init.users", havingValue = "true")
@RequiredArgsConstructor
public class UserDataInitializer implements CommandLineRunner {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${app.init.default-password:ChangeMe@123!}")
	private String defaultPassword;

	@Override
	public void run(String... args) {

		log.info("🚀 Running UserDataInitializer...");

		createUsersForRole("ADMIN", List.of("admin1@test.com", "admin2@test.com", "admin3@test.com"));

		createUsersForRole("MANAGER", List.of("manager1@test.com", "manager2@test.com", "manager3@test.com"));

		createUsersForRole("HR", List.of("hr1@test.com", "hr2@test.com", "hr3@test.com"));

		createUsersForRole("USER", List.of("user1@test.com", "user2@test.com", "user3@test.com"));

		createUsersForRole("VENDOR", List.of("vendor1@test.com", "vendor2@test.com", "vendor3@test.com"));
	}

	// ==================== HELPER ====================

	private void createUsersForRole(String roleName, List<String> emails) {

		Role role = roleRepository.findByName(roleName)
				.orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

		for (String email : emails) {

			userRepository.findByUsername(email).orElseGet(() -> {

				User user = new User();
				user.setUsername(email);
				// ✅ FIXED: Use configurable default password from application.properties
				user.setPassword(passwordEncoder.encode(defaultPassword));
				user.setRole(role);
				user.setStatus(Status.ACTIVE);
				user.setEnabled(true);

				log.info("✅ Created {} user: {}", roleName, email);

				return userRepository.save(user);
			});
		}
	}
}