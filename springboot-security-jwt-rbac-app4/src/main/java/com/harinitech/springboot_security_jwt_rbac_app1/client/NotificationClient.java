package com.harinitech.springboot_security_jwt_rbac_app1.client;

import com.harinitech.springboot_security_jwt_rbac_app1.dto.SendNotificationRequest;

public interface NotificationClient {

	void sendNotification(SendNotificationRequest request);
}