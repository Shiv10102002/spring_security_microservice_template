package com.shiv.springboot_security_jwt_rbac_app1.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSummaryDto {

	private Long userId;
	private String username;
	private String role;
	private Status status;
	private boolean enabled;
}