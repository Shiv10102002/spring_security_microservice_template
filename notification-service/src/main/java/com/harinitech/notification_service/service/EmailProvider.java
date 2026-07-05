package com.harinitech.notification_service.service;

public interface EmailProvider {

	void sendEmail(String recipient, String subject, String body);

}