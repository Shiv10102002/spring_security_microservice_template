package com.shiv.springboot_security_jwt_rbac_app1.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;

@Entity
@Data
@Table(name = "user_tokens")
public class UserToken extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// ======================== 👤 USER ========================

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	// ======================== 🆔 TOKEN IDENTIFIER ========================

	/**
	 * Unique token identifier (jti) Used for tracking and revocation
	 */
	@Column(nullable = false, unique = true, updatable = false)
	private String tokenId = UUID.randomUUID().toString();

	// ======================== 🔐 ACCESS TOKEN ========================

	@Column(length = 1000)
	private String accessToken;

	private Instant accessIssuedAt;
	private Instant accessExpiry;

	// ======================== 🔄 REFRESH TOKEN ========================

	@Column(length = 1000, nullable = false, unique = true)
	private String refreshToken;

	private Instant refreshIssuedAt;
	private Instant refreshExpiry;

	// ======================== 🔒 SECURITY FLAGS ========================

	private boolean revoked = false;
	private boolean expired = false;

	/**
	 * Detect reuse of refresh tokens (advanced security)
	 */
	private boolean refreshUsed = false;

	// ======================== 📱 DEVICE INFO ========================

	@Column(length = 255)
	private String deviceInfo;

	@Column(length = 45)
	private String ipAddress;

	// ======================== 🧠 HELPER ========================

	@Transient
	public boolean isActive() {
		return !revoked && !expired;
	}
}