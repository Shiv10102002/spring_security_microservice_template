package com.harinitech.springboot_security_jwt_rbac_app1.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

	@NotBlank(message = "Current password is required")
	private String currentPassword;

	@NotBlank(message = "New password is required")
	@Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
	@Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=!]).*$", message = "Password must contain uppercase, lowercase, number and special character")
	private String newPassword;

	@NotBlank(message = "Confirm password is required")
	private String confirmPassword;
}