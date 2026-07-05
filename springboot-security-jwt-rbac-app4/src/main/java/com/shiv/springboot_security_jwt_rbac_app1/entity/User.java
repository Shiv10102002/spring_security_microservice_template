package com.shiv.springboot_security_jwt_rbac_app1.entity;

import java.time.Instant;

import com.shiv.springboot_security_jwt_rbac_app1.model.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", indexes = { @Index(name = "idx_user_username", columnList = "username"),
		@Index(name = "idx_user_status", columnList = "status") })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// ======================== 🔐 AUTH ========================

	@Column(unique = true, nullable = false, length = 100)
	@NotBlank(message = "Email is required")
	@Email(message = "Invalid email format")
	private String username;

	@Column(nullable = false)
	@NotBlank(message = "Password is required")
	@Size(min = 8, max = 100)
	private String password;

	// ======================== 🎭 ROLE ========================

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "role_id", nullable = false)
	private Role role;

	// ======================== 📊 STATUS ========================

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Status status = Status.ACTIVE;

	/**
	 * The role the user requested during employee registration. Set only when
	 * status = PENDING_APPROVAL, cleared after approval/rejection.
	 */
	@Column(name = "requested_role", length = 50)
	private String requestedRole;

	// ======================== 🔒 ACCOUNT SECURITY ========================

	/**
	 * Account manually disabled by admin
	 */
	@Column(nullable = false)
	private boolean enabled = true;

	/**
	 * Lock mechanism (brute-force protection)
	 */
	@Column(nullable = false)
	private boolean accountLocked = false;

	/**
	 * Number of failed login attempts
	 */
	@Column(nullable = false)
	private int failedLoginAttempts = 0;

	/**
	 * When account was locked
	 */
	private Instant lockTime;

	// ======================== 📱 LOGIN TRACKING ========================

	/**
	 * Last successful login timestamp
	 */
	private Instant lastLoginAt;

	/**
	 * Last login IP address
	 */
	@Column(length = 45) // supports IPv6
	private String lastLoginIp;

	/**
	 * Last login device info (browser/device)
	 */
	@Column(length = 255)
	private String lastLoginDevice;

	// ======================== 🔁 PASSWORD MANAGEMENT ========================

	/**
	 * Force user to change password on next login
	 */
	private boolean forcePasswordChange = false;

	/**
	 * Last password update time
	 */
	private Instant passwordChangedAt;

	// ======================== 🧠 HELPERS ========================

	/**
	 * Convenience method (not stored in DB)
	 */
	@Transient
	public boolean isAccountNonLocked() {
		return !accountLocked;
	}
}