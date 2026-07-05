package com.harinitech.springboot_security_jwt_rbac_app1.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "audit_logs",
    indexes = {

        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_status", columnList = "status"),
        @Index(name = "idx_audit_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ======================== 👤 USER ========================

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 150)
    private String username;

    @Column(name = "role", length = 50)
    private String role;

    // ======================== 🔐 ACTION ========================

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    // ======================== 🌐 REQUEST ========================

    @Column(name = "endpoint", length = 500)
    private String endpoint;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "device_info", length = 500)
    private String deviceInfo;

    // ======================== ⏱️ AUDIT TIME ========================

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }
}