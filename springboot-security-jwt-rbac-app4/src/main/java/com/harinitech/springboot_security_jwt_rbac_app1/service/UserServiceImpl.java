package com.harinitech.springboot_security_jwt_rbac_app1.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.harinitech.springboot_security_jwt_rbac_app1.client.NotificationFacade;
import com.harinitech.springboot_security_jwt_rbac_app1.dto.NotificationType;
import com.harinitech.springboot_security_jwt_rbac_app1.entity.Role;
import com.harinitech.springboot_security_jwt_rbac_app1.entity.User;
import com.harinitech.springboot_security_jwt_rbac_app1.entity.UserToken;
import com.harinitech.springboot_security_jwt_rbac_app1.model.AuditAction;
import com.harinitech.springboot_security_jwt_rbac_app1.model.AuditStatus;
import com.harinitech.springboot_security_jwt_rbac_app1.model.EmployeeRegisterRequest;
import com.harinitech.springboot_security_jwt_rbac_app1.model.OtpPurpose;
import com.harinitech.springboot_security_jwt_rbac_app1.model.RedisOtpData;
import com.harinitech.springboot_security_jwt_rbac_app1.model.RegisterRequest;
import com.harinitech.springboot_security_jwt_rbac_app1.model.Status;
import com.harinitech.springboot_security_jwt_rbac_app1.model.UserResponseDto;
import com.harinitech.springboot_security_jwt_rbac_app1.passwordreset.PasswordResetTokenRepository;
import com.harinitech.springboot_security_jwt_rbac_app1.repo.OtpRepository;
import com.harinitech.springboot_security_jwt_rbac_app1.repo.RoleRepository;
import com.harinitech.springboot_security_jwt_rbac_app1.repo.UserRepository;
import com.harinitech.springboot_security_jwt_rbac_app1.repo.UserTokenRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * ✅ PRODUCTION UserServiceImpl — fully userId-based.
 *
 * Principal contract (matches JwtFilter + all other services): SecurityContext
 * principal → userId (Long as String) getCurrentUser() →
 * Long.parseLong(principal) → userRepository.findById()
 *
 * This means if a user changes their email (username), the active session
 * continues working without any interruption.
 */
@Slf4j
@Transactional
@Service
public class UserServiceImpl implements IUserService {

	// ======================== DEPENDENCIES ========================

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private OtpRepository otpRepository;

	@Autowired
	private UserTokenRepository userTokenRepository;

	@Autowired
	private PasswordResetTokenRepository passwordResetTokenRepository;

	// ✅ FIXED: inject NotificationFacade, NOT NotificationClient directly.
//  NotificationFacade provides typed overloads and fire-and-tolerate handling.
	@Autowired
	private NotificationFacade notificationFacade;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AuditService auditService;

	@Autowired
	private RedisOtpService redisOtpService;

	// ======================== 👥 USERS ========================

	@Override
	@Transactional(readOnly = true)
	public ResponseEntity<?> getAllUsers() {

		List<UserResponseDto> users = userRepository.findAll().stream()
				.map(user -> UserResponseDto.builder().userId(user.getId()).username(user.getUsername())
						.role(user.getRole().getName()).status(user.getStatus()).enabled(user.isEnabled())
						.lastLoginAt(user.getLastLoginAt()).build())
				.toList();

		log.info("USERS FETCHED | requestedByUserId={} | count={}", getCurrentUser().getId(), users.size());

		auditService.log(AuditAction.VIEW_USERS, AuditStatus.SUCCESS, "Fetched all users successfully", null);

		return ResponseEntity.ok(users);
	}

	// ======================== 📩 OTP / REGISTRATION ========================

	@Override
	public ResponseEntity<?> sendRegistrationOtp(String email) {

		String normalizedEmail = normalize(email);

		if (userRepository.findByUsername(normalizedEmail).isPresent()) {
			return ResponseEntity.badRequest().body("An account with email '" + normalizedEmail + "' already exists.");
		}

		String otp = generateOtp();

//		OtpToken token = OtpToken.builder().username(normalizedEmail).otpHash(passwordEncoder.encode(otp)) // ✅ HASHED
//																											// OTP
//				.expiryTime(Instant.now().plusSeconds(300)) // ✅ Instant
//				.used(false).attempts(0).purpose(OtpPurpose.REGISTER) // ✅ PURPOSE
//				.build();
//
//		otpRepository.invalidateAllActiveOtps(normalizedEmail);
//
//		otpRepository.save(token);

		// Using Redis for OTP storage instead of DB for better performance and
		// auto-expiry handling
		RedisOtpData redisOtp = RedisOtpData.builder().username(normalizedEmail).otpHash(passwordEncoder.encode(otp))
				.purpose(OtpPurpose.REGISTER).expiryTime(Instant.now().plusSeconds(300)).attempts(0).build();

		redisOtpService.saveOtp(redisOtp);

		// ✅ FIXED: use NotificationFacade with proper NotificationType enum
		notificationFacade.sendNotification(normalizedEmail, NotificationType.REGISTRATION_OTP, otp);

		log.info("OTP SENT | email={} | purpose=REGISTER", normalizedEmail);

		auditService.log(AuditAction.REGISTRATION_OTP_SENT, AuditStatus.SUCCESS, "Registration OTP sent successfully",
				null);

		return ResponseEntity
				.ok(Map.of("message", "OTP sent successfully", "email", normalizedEmail, "expirySeconds", 300));
	}

	@Override
	public ResponseEntity<?> registerWithOtp(RegisterRequest request) {

		validateOtp(request.getUsername(), request.getOtp());

		if (userRepository.findByUsername(normalize(request.getUsername())).isPresent()) {
			throw new RuntimeException("User already exists.");
		}

		createUser(normalize(request.getUsername()), request.getPassword(), "USER");

		log.info("USER REGISTERED | email={}", request.getUsername());

		auditService.log(AuditAction.REGISTER, AuditStatus.SUCCESS, "New user registered successfully", null);

		return ResponseEntity.ok(Map.of("message", "User registered successfully", "username", request.getUsername()));
	}

	// ======================== 👤 PROFILE ========================

	@Override
	public ResponseEntity<?> getCurrentUserProfile() {

		User user = getCurrentUser();

		UserResponseDto dto = UserResponseDto.builder().userId(user.getId()).username(user.getUsername())
				.role(user.getRole().getName()).status(user.getStatus()).enabled(user.isEnabled())
				.lastLoginAt(user.getLastLoginAt()).build();

		auditService.log(AuditAction.VIEW_PROFILE, AuditStatus.SUCCESS, "User viewed profile", null);

		return ResponseEntity.ok(dto);
	}
	// ======================== 🔑 PASSWORD ========================

	@Override
	public ResponseEntity<?> changePassword(String currentPassword, String newPassword, String confirmPassword) {

		User user = getCurrentUser();

		// ========================
		// VALIDATE CURRENT PASSWORD
		// ========================

		if (!passwordEncoder.matches(currentPassword, user.getPassword())) {

			log.warn("CHANGE PASSWORD FAILED | Invalid current password | userId={}", user.getId());

			return ResponseEntity.badRequest().body("Current password is incorrect.");
		}

		// ========================
		// CONFIRM PASSWORD MATCH
		// ========================

		if (!newPassword.equals(confirmPassword)) {

			log.warn("CHANGE PASSWORD FAILED | Password mismatch | userId={}", user.getId());

			return ResponseEntity.badRequest().body("New password and confirm password do not match.");
		}

		// ========================
		// PREVENT SAME PASSWORD REUSE
		// ========================

		if (passwordEncoder.matches(newPassword, user.getPassword())) {

			log.warn("CHANGE PASSWORD FAILED | Same password reuse attempt | userId={}", user.getId());

			return ResponseEntity.badRequest().body("New password cannot be same as current password.");
		}

		// ========================
		// UPDATE PASSWORD
		// ========================

		user.setPassword(passwordEncoder.encode(newPassword));

		// security metadata
		user.setPasswordChangedAt(Instant.now());

		// reset forced password change flag
		user.setForcePasswordChange(false);

		userRepository.save(user);

		// ========================
		// REVOKE ALL ACTIVE SESSIONS
		// ========================

		revokeAllActiveTokens(user);

		// clear current authentication
		SecurityContextHolder.clearContext();

		log.info("PASSWORD CHANGED SUCCESSFULLY | userId={}", user.getId());

		auditService.log(AuditAction.PASSWORD_CHANGED, AuditStatus.SUCCESS, "Password changed successfully", null);

		// ✅ FIXED: notify user via NotificationFacade with proper NotificationType
		notificationFacade.sendNotification(user.getUsername(), NotificationType.PASSWORD_CHANGED);

		return ResponseEntity
				.ok(Map.of("message", "Password changed successfully. Please login again.", "userId", user.getId()));
	}

	@Override
	public ResponseEntity<?> forgotPassword(String username) {

		User user = userRepository.findByUsername(normalize(username)).orElse(null);

		if (user == null) {
			// Do NOT reveal user existence
			return ResponseEntity.ok("If the account exists, an OTP has been sent.");
		}

		String otp = generateOtp();

//		OtpToken token = OtpToken.builder().username(user.getUsername()).otpHash(passwordEncoder.encode(otp))
//				.expiryTime(Instant.now().plusSeconds(300)).attempts(0).used(false).purpose(OtpPurpose.RESET_PASSWORD)
//				.build();
//
//		otpRepository.save(token);

		// Using Redis for OTP storage instead of DB for better performance and
		// auto-expiry handling
		RedisOtpData redisOtp = RedisOtpData.builder().username(user.getUsername()).otpHash(passwordEncoder.encode(otp))
				.purpose(OtpPurpose.RESET_PASSWORD).expiryTime(Instant.now().plusSeconds(300)).attempts(0).build();

		redisOtpService.saveOtp(redisOtp);

		// ✅ FIXED: use NotificationFacade with proper NotificationType enum
		notificationFacade.sendNotification(user.getUsername(), NotificationType.PASSWORD_RESET_OTP, otp);

		log.info("RESET OTP REQUEST | email={} | userExists=true", username);

		auditService.log(AuditAction.PASSWORD_RESET_REQUEST, AuditStatus.SUCCESS, "Password reset OTP requested", null);

		return ResponseEntity.ok("Password reset OTP sent. It expires in 5 minutes.");
	}

	@Override
	public ResponseEntity<?> resetPassword(String username, String otp, String newPassword) {

		validateOtpForPurpose(username, otp, OtpPurpose.RESET_PASSWORD);

		User user = getUserByUsername(username);

		if (passwordEncoder.matches(newPassword, user.getPassword())) {
			throw new RuntimeException("New password cannot be same as old password.");
		}

		user.setPassword(passwordEncoder.encode(newPassword));
		user.setPasswordChangedAt(Instant.now());

		userRepository.save(user);

		revokeAllActiveTokens(user);
		SecurityContextHolder.clearContext();

		log.info("PASSWORD RESET SUCCESS | userId={}", user.getId());

		auditService.log(AuditAction.PASSWORD_RESET, AuditStatus.SUCCESS, "Password reset successfully", null);

		// ✅ FIXED: notify user of successful password reset
		notificationFacade.sendNotification(user.getUsername(), NotificationType.PASSWORD_RESET_COMPLETED);

		return ResponseEntity.ok(Map.of("message", "Password reset successfully", "userId", user.getId()));
	}

	private void validateOtpForPurpose(String email, String otpInput, OtpPurpose purpose) {

//		OtpToken token = otpRepository.findTopByUsernameOrderByIdDesc(normalize(email))
//				.orElseThrow(() -> new RuntimeException("No OTP found."));

		RedisOtpData token = redisOtpService.getOtp(normalize(email), purpose)
				.orElseThrow(() -> new RuntimeException("No OTP found."));

		email = normalize(email);

		if (token.getAttempts() >= 5) {

			log.warn("OTP BLOCKED | email={} | attempts={}", email, token.getAttempts());

			throw new RuntimeException("Too many OTP attempts. Request new OTP.");
		}

		if (token.getPurpose() != purpose) {
			log.warn("OTP PURPOSE MISMATCH | email={} | expected={} | actual={}", email, purpose, token.getPurpose());
			throw new RuntimeException("Invalid OTP type.");
		}

//		if (token.isUsed()) {
//			log.warn("OTP REUSE DETECTED | email={}", email);
//			throw new RuntimeException("OTP already used.");
//		}

		if (token.getExpiryTime().isBefore(Instant.now())) {
			log.warn("OTP EXPIRED | email={}", email);
			throw new RuntimeException("OTP has expired.");
		}

		if (!passwordEncoder.matches(otpInput, token.getOtpHash())) {

			token.setAttempts(token.getAttempts() + 1);
			token.setLastAttemptAt(Instant.now());

//			otpRepository.save(token);
			redisOtpService.updateOtp(token);

			log.warn("OTP FAILED | email={} | attempts={}", email, token.getAttempts());

			auditService.log(AuditAction.OTP_FAILED, AuditStatus.FAILED, "Invalid OTP entered", null);

			throw new RuntimeException("Incorrect OTP.");
		}
//		token.setUsed(true);
//		otpRepository.invalidateAllActiveOtps(email);
//		otpRepository.save(token);

		redisOtpService.deleteOtp(email, purpose);
	}

	// ======================== 🧑‍💼 EMPLOYEE REGISTRATION ========================

	@Override
	public ResponseEntity<?> employeeRegistration(EmployeeRegisterRequest request) {
		String email = normalize(request.getEmail());

		// 1. Validate OTP (reuse existing registration OTP validation)
		validateOtp(email, request.getOtp());

		// 2. Prevent duplicate accounts
		if (userRepository.findByUsername(email).isPresent()) {
			throw new RuntimeException("An account with this email already exists.");
		}

		// 3. Whitelist allowed employee roles
		Set<String> allowedRoles = Set.of("EMPLOYEE", "HR", "VENDOR", "SUPPORT", "MANAGER");
		String requestedRole = request.getRequestedRole().toUpperCase();
		if (!allowedRoles.contains(requestedRole)) {
			throw new RuntimeException("Invalid role requested. Allowed: " + allowedRoles);
		}

		// 4. Create user in PENDING state (minimal role = USER while pending)
		Role placeholderRole = getRole("USER");
		User user = new User();

		user.setUsername(email);
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setRole(placeholderRole);
		user.setStatus(Status.PENDING_APPROVAL);
		user.setEnabled(false);
		user.setAccountLocked(false);
		user.setForcePasswordChange(false);
		user.setFailedLoginAttempts(0);
		user.setPasswordChangedAt(Instant.now());
		user.setRequestedRole(requestedRole);

		userRepository.save(user);

		// 5. Notify the employee that registration was submitted
		notificationFacade.sendNotification(email, NotificationType.EMPLOYEE_REGISTRATION_SUBMITTED);

		// 6. Notify all ADMINs about new pending registration
		notifyApproversOfNewRegistration(user);

		log.info("EMPLOYEE REGISTRATION SUBMITTED | email={} | requestedRole={}", email, requestedRole);

		auditService.log(AuditAction.REGISTER, AuditStatus.SUCCESS,
				"Employee registration submitted for role " + requestedRole, null);

		return ResponseEntity.ok(Map.of("message",
				"Registration submitted. You will receive a confirmation once approved.", "email", email));
	}

	// Helper: notify all users with ADMIN role
	private void notifyAdminsOfNewRegistration(User pendingUser) {

		List<User> admins = userRepository.findAll().stream()
				.filter(u -> u.getRole() != null && "ADMIN".equals(u.getRole().getName())).toList();

		// ✅ FIXED: use NotificationFacade.sendPendingApprovalAlertToAdmin()
//	    with employeeEmail and requestedRole context fields
		admins.forEach(admin -> notificationFacade.sendPendingApprovalAlertToAdmin(admin.getUsername(),
				pendingUser.getUsername(), pendingUser.getRequestedRole()));

	}

	// ======================== 🧑‍💼 EMPLOYEE REGISTRATION ========================

	// ======================== 🧰 PRIVATE HELPERS ========================

	private void notifyApproversOfNewRegistration(User pendingUser) {

		String approverRole = "MANAGER".equalsIgnoreCase(pendingUser.getRequestedRole()) ? "ADMIN" : "MANAGER";

		List<User> approvers = userRepository.findAll().stream()
				.filter(user -> user.getRole() != null && approverRole.equalsIgnoreCase(user.getRole().getName()))
				.filter(User::isEnabled).filter(user -> user.getStatus() == Status.ACTIVE).toList();

		approvers.forEach(approver -> notificationFacade.sendPendingApprovalAlertToAdmin(approver.getUsername(),
				pendingUser.getUsername(), pendingUser.getRequestedRole()));

	}

	private void validateOtp(String email, String otpInput) {

//		OtpToken token = otpRepository.findTopByUsernameOrderByIdDesc(normalize(email))
//				.orElseThrow(() -> new RuntimeException("No OTP found for '" + email + "'. Please request a new OTP."));

		RedisOtpData token = redisOtpService.getOtp(normalize(email), OtpPurpose.REGISTER)
				.orElseThrow(() -> new RuntimeException("No OTP found for '" + email + "'. Please request a new OTP."));

		String normalizedEmail = normalize(email);

		// ✅ PURPOSE CHECK
		if (token.getPurpose() != OtpPurpose.REGISTER) {
			throw new RuntimeException("Invalid OTP type.");
		}

//		if (token.isUsed()) {
//			throw new RuntimeException("This OTP has already been used.");
//		}

		if (token.getExpiryTime().isBefore(Instant.now())) {
			throw new RuntimeException("OTP has expired.");
		}

		// ✅ ATTEMPT TRACKING
		if (token.getAttempts() >= 5) {
			log.warn("OTP BLOCKED | email={} | attempts={}", email, token.getAttempts());
			throw new RuntimeException("Too many attempts. Request new OTP.");
		}

		// ✅ HASH CHECK
		if (!passwordEncoder.matches(otpInput, token.getOtpHash())) {

			token.setAttempts(token.getAttempts() + 1);
			token.setLastAttemptAt(Instant.now());
//			otpRepository.save(token);
			redisOtpService.updateOtp(token);

			log.warn("OTP FAILED | email={} | attempts={}", normalizedEmail, token.getAttempts());

			auditService.log(AuditAction.OTP_FAILED, AuditStatus.FAILED, "Invalid registration OTP entered", null);

			throw new RuntimeException("Incorrect OTP.");
		}

//		otpRepository.invalidateAllActiveOtps(normalizedEmail);
//
//		token.setUsed(true);
//
//		otpRepository.save(token);

		redisOtpService.deleteOtp(normalizedEmail, OtpPurpose.REGISTER);

		log.info("OTP VERIFIED SUCCESS | email={} | purpose=REGISTER", email);
	}

	private User createUser(String email, String password, String roleName) {

		Role role = getRole(roleName);

		User user = new User();
		user.setUsername(normalize(email));
		user.setPassword(passwordEncoder.encode(password));
		user.setRole(role);
		user.setStatus(Status.ACTIVE);
		user.setEnabled(true);

		// ✅ NEW FIELDS INITIALIZATION
		user.setAccountLocked(false);
		user.setFailedLoginAttempts(0);
		user.setForcePasswordChange(false);
		user.setPasswordChangedAt(Instant.now());

		User savedUser = userRepository.save(user);

		log.info("USER CREATED | userId={} | role={}", savedUser.getId(), roleName);

		return savedUser;
	}

	private void revokeAllActiveTokens(User user) {
		List<UserToken> activeTokens = userTokenRepository.findAllByUserAndRevokedFalseAndExpiredFalse(user);
		if (activeTokens.isEmpty())
			return;
		activeTokens.forEach(t -> {
			t.setRevoked(true);
			t.setExpired(true);
		});

		log.info("TOKENS REVOKED | userId={} | count={}", user.getId(), activeTokens.size());

		userTokenRepository.saveAll(activeTokens);
	}

	private User getUserByUsername(String username) {
		return userRepository.findByUsername(normalize(username))
				.orElseThrow(() -> new RuntimeException("No account found for username: " + username));
	}

	/**
	 * ✅ Returns the currently authenticated user from the SecurityContext.
	 *
	 * JwtFilter sets userId (Long as String) as the principal. We parse it as Long
	 * and fetch by ID — NOT by username.
	 *
	 * This is username-change-safe: If the user updates their email mid-session,
	 * this lookup still works because userId (DB PK) never changes.
	 */
	private User getCurrentUser() {

		if (SecurityContextHolder.getContext().getAuthentication() == null
				|| SecurityContextHolder.getContext().getAuthentication().getPrincipal() == null) {

			log.error("SECURITY CONTEXT EMPTY");

			throw new RuntimeException("Authentication required.");
		}

		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		Long userId;

		try {

			userId = Long.parseLong(principal.toString());

		} catch (NumberFormatException e) {

			log.error("INVALID PRINCIPAL FORMAT | principal={}", principal);

			throw new RuntimeException("Invalid authentication principal.");
		}

		return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Authenticated user not found."));
	}

	private Role getRole(String roleName) {
		return roleRepository.findByName(roleName.toUpperCase())
				.orElseThrow(() -> new RuntimeException("Role '" + roleName + "' does not exist in the system."));
	}

	private static final SecureRandom secureRandom = new SecureRandom();

	private String generateOtp() {
		int otp = secureRandom.nextInt(900000) + 100000;
		return String.valueOf(otp);
	}

	private String normalize(String value) {
		return value == null ? null : value.trim().toLowerCase();
	}
}
