package com.harinitech.springboot_security_jwt_rbac_app1.client;

import com.harinitech.springboot_security_jwt_rbac_app1.dto.NotificationType;

/**
 * ✅ PRODUCTION NotificationFacade
 *
 * Single entry point for all notification calls from UserServiceImpl and AdminServiceImpl.
 * Wraps NotificationClient and builds SendNotificationRequest internally.
 *
 * Overloads:
 *  (1) recipient + type                           → simple status notifications (PASSWORD_CHANGED, FORCE_LOGOUT, etc.)
 *  (2) recipient + type + otp                     → OTP notifications (REGISTRATION_OTP, PASSWORD_RESET_OTP)
 *  (3) recipient + type + role + status + reason  → admin action notifications (REGISTRATION_APPROVED/REJECTED, ROLE_CHANGED, STATUS_CHANGED, etc.)
 *  (4) recipient + type + employeeEmail + requestedRole → admin notification for new employee registration pending approval
 */
public interface NotificationFacade {

    // ─── (1) Simple notification — no extra context needed ───────────────────
    void sendNotification(
            String recipient,
            NotificationType notificationType
    );

    // ─── (2) OTP notification ─────────────────────────────────────────────────
    void sendNotification(
            String recipient,
            NotificationType notificationType,
            String otp
    );

    // ─── (3) Admin action notification — role, status, reason ────────────────
    void sendNotification(
            String recipient,
            NotificationType notificationType,
            String role,
            String status,
            String reason
    );

    // ─── (4) Admin alert: new employee registration pending approval ──────────
    void sendPendingApprovalAlertToAdmin(
            String adminEmail,
            String employeeEmail,
            String requestedRole
    );
}
