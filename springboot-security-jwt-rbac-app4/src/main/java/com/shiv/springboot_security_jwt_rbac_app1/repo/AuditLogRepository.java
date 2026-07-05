package com.shiv.springboot_security_jwt_rbac_app1.repo;

import java.time.Instant;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.shiv.springboot_security_jwt_rbac_app1.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	// ======================== 👤 all logs from app ========================

	Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

	// ======================== 👤 USER HISTORY ========================

	Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	// ======================== 🚨 ACTION + STATUS ========================

	Page<AuditLog> findByActionAndStatusOrderByCreatedAtDesc(String action, String status, Pageable pageable);

	// ======================== ⚠️ ACTION LIST ========================

	Page<AuditLog> findByActionInOrderByCreatedAtDesc(List<String> actions, Pageable pageable);

	// ======================== 📊 COUNTS ========================

	long countByAction(String action);

	long countByActionAndStatus(String action, String status);

	// ======================== 🔍 FILTER SEARCH ========================

	@Query("""
			    SELECT a FROM AuditLog a
			    WHERE
			        (:action IS NULL OR a.action = :action)
			    AND (:status IS NULL OR a.status = :status)
			    AND (:username IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%')))
			    AND (:role IS NULL OR a.role = :role)
			    AND (:ipAddress IS NULL OR a.ipAddress = :ipAddress)
			    AND (:fromDate IS NULL OR a.createdAt >= :fromDate)
			    AND (:toDate IS NULL OR a.createdAt <= :toDate)
			    ORDER BY a.createdAt DESC
			""")
	Page<AuditLog> searchAuditLogs(String action, String status, String username, String role, String ipAddress,
			Instant fromDate, Instant toDate, Pageable pageable);
}