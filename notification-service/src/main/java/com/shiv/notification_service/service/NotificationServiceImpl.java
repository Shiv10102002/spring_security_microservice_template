package com.shiv.notification_service.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.shiv.notification_service.dto.NotificationDto;
import com.shiv.notification_service.dto.NotificationResponse;
import com.shiv.notification_service.dto.NotificationStatisticsDto;
import com.shiv.notification_service.dto.SendNotificationRequest;
import com.shiv.notification_service.entity.Notification;
import com.shiv.notification_service.entity.NotificationProvider;
import com.shiv.notification_service.entity.NotificationStatus;
import com.shiv.notification_service.mapper.NotificationMapper;
import com.shiv.notification_service.repo.NotificationRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

	private final NotificationRepository repository;

	private final NotificationMapper mapper;

	private final EmailProvider emailProvider;

	private final NotificationTemplateService templateService;

	private static final String NOTIFICATION_NOT_FOUND = "Notification not found with id: ";

	@Override
	@Transactional(rollbackOn = Exception.class)
	public NotificationResponse sendNotification(SendNotificationRequest request) {

		log.info("Processing notification | recipient={} | type={}", request.getRecipient(), request.getType());

		String subject = templateService.buildSubject(request);

		String body = templateService.buildBody(request);

		Notification notification = Notification.builder().recipient(request.getRecipient()).subject(subject).body(body)
				.type(request.getType()).status(NotificationStatus.PENDING).provider(NotificationProvider.SENDGRID)
				.build();

		notification = repository.save(notification);

		try {

			emailProvider.sendEmail(request.getRecipient(), subject, body);

			notification.setStatus(NotificationStatus.SENT);
			notification.setSentAt(LocalDateTime.now());

			log.info("Notification sent successfully | notificationId={}", notification.getId());

		} catch (Exception ex) {

			log.error("Notification failed | notificationId={} | error={}", notification.getId(), ex.getMessage(), ex);

			notification.setStatus(NotificationStatus.FAILED);
			notification.setErrorMessage(ex.getMessage());
		}

		notification = repository.save(notification);

		return NotificationResponse.builder().notificationId(notification.getId())
				.status(notification.getStatus().name()).message(buildResponseMessage(notification)).build();
	}

	@Override
	public NotificationDto getNotification(Long id) {

		Notification notification = repository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException(NOTIFICATION_NOT_FOUND + id));

		return mapper.toDto(notification);
	}

	@Override
	public Page<NotificationDto> getNotifications(Pageable pageable) {

		return mapper.toDtoPage(repository.findAll(pageable));
	}

	@Override
	public Page<NotificationDto> getFailedNotifications(Pageable pageable) {

		return mapper.toDtoPage(repository.findByStatus(NotificationStatus.FAILED, pageable));
	}

	@Override
	public NotificationStatisticsDto getStatistics() {

		log.debug("Notification statistics requested");

		return NotificationStatisticsDto.builder().totalNotifications(repository.count())
				.sentNotifications(repository.countByStatus(NotificationStatus.SENT))
				.failedNotifications(repository.countByStatus(NotificationStatus.FAILED))
				.pendingNotifications(repository.countByStatus(NotificationStatus.PENDING)).build();
	}

	private String buildResponseMessage(Notification notification) {

		return switch (notification.getStatus()) {

		case SENT -> "Notification sent successfully";

		case FAILED -> "Notification delivery failed";

		case PENDING -> "Notification is pending delivery";
		};
	}
}