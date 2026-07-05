package com.shiv.springboot_security_jwt_rbac_app1.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

	// ======================== ⏱ TIMESTAMPS ========================

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private Instant updatedAt;

	// ======================== 👤 USER TRACKING ========================

	/**
	 * Stores userId (NOT username) Matches your JWT design (principal = userId)
	 */
	@CreatedBy
	@Column(updatable = false)
	private Long createdBy;

	@LastModifiedBy
	private Long updatedBy;

	// ======================== 🌐 OPTIONAL (ADVANCED) ========================

	/**
	 * Optional: track source IP (set manually if needed)
	 */
	private String createdIp;

	private String updatedIp;
}