package com.shiv.notification_service.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiResponse<T> {

	private boolean success;

	private String message;

	private T data;

	@Builder.Default
	private LocalDateTime timestamp = LocalDateTime.now();

	public static <T> ApiResponse<T> success(String message, T data) {

		return ApiResponse.<T>builder().success(true).message(message).data(data).build();
	}

	public static <T> ApiResponse<T> failure(String message) {

		return ApiResponse.<T>builder().success(false).message(message).build();
	}
}