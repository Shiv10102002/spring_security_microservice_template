package com.harinitech.notification_service.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.harinitech.notification_service.dto.NotificationDto;
import com.harinitech.notification_service.entity.Notification;

@Component
public class NotificationMapper {

	public NotificationDto toDto(Notification notification) {

		if (notification == null) {
			return null;
		}

		return NotificationDto.builder().id(notification.getId()).recipient(notification.getRecipient())
				.subject(notification.getSubject()).type(notification.getType()).status(notification.getStatus())
				.provider(notification.getProvider()).errorMessage(notification.getErrorMessage())
				.sentAt(notification.getSentAt()).createdDate(notification.getCreatedDate()).build();
	}

	public List<NotificationDto> toDtoList(List<Notification> notifications) {

		if (notifications == null || notifications.isEmpty()) {
			return Collections.emptyList();
		}

		return notifications.stream().filter(Objects::nonNull).map(this::toDto).toList();
	}

	public Page<NotificationDto> toDtoPage(Page<Notification> notifications) {

		if (notifications == null) {
			return Page.empty();
		}

		return notifications.map(this::toDto);
	}
}