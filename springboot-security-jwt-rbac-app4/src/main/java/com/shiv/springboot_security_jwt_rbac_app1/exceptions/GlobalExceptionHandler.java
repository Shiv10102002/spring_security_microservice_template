package com.shiv.springboot_security_jwt_rbac_app1.exceptions;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.shiv.springboot_security_jwt_rbac_app1.model.ErrorResponse;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	// ======================== 📋 VALIDATION ========================

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {

		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));

		log.warn("VALIDATION FAILED | path={} | errors={}", request.getRequestURI(), errors);

		return build(HttpStatus.BAD_REQUEST, "VALIDATION_400", errors.toString(), request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<?> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {

		log.warn("MALFORMED JSON | path={}", request.getRequestURI());

		return build(HttpStatus.BAD_REQUEST, "REQUEST_400", "Invalid or missing request body", request);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<?> handleMissingParam(MissingServletRequestParameterException ex,
			HttpServletRequest request) {

		return build(HttpStatus.BAD_REQUEST, "PARAM_400", "Missing parameter: " + ex.getParameterName(), request);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

		return build(HttpStatus.BAD_REQUEST, "TYPE_400",
				"Parameter '" + ex.getName() + "' must be of type " + ex.getRequiredType().getSimpleName(), request);
	}

	// ======================== 🔐 AUTH ========================

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<?> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {

		return build(HttpStatus.UNAUTHORIZED, "AUTH_401", "Invalid username or password", request);
	}

	@ExceptionHandler(UsernameNotFoundException.class)
	public ResponseEntity<?> handleUserNotFound(UsernameNotFoundException ex, HttpServletRequest request) {

		return build(HttpStatus.UNAUTHORIZED, "AUTH_401", "Invalid username or password", request);
	}

	@ExceptionHandler(DisabledException.class)
	public ResponseEntity<?> handleDisabled(DisabledException ex, HttpServletRequest request) {

		return build(HttpStatus.FORBIDDEN, "AUTH_403", "Account is disabled", request);
	}

	@ExceptionHandler(LockedException.class)
	public ResponseEntity<?> handleLocked(LockedException ex, HttpServletRequest request) {

		return build(HttpStatus.FORBIDDEN, "AUTH_403", "Account is locked", request);
	}

	// ======================== 🔑 JWT ========================

	@ExceptionHandler(ExpiredJwtException.class)
	public ResponseEntity<?> handleExpiredJwt(ExpiredJwtException ex, HttpServletRequest request) {

		return build(HttpStatus.UNAUTHORIZED, "JWT_401", "Token expired", request);
	}

	@ExceptionHandler({ MalformedJwtException.class, SignatureException.class })
	public ResponseEntity<?> handleInvalidJwt(Exception ex, HttpServletRequest request) {

		return build(HttpStatus.UNAUTHORIZED, "JWT_401", "Invalid token", request);
	}

	// ======================== 🚫 AUTHORIZATION ========================

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {

		log.warn("ACCESS DENIED | path={} | reason={}", request.getRequestURI(), ex.getMessage());

		return build(HttpStatus.FORBIDDEN, "ACCESS_403", "You do not have permission to perform this action", request);
	}

	// ======================== ⚙️ BUSINESS ========================

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<?> handleRuntime(RuntimeException ex, HttpServletRequest request) {

		log.warn("BUSINESS ERROR | path={} | message={}", request.getRequestURI(), ex.getMessage());

		return build(HttpStatus.BAD_REQUEST, "BUSINESS_400", ex.getMessage(), request);
	}

	// ======================== 💥 SYSTEM ========================

	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleGeneral(Exception ex, HttpServletRequest request) {

		log.error("SYSTEM ERROR | path={}", request.getRequestURI(), ex);

		return build(HttpStatus.INTERNAL_SERVER_ERROR, "SYS_500", "Something went wrong. Please try again later.",
				request);
	}

	// ======================== 🧰 BUILDER ========================

	private ResponseEntity<ErrorResponse> build(HttpStatus status, String errorCode, String message,
			HttpServletRequest request) {

		ErrorResponse response = ErrorResponse.builder().success(false).status(status.value()).errorCode(errorCode)
				.message(message).path(request.getRequestURI()).timestamp(Instant.now()).build();

		return ResponseEntity.status(status).body(response);
	}
}