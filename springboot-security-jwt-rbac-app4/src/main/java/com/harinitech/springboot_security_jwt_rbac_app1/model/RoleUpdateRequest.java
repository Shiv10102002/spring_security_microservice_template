package com.harinitech.springboot_security_jwt_rbac_app1.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleUpdateRequest {

	@NotBlank(message = "Role is required")
	private String role;

}
