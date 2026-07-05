package com.shiv.springboot_security_jwt_rbac_app1.exceptions;

public class NotificationServiceException extends RuntimeException {

	public NotificationServiceException(String message) {
		super(message);
	}

	public NotificationServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}