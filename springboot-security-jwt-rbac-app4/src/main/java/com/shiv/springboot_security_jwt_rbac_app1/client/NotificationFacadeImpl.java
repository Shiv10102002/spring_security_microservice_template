package com.shiv.springboot_security_jwt_rbac_app1.client;

import org.springframework.stereotype.Service;

import com.shiv.springboot_security_jwt_rbac_app1.dto.NotificationType;
import com.shiv.springboot_security_jwt_rbac_app1.dto.SendNotificationRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ✅ PRODUCTION NotificationFacadeImpl
 *
 * Builds SendNotificationRequest for every notification scenario and
 * delegates to NotificationClientImpl (WebClient HTTP call).
 *
 * ALL notification calls are fire-and-tolerate: a failure in the
 * notification service NEVER fails the main business transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationFacadeImpl implements NotificationFacade {

    private final NotificationClient notificationClient;

    // ─── (1) Simple notification ──────────────────────────────────────────────

    @Override
    public void sendNotification(String recipient, NotificationType notificationType) {

        SendNotificationRequest request = SendNotificationRequest.builder()
                .recipient(recipient)
                .type(notificationType)
                .build();

        send(request);
    }

    // ─── (2) OTP notification ─────────────────────────────────────────────────

    @Override
    public void sendNotification(String recipient, NotificationType notificationType, String otp) {

        SendNotificationRequest request = SendNotificationRequest.builder()
                .recipient(recipient)
                .type(notificationType)
                .otp(otp)
                .build();

        send(request);
    }

    // ─── (3) Admin action notification ────────────────────────────────────────

    @Override
    public void sendNotification(String recipient, NotificationType notificationType,
            String role, String status, String reason) {

        SendNotificationRequest request = SendNotificationRequest.builder()
                .recipient(recipient)
                .type(notificationType)
                .role(role)
                .status(status)
                .reason(reason)
                .build();

        send(request);
    }

    // ─── (4) Admin alert: new employee registration pending approval ──────────

    @Override
    public void sendPendingApprovalAlertToAdmin(String adminEmail, String employeeEmail, String requestedRole) {

        SendNotificationRequest request = SendNotificationRequest.builder()
                .recipient(adminEmail)
                .type(NotificationType.EMPLOYEE_REGISTRATION_PENDING_APPROVAL)
                .employeeEmail(employeeEmail)
                .requestedRole(requestedRole)
                .build();

        send(request);
    }

    // ─── Internal fire-and-tolerate dispatcher ────────────────────────────────

    private void send(SendNotificationRequest request) {

        try {

            notificationClient.sendNotification(request);

        } catch (Exception ex) {

            // ✅ INTENTIONAL: Notification failure must NEVER roll back the
            //    main business transaction (registration, approval, OTP send, etc.)
            log.error(
                "NOTIFICATION SEND FAILED | recipient={} | type={} | error={}",
                request.getRecipient(),
                request.getType(),
                ex.getMessage(),
                ex
            );
        }
    }
}
