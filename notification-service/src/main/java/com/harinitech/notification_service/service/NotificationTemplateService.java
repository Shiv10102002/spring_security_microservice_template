package com.harinitech.notification_service.service;

import com.harinitech.notification_service.dto.SendNotificationRequest;

public interface NotificationTemplateService {

	String buildSubject(SendNotificationRequest request);

	String buildBody(SendNotificationRequest request);
}