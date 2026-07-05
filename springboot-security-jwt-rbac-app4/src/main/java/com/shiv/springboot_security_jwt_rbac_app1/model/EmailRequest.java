package com.shiv.springboot_security_jwt_rbac_app1.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailRequest {

	@NotBlank
	@Email(message = "Invalid email")
	private String email;


}
