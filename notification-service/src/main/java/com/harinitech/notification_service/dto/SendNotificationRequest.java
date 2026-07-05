package com.harinitech.notification_service.dto;

import com.harinitech.notification_service.entity.NotificationType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendNotificationRequest {

    @Email
    @NotBlank
    private String recipient;

    @NotNull
    private NotificationType type;

    private String otp;

    private String role;

    private String requestedRole;

    private String employeeEmail;

    private String reason;

    private String status;
}