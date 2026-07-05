package com.shiv.notification_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {

	private Long notificationId;

	private String status;

	private String message;
}