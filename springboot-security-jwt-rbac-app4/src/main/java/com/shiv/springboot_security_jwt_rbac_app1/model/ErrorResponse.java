package com.shiv.springboot_security_jwt_rbac_app1.model;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {

	private boolean success;
	private int status;
	private String errorCode;
	private String message;
	private String path;
	private Instant timestamp;
}