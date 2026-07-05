package com.harinitech.notification_service.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.harinitech.notification_service.dto.NotificationDto;
import com.harinitech.notification_service.dto.NotificationResponse;
import com.harinitech.notification_service.dto.NotificationStatisticsDto;
import com.harinitech.notification_service.dto.SendNotificationRequest;

public interface NotificationService {

	NotificationResponse sendNotification(SendNotificationRequest request);

	NotificationDto getNotification(Long id);

	Page<NotificationDto> getNotifications(Pageable pageable);

	Page<NotificationDto> getFailedNotifications(Pageable pageable);

	NotificationStatisticsDto getStatistics();
}