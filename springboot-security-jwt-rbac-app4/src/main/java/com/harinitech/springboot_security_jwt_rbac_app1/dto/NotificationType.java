package com.harinitech.springboot_security_jwt_rbac_app1.dto;

/**
 * ✅ PRODUCTION NotificationType (security-app side)
 *
 * Must stay in EXACT sync with notification-service's
 * com.harinitech.notification_service.entity.NotificationType.
 *
 * Adding a value here requires adding the same value there, AND adding a
 * subject + body template in NotificationTemplateServiceImpl.
 */
public enum NotificationType {

	REGISTRATION_OTP,

	PASSWORD_RESET_OTP,

	EMPLOYEE_REGISTRATION_SUBMITTED,

	EMPLOYEE_REGISTRATION_PENDING_APPROVAL,

	REGISTRATION_APPROVED,

	REGISTRATION_REJECTED,

	PASSWORD_CHANGED,

	PASSWORD_RESET_COMPLETED,

	ROLE_CHANGED,

	STATUS_CHANGED,

	ACCESS_ENABLED,

	ACCESS_DISABLED,

	ACCOUNT_LOCKED,

	ACCOUNT_UNLOCKED,

	FORCE_LOGOUT,

	SYSTEM_ALERT
}