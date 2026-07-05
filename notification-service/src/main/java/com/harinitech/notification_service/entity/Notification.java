package com.harinitech.notification_service.entity;

import java.time.LocalDateTime;

import com.harinitech.notification_service.audit.Auditable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications", indexes = { @Index(name = "idx_notification_status", columnList = "status"),
		@Index(name = "idx_notification_type", columnList = "type"),
		@Index(name = "idx_notification_recipient", columnList = "recipient") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String recipient;

	@Column(nullable = false, length = 500)
	private String subject;

	@Column(nullable = false, columnDefinition = "LONGTEXT")
	private String body;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private NotificationType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private NotificationStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private NotificationProvider provider;

	@Column(columnDefinition = "TEXT")
	private String errorMessage;

	private LocalDateTime sentAt;
}