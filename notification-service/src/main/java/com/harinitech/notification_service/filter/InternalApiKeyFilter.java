package com.harinitech.notification_service.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

	private static final String API_KEY_HEADER = "X-Internal-Api-Key";
	private static final int MIN_KEY_LENGTH = 32;

	private final ObjectMapper objectMapper;

	@Value("${notification.internal-api-key}")
	private String expectedApiKey;

	public InternalApiKeyFilter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@PostConstruct
	void validateConfiguration() {

		if (!StringUtils.hasText(expectedApiKey)) {
			throw new IllegalStateException(
					"Internal API key is not configured. Set INTERNAL_API_KEY environment variable "
							+ "or notification.internal-api-key in application-secret.properties.");
		}

		if (expectedApiKey.trim().length() < MIN_KEY_LENGTH) {
			throw new IllegalStateException(
					"Internal API key is too short. Use at least " + MIN_KEY_LENGTH + " characters.");
		}

		log.info("Internal API key filter initialized");
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {

		String path = request.getRequestURI();

		return path.startsWith("/actuator") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")
				|| path.startsWith("/api-docs");
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		String providedKey = request.getHeader(API_KEY_HEADER);

		if (!StringUtils.hasText(providedKey)) {

			log.warn("INTERNAL API KEY MISSING | uri={} | remoteAddr={}", request.getRequestURI(),
					request.getRemoteAddr());

			sendUnauthorized(response, "Missing X-Internal-Api-Key header");

			return;
		}

		if (!isValidApiKey(providedKey)) {

			log.warn("INTERNAL API KEY INVALID | uri={} | remoteAddr={}", request.getRequestURI(),
					request.getRemoteAddr());

			sendUnauthorized(response, "Invalid X-Internal-Api-Key");

			return;
		}

		filterChain.doFilter(request, response);
	}

	private boolean isValidApiKey(String providedKey) {

		byte[] expectedBytes = expectedApiKey.getBytes(StandardCharsets.UTF_8);
		byte[] providedBytes = providedKey.getBytes(StandardCharsets.UTF_8);

		return MessageDigest.isEqual(expectedBytes, providedBytes);
	}

	private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {

		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		objectMapper.writeValue(response.getWriter(),
				Map.of("status", HttpStatus.UNAUTHORIZED.value(), "error", "Unauthorized", "message", message));
	}
}