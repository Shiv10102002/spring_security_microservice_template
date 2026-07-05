package com.shiv.notification_service.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shiv.notification_service.entity.Notification;
import com.shiv.notification_service.entity.NotificationStatus;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

	Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);

	long countByStatus(NotificationStatus status);
}