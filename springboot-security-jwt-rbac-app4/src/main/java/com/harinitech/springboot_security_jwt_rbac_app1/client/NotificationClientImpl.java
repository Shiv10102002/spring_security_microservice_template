package com.harinitech.springboot_security_jwt_rbac_app1.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.harinitech.springboot_security_jwt_rbac_app1.dto.SendNotificationRequest;
import com.harinitech.springboot_security_jwt_rbac_app1.exceptions.NotificationServiceException;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebClient-based HTTP client for notification-service.
 *
 * Sends POST to {notification.service.url}/api/v1/notifications
 * with X-Internal-Api-Key header for service-to-service auth.
 *
 * Do NOT call directly from services — use NotificationFacade.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationClientImpl implements NotificationClient {

	private static final int MIN_KEY_LENGTH = 32;

	private final WebClient.Builder webClientBuilder;

	@Value("${notification.service.url}")
	private String notificationServiceUrl;

	@Value("${notification.service.internal-api-key}")
	private String internalApiKey;

	@PostConstruct
	void validateConfiguration() {

		if (!StringUtils.hasText(notificationServiceUrl)) {
			throw new IllegalStateException(
					"Notification service URL is not configured. Set NOTIFICATION_SERVICE_URL or notification.service.url.");
		}

		if (!StringUtils.hasText(internalApiKey)) {
			throw new IllegalStateException(
					"Internal API key is not configured. Set INTERNAL_API_KEY environment variable "
							+ "or notification.service.internal-api-key in application-secret.properties.");
		}

		if (internalApiKey.trim().length() < MIN_KEY_LENGTH) {
			throw new IllegalStateException(
					"Internal API key is too short. Use at least " + MIN_KEY_LENGTH + " characters.");
		}

		log.info("Notification client initialized | serviceUrl={}", notificationServiceUrl);
	}

	@Override
	public void sendNotification(SendNotificationRequest request) {

		log.info("Calling Notification Service | recipient={} | type={}", request.getRecipient(),
				request.getType());

		try {

			webClientBuilder.build().post().uri(notificationServiceUrl + "/api/v1/notifications")
					.contentType(MediaType.APPLICATION_JSON).header("X-Internal-Api-Key", internalApiKey)
					.bodyValue(request).retrieve().onStatus(HttpStatusCode::isError,
							response -> response.bodyToMono(String.class).map(
									error -> new NotificationServiceException("Notification service returned error: "
											+ error)))
					.toBodilessEntity().block();

			log.info("Notification Service call successful | recipient={} | type={}", request.getRecipient(),
					request.getType());

		} catch (NotificationServiceException ex) {

			throw ex;

		} catch (Exception ex) {

			log.error("Notification Service unreachable | recipient={} | type={} | error={}",
					request.getRecipient(), request.getType(), ex.getMessage(), ex);

			throw new NotificationServiceException("Failed to communicate with notification service", ex);
		}
	}
}