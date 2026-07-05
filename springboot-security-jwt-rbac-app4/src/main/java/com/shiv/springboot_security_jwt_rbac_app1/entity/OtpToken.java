package com.shiv.springboot_security_jwt_rbac_app1.entity;

import java.time.Instant;

import com.shiv.springboot_security_jwt_rbac_app1.model.OtpPurpose;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "otp_tokens", indexes = {
        @Index(name = "idx_otp_username", columnList = "username")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpToken extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ======================== 👤 USER ========================

    @Column(nullable = false)
    private String username;

    // ======================== 🔐 OTP ========================

    /**
     * Store hashed OTP (never plain text)
     */
    @Column(nullable = false)
    private String otpHash;

    private Instant expiryTime;

    private boolean used = false;

    // ======================== 🛡️ SECURITY ========================

    private int attempts = 0;

    private Instant lastAttemptAt;

    // ======================== 🎯 PURPOSE ========================

    @Enumerated(EnumType.STRING)
    private OtpPurpose purpose; // REGISTER, RESET_PASSWORD

    // ======================== 🧠 HELPER ========================

    @Transient
    public boolean isExpired() {
        return expiryTime != null && expiryTime.isBefore(Instant.now());
    }
}