package com.shiv.springboot_security_jwt_rbac_app1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

	// 🔥 Access Token (short-lived)
	private String accessToken;

	// 🔥 Refresh Token (long-lived)
	private String refreshToken;

	// 🔥 User Role
	private String role;
}