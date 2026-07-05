// -- JwtRequest.java --
package com.shiv.springboot_security_jwt_rbac_app1.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtRequest {

	@NotBlank(message = "Email is required")
	@Email(message = "Invalid email format")
	private String username;

	@NotBlank
	private String password;
}