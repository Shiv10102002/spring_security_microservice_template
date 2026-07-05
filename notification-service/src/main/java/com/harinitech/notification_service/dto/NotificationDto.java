package com.harinitech.notification_service.dto;

import java.time.LocalDateTime;

import com.harinitech.notification_service.entity.NotificationProvider;
import com.harinitech.notification_service.entity.NotificationStatus;
import com.harinitech.notification_service.entity.NotificationType;

import lombok.Builder;

@Builder
public record NotificationDto(

		Long id,

		String recipient,

		String subject,

		NotificationType type,

		NotificationStatus status,

		NotificationProvider provider,

		String errorMessage,

		LocalDateTime sentAt,

		LocalDateTime createdDate) {
}