package com.harinitech.springboot_security_jwt_rbac_app1.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.harinitech.springboot_security_jwt_rbac_app1.model.ApiResponse;
import com.harinitech.springboot_security_jwt_rbac_app1.model.ChangePasswordRequest;
import com.harinitech.springboot_security_jwt_rbac_app1.model.EmailRequest;
import com.harinitech.springboot_security_jwt_rbac_app1.model.EmployeeRegisterRequest;
import com.harinitech.springboot_security_jwt_rbac_app1.model.RegisterRequest;
import com.harinitech.springboot_security_jwt_rbac_app1.passwordreset.ForgotPasswordRequest;
import com.harinitech.springboot_security_jwt_rbac_app1.passwordreset.ResetPasswordRequest;
import com.harinitech.springboot_security_jwt_rbac_app1.service.IUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

	private final IUserService userService;

	// ======================== 👥 USER MANAGEMENT ========================

	@PreAuthorize("hasAuthority('VIEW_USERS')")
	@GetMapping
	public ResponseEntity<ApiResponse<?>> getAllUsers() {

		log.info("USER API | Fetch all users");

		return ResponseEntity
				.ok(ApiResponse.success("Users fetched successfully", userService.getAllUsers().getBody()));
	}

	// ======================== 👤 PROFILE ========================

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<?>> getMyProfile() {

		log.info("USER API | Fetch profile");

		return ResponseEntity
				.ok(ApiResponse.success("Profile fetched successfully", userService.getCurrentUserProfile().getBody()));
	}

	// ======================== 📩 REGISTRATION ========================

	@PostMapping("/send-registration-otp")
	public ResponseEntity<ApiResponse<?>> sendRegistrationOtp(@Valid @RequestBody EmailRequest request) {

		log.info("USER API | Registration OTP request | email={}", request.getEmail());

		return ResponseEntity.ok(ApiResponse.success("Registration OTP sent successfully",
				userService.sendRegistrationOtp(request.getEmail()).getBody()));
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<?>> register(@Valid @RequestBody RegisterRequest request) {

		log.info("USER API | Registration attempt | email={}", request.getUsername());

		return ResponseEntity.ok(
				ApiResponse.success("User registered successfully", userService.registerWithOtp(request).getBody()));
	}

	// ======================== 🔑 PASSWORD ========================

	@PostMapping("/forgot-password")
	public ResponseEntity<ApiResponse<?>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {

		log.info("USER API | Forgot password request | email={}", request.getUsername());

		return ResponseEntity.ok(ApiResponse.success("Password reset OTP sent successfully",
				userService.forgotPassword(request.getUsername()).getBody()));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<ApiResponse<?>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {

		log.info("USER API | Reset password request | email={}", request.getUsername());

		return ResponseEntity.ok(ApiResponse.success("Password reset successful", userService
				.resetPassword(request.getUsername(), request.getOtp(), request.getNewPassword()).getBody()));
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/change-password")
	public ResponseEntity<ApiResponse<?>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {

		log.info("USER API | Change password request");

		return ResponseEntity.ok(ApiResponse.success("Password changed successfully", userService
				.changePassword(request.getCurrentPassword(), request.getNewPassword(), request.getConfirmPassword())
				.getBody()));
	}

	@PostMapping("/employee-register")
	public ResponseEntity<ApiResponse<?>> employeeRegister(@Valid @RequestBody EmployeeRegisterRequest request) {

		log.info("USER API | Employee registration attempt | email={}", request.getEmail());

		return ResponseEntity
				.ok(ApiResponse.success("Registration submitted", userService.employeeRegistration(request).getBody()));
	}
}