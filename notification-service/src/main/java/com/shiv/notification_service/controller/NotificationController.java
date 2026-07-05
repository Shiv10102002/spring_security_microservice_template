package com.shiv.notification_service.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shiv.notification_service.dto.ApiResponse;
import com.shiv.notification_service.dto.NotificationDto;
import com.shiv.notification_service.dto.NotificationResponse;
import com.shiv.notification_service.dto.NotificationStatisticsDto;
import com.shiv.notification_service.dto.SendNotificationRequest;
import com.shiv.notification_service.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

	private final NotificationService service;

	// =========================================================================
	// SEND NOTIFICATION
	// =========================================================================

	@PostMapping
	public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
			@Valid @RequestBody SendNotificationRequest request) {

		log.info("NOTIFICATION API | SEND | recipient={} | type={}", request.getRecipient(), request.getType());

		NotificationResponse response = service.sendNotification(request);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Notification processed successfully", response));
	}

	// =========================================================================
	// GET NOTIFICATION BY ID
	// =========================================================================

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<NotificationDto>> getNotificationById(@PathVariable Long id) {

		log.info("NOTIFICATION API | GET BY ID | notificationId={}", id);

		return ResponseEntity.ok(ApiResponse.success("Notification fetched successfully", service.getNotification(id)));
	}

	// =========================================================================
	// GET NOTIFICATION HISTORY
	// =========================================================================

	@GetMapping
	public ResponseEntity<ApiResponse<Page<NotificationDto>>> getNotifications(Pageable pageable) {

		log.info("NOTIFICATION API | HISTORY | page={} | size={} | sort={}", pageable.getPageNumber(),
				pageable.getPageSize(), pageable.getSort());

		return ResponseEntity
				.ok(ApiResponse.success("Notifications fetched successfully", service.getNotifications(pageable)));
	}

	// =========================================================================
	// FAILED NOTIFICATIONS
	// =========================================================================

	@GetMapping("/failed")
	public ResponseEntity<ApiResponse<Page<NotificationDto>>> getFailedNotifications(Pageable pageable) {

		log.info("NOTIFICATION API | FAILED NOTIFICATIONS");

		return ResponseEntity.ok(ApiResponse.success("Failed notifications fetched successfully",
				service.getFailedNotifications(pageable)));
	}

	// =========================================================================
	// NOTIFICATION STATISTICS
	// =========================================================================

	@GetMapping("/statistics")
	public ResponseEntity<ApiResponse<NotificationStatisticsDto>> getStatistics() {

		log.info("NOTIFICATION API | STATISTICS");

		return ResponseEntity
				.ok(ApiResponse.success("Notification statistics fetched successfully", service.getStatistics()));
	}

	// =========================================================================
	// HEALTH CHECK
	// =========================================================================

	@GetMapping("/health")
	public ResponseEntity<ApiResponse<String>> health() {

		return ResponseEntity.ok(ApiResponse.success("Notification service is running", "UP"));
	}
}