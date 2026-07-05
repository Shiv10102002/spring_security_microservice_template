package com.shiv.notification_service.service;

import com.shiv.notification_service.dto.SendNotificationRequest;

public interface NotificationTemplateService {

	String buildSubject(SendNotificationRequest request);

	String buildBody(SendNotificationRequest request);
}