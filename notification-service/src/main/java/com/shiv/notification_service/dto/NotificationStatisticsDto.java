package com.shiv.notification_service.dto;

import lombok.Builder;

@Builder
public record NotificationStatisticsDto(

		long totalNotifications,

		long sentNotifications,

		long failedNotifications,

		long pendingNotifications) {
}