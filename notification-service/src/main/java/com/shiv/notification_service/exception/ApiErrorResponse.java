package com.shiv.notification_service.exception;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Builder;

@Builder
public record ApiErrorResponse(

		LocalDateTime timestamp,

		int status,

		String error,

		String message,

		String path,

		Map<String, String> validationErrors

) {
}