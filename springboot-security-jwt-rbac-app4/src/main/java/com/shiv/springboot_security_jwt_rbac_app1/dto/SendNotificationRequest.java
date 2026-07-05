package com.shiv.springboot_security_jwt_rbac_app1.dto;

import lombok.Builder;
import lombok.Data;

/**
 * ✅ PRODUCTION SendNotificationRequest (security-app side)
 *
 * This DTO is serialized to JSON and sent to notification-service.
 * Field names MUST match notification-service's SendNotificationRequest fields exactly.
 *
 * Mapping:
 *   recipient       → email of the person receiving the notification
 *   type            → NotificationType enum (serialized as String)
 *   otp             → filled for REGISTRATION_OTP and PASSWORD_RESET_OTP
 *   role            → filled for ROLE_CHANGED, REGISTRATION_APPROVED
 *   status          → filled for STATUS_CHANGED
 *   reason          → filled for REGISTRATION_REJECTED, ACCESS_DISABLED
 *   requestedRole   → filled for EMPLOYEE_REGISTRATION_PENDING_APPROVAL
 *   employeeEmail   → filled for EMPLOYEE_REGISTRATION_PENDING_APPROVAL (admin alert)
 */
@Data
@Builder
public class SendNotificationRequest {

    private String recipient;

    private NotificationType type;

    private String otp;

    private String role;

    private String status;

    private String reason;

    private String requestedRole;

    private String employeeEmail;
}
