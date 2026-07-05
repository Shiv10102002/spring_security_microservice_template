package com.shiv.springboot_security_jwt_rbac_app1.service;

import org.springframework.http.ResponseEntity;

import com.shiv.springboot_security_jwt_rbac_app1.model.EmployeeRegisterRequest;
import com.shiv.springboot_security_jwt_rbac_app1.model.RegisterRequest;

/**
 * Handles all self-service user operations: registration, profile, OTP, and
 * password management.
 */
public interface IUserService {

	ResponseEntity<?> getAllUsers();

	// REGISTRATION
	ResponseEntity<?> sendRegistrationOtp(String email);

	ResponseEntity<?> registerWithOtp(RegisterRequest request);

	// PROFILE
	ResponseEntity<?> getCurrentUserProfile();

	// PASSWORD
	ResponseEntity<?> changePassword(String currentPassword, String newPassword, String confirmPassword);

	ResponseEntity<?> forgotPassword(String username);

	ResponseEntity<?> resetPassword(String username, String otp, String newPassword);

	// ======================== 🧑‍💼 EMPLOYEE REGISTRATION ========================
	ResponseEntity<?> employeeRegistration(EmployeeRegisterRequest request);

}