package com.harinitech.springboot_security_jwt_rbac_app1.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

	@NotBlank
	@Email(message = "Invalid email")
	private String username; // email

	@NotBlank
	@Size(min = 8, message = "Password must be at least 8 characters")
	private String password;

	@NotBlank(message = "OTP is required")
	private String otp;

}