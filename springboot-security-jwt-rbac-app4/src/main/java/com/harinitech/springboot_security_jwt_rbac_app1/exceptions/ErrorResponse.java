package com.harinitech.springboot_security_jwt_rbac_app1.exceptions;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // ✅ Hides null fields from JSON response
public class ErrorResponse {

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private final LocalDateTime timestamp;

	private final int status;
	private final String error;
	private final String message;
	private final String path; // ✅ Optional: which endpoint caused the error

	// ======================== Standard (no path) ========================
	public ErrorResponse(int status, String error, String message) {
		this.timestamp = LocalDateTime.now();
		this.status = status;
		this.error = error;
		this.message = message;
		this.path = null;
	}

	// ======================== With path (optional use) ========================
	public ErrorResponse(int status, String error, String message, String path) {
		this.timestamp = LocalDateTime.now();
		this.status = status;
		this.error = error;
		this.message = message;
		this.path = path;
	}
}