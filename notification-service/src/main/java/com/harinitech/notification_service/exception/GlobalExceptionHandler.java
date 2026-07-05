package com.harinitech.notification_service.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex,
			HttpServletRequest request) {

		log.warn("Resource not found: {}", ex.getMessage());

		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.NOT_FOUND.value())
						.error(HttpStatus.NOT_FOUND.getReasonPhrase()).message(ex.getMessage())
						.path(request.getRequestURI()).validationErrors(null).build());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
			HttpServletRequest request) {

		Map<String, String> errors = new HashMap<>();

		ex.getBindingResult().getAllErrors().forEach(error -> {

			String fieldName = ((FieldError) error).getField();

			String message = error.getDefaultMessage();

			errors.put(fieldName, message);
		});

		log.warn("Validation failed: {}", errors);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.BAD_REQUEST.value())
						.error(HttpStatus.BAD_REQUEST.getReasonPhrase()).message("Validation failed")
						.path(request.getRequestURI()).validationErrors(errors).build());
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(ConstraintViolationException ex,
			HttpServletRequest request) {

		Map<String, String> errors = new HashMap<>();

		for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {

			errors.put(violation.getPropertyPath().toString(), violation.getMessage());
		}

		log.warn("Constraint violation: {}", errors);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.BAD_REQUEST.value())
						.error(HttpStatus.BAD_REQUEST.getReasonPhrase()).message("Constraint violation")
						.path(request.getRequestURI()).validationErrors(errors).build());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex,
			HttpServletRequest request) {

		log.warn("Bad request: {}", ex.getMessage());

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiErrorResponse.builder().timestamp(LocalDateTime.now()).status(HttpStatus.BAD_REQUEST.value())
						.error(HttpStatus.BAD_REQUEST.getReasonPhrase()).message(ex.getMessage())
						.path(request.getRequestURI()).validationErrors(null).build());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {

		log.error("Unexpected error occurred", ex);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiErrorResponse.builder()
				.timestamp(LocalDateTime.now()).status(HttpStatus.INTERNAL_SERVER_ERROR.value())
				.error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()).message("An unexpected error occurred")
				.path(request.getRequestURI()).validationErrors(null).build());
	}
}