package com.harinitech.springboot_security_jwt_rbac_app1.model;

import java.time.Instant;

import com.harinitech.springboot_security_jwt_rbac_app1.model.Status;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDto {

	private Long userId;
	private String username;

	private String role;
	private Status status;
	private boolean enabled;

	private boolean accountLocked;
	private int failedLoginAttempts;

	private Instant lastLoginAt;
	private String lastLoginIp;
	private String lastLoginDevice;

	private Instant passwordChangedAt;
}