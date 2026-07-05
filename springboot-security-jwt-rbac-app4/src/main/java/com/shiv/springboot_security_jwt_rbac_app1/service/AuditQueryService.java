package com.shiv.springboot_security_jwt_rbac_app1.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shiv.springboot_security_jwt_rbac_app1.entity.AuditLog;
import com.shiv.springboot_security_jwt_rbac_app1.model.AuditAction;
import com.shiv.springboot_security_jwt_rbac_app1.model.AuditStatus;
import com.shiv.springboot_security_jwt_rbac_app1.repo.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuditQueryService {

	private final AuditLogRepository auditLogRepository;

	// ======================== 📋 FILTERED AUDIT LOGS ========================

	// ======================== 📋 ALL AUDIT LOGS ========================

	public Page<AuditLog> getAllAuditLogs(Pageable pageable) {

		return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
	}

	// ======================== 🔍 SINGLE AUDIT ========================

	public AuditLog getAuditLogById(Long id) {

		return auditLogRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Audit log not found with ID: " + id));
	}

	// ======================== 👤 USER HISTORY ========================

	public Page<AuditLog> getUserAuditHistory(Long userId, Pageable pageable) {

		return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
	}

	// ======================== 🚨 FAILED LOGINS ========================

	public Page<AuditLog> getFailedLoginAttempts(Pageable pageable) {

		return auditLogRepository.findByActionAndStatusOrderByCreatedAtDesc(AuditAction.LOGIN.name(),
				AuditStatus.FAILED.name(), pageable);
	}

	// ======================== ⚠️ SUSPICIOUS EVENTS ========================

	public Page<AuditLog> getSuspiciousActivities(Pageable pageable) {

		List<String> suspiciousActions = List.of(AuditAction.TOKEN_REUSE_ATTACK.name(),
				AuditAction.ACCESS_DENIED.name(), AuditAction.REVOKED_TOKEN_USAGE.name(),
				AuditAction.EXPIRED_TOKEN_USAGE.name(), AuditAction.ACCOUNT_LOCKED.name());

		return auditLogRepository.findByActionInOrderByCreatedAtDesc(suspiciousActions, pageable);
	}

	// ======================== 📊 DASHBOARD ========================

	public Map<String, Object> getAuditStatistics() {

		long totalLogs = auditLogRepository.count();

		long successfulLogins = auditLogRepository.countByActionAndStatus(AuditAction.LOGIN.name(),
				AuditStatus.SUCCESS.name());

		long failedLogins = auditLogRepository.countByActionAndStatus(AuditAction.LOGIN.name(),
				AuditStatus.FAILED.name());

		long lockedAccounts = auditLogRepository.countByAction(AuditAction.ACCOUNT_LOCKED.name());

		long passwordResets = auditLogRepository.countByAction(AuditAction.PASSWORD_RESET.name());

		long tokenAttacks = auditLogRepository.countByAction(AuditAction.TOKEN_REUSE_ATTACK.name());

		return Map.of("totalLogs", totalLogs, "successfulLogins", successfulLogins, "failedLogins", failedLogins,
				"lockedAccounts", lockedAccounts, "passwordResets", passwordResets, "tokenAttacks", tokenAttacks);
	}
}