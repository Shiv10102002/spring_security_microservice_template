package com.shiv.springboot_security_jwt_rbac_app1.service;

import java.time.Instant;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.shiv.springboot_security_jwt_rbac_app1.entity.AuditLog;
import com.shiv.springboot_security_jwt_rbac_app1.entity.User;
import com.shiv.springboot_security_jwt_rbac_app1.model.AuditAction;
import com.shiv.springboot_security_jwt_rbac_app1.model.AuditStatus;
import com.shiv.springboot_security_jwt_rbac_app1.repo.AuditLogRepository;
import com.shiv.springboot_security_jwt_rbac_app1.repo.UserRepository;
import com.shiv.springboot_security_jwt_rbac_app1.utility.RequestInfoUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

	private final AuditLogRepository auditLogRepository;
	private final UserRepository userRepository;

	/**
	 * ✅ Production-grade audit logging
	 *
	 * IMPORTANT: - Never throws exception to caller - Runs in separate transaction
	 * - Works for authenticated + public APIs - Stores security-critical metadata
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void log(AuditAction action, AuditStatus status, String details, HttpServletRequest request) {

		try {

			User currentUser = getCurrentUserSafely();

			AuditLog audit = AuditLog.builder().userId(currentUser != null ? currentUser.getId() : null)
					.username(currentUser != null ? currentUser.getUsername() : "ANONYMOUS")
					.role(currentUser != null ? currentUser.getRole().getName() : "PUBLIC")

					.action(action.name()).status(status.name()).details(details)

					.endpoint(request != null ? request.getRequestURI() : null)
					.httpMethod(request != null ? request.getMethod() : null)

					.ipAddress(request != null ? RequestInfoUtil.getClientIp(request) : null)

					.deviceInfo(request != null ? RequestInfoUtil.getDeviceInfo(request) : null)

					.createdAt(Instant.now()).build();

			auditLogRepository.save(audit);

		} catch (Exception ex) {

			/*
			 * 🔥 NEVER BREAK BUSINESS FLOW
			 *
			 * Audit failure must NEVER fail: - login - payment - password reset - admin
			 * operations
			 */
			log.error("AUDIT LOG FAILURE | action={} | reason={}", action, ex.getMessage());
		}
	}

	/**
	 * Safely resolves authenticated user. Returns null for public endpoints.
	 */
	private User getCurrentUserSafely() {

		try {

			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

			if (authentication == null || authentication.getPrincipal() == null) {
				return null;
			}

			Long userId = Long.parseLong(authentication.getPrincipal().toString());

			return userRepository.findById(userId).orElse(null);

		} catch (Exception ex) {
			return null;
		}
	}
}