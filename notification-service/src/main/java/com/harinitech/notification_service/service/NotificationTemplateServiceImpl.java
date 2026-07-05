package com.harinitech.notification_service.service;

import org.springframework.stereotype.Service;

import com.harinitech.notification_service.dto.SendNotificationRequest;

@Service
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

	@Override
	public String buildSubject(SendNotificationRequest request) {

		return switch (request.getType()) {

		case REGISTRATION_OTP -> "Registration OTP Verification";

		case PASSWORD_RESET_OTP -> "Password Reset OTP";

		case EMPLOYEE_REGISTRATION_SUBMITTED -> "Employee Registration Submitted";

		case EMPLOYEE_REGISTRATION_PENDING_APPROVAL -> "New Employee Registration Pending Your Approval";

		case REGISTRATION_APPROVED -> "Registration Approved";

		case REGISTRATION_REJECTED -> "Registration Rejected";

		case PASSWORD_CHANGED -> "Password Changed Successfully";

		case PASSWORD_RESET_COMPLETED -> "Password Reset Completed";

		case ROLE_CHANGED -> "Role Assignment Updated";

		case STATUS_CHANGED -> "Account Status Updated";

		case ACCESS_ENABLED -> "Account Access Enabled";

		case ACCESS_DISABLED -> "Account Access Disabled";

		case ACCOUNT_LOCKED -> "Account Locked";

		case ACCOUNT_UNLOCKED -> "Account Unlocked";

		case FORCE_LOGOUT -> "Session Terminated";

		case SYSTEM_ALERT -> "System Alert";
		};

	}

	/**
	 * Null-safe field accessor — returns fallback if value is null or blank.
	 */
	private String safe(String value, String fallback) {
		return (value != null && !value.isBlank()) ? value : fallback;
	}

	@Override
	public String buildBody(SendNotificationRequest request) {

		return switch (request.getType()) {

		// ─── OTP notifications ──────────────────────────────────────────────

		case REGISTRATION_OTP -> """
				Dear User,

				Your registration OTP is: %s

				This OTP will expire in 5 minutes.

				If you did not request this, please ignore this message.

				Regards,
				Security Team
				""".formatted(safe(request.getOtp(), "N/A"));

		case PASSWORD_RESET_OTP -> """
				Dear User,

				Your password reset OTP is: %s

				This OTP will expire in 5 minutes.

				If you did not request a password reset, please contact support immediately.

				Regards,
				Security Team
				""".formatted(safe(request.getOtp(), "N/A"));

		// ─── Registration flow ───────────────────────────────────────────────

		case EMPLOYEE_REGISTRATION_SUBMITTED -> """
				Dear User,

				Your employee registration has been submitted successfully.

				Your account is currently under review. You will be notified once
				an administrator approves your registration.

				Regards,
				Administration Team
				""";

		case EMPLOYEE_REGISTRATION_PENDING_APPROVAL -> """
				Dear Administrator,

				A new employee registration is pending your approval.

				Employee Email  : %s
				Requested Role  : %s

				Please review and approve or reject the registration from the admin panel.

				Regards,
				Notification Service
				""".formatted(safe(request.getEmployeeEmail(), "Unknown"),
				safe(request.getRequestedRole(), "Not specified"));

		case REGISTRATION_APPROVED -> """
				Dear User,

				Your registration request has been approved.

				Assigned Role: %s

				You may now log in to the system.

				Regards,
				Administration Team
				""".formatted(safe(request.getRole(), "User"));

		case REGISTRATION_REJECTED -> """
				Dear User,

				Your registration request has been rejected.

				Reason:
				%s

				If you believe this is incorrect, please contact support.

				Regards,
				Administration Team
				""".formatted(safe(request.getReason(), "No reason provided"));

		// ─── Password notifications ──────────────────────────────────────────

		case PASSWORD_CHANGED -> """
				Dear User,

				Your password has been changed successfully.

				If you did not make this change, please contact support immediately
				and secure your account.

				Regards,
				Security Team
				""";

		case PASSWORD_RESET_COMPLETED -> """
				Dear User,

				Your password has been reset successfully.

				You can now log in with your new password.

				If you did not request this password reset, please contact support immediately.

				Regards,
				Security Team
				""";

		// ─── Admin action notifications ──────────────────────────────────────

		case ROLE_CHANGED -> """
				Dear User,

				Your role has been updated by an administrator.

				New Role: %s

				Your active sessions have been terminated. Please log in again to continue.

				Regards,
				Administration Team
				""".formatted(safe(request.getRole(), "Updated"));

		case STATUS_CHANGED -> """
				Dear User,

				Your account status has been updated by an administrator.

				New Status: %s

				If you have questions, please contact support.

				Regards,
				Administration Team
				""".formatted(safe(request.getStatus(), "Updated"));

		case ACCESS_ENABLED -> """
				Dear User,

				Access has been enabled for your account.

				You may now log in to the system.

				Regards,
				Administration Team
				""";

		case ACCESS_DISABLED -> """
				Dear User,

				Access has been disabled for your account.

				Reason:
				%s

				Please contact your administrator for assistance.

				Regards,
				Administration Team
				""".formatted(safe(request.getReason(), "No reason provided"));

		case ACCOUNT_LOCKED -> """
				Dear User,

				Your account has been locked by an administrator.

				All active sessions have been terminated.

				Please contact your administrator for assistance.

				Regards,
				Security Team
				""";

		case ACCOUNT_UNLOCKED -> """
				Dear User,

				Your account has been unlocked.

				You may now log in to the system.

				Regards,
				Security Team
				""";

		case FORCE_LOGOUT -> """
				Dear User,

				Your account session has been terminated by an administrator.

				All active sessions have been invalidated.

				Please log in again when ready.

				Regards,
				Security Team
				""";

		// ─── System notifications ────────────────────────────────────────────

		case SYSTEM_ALERT -> """
				System Alert

				This is an automated system notification.

				Please review your account or contact your administrator for details.

				Regards,
				Notification Service
				""";
		};

	}
}