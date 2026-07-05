package com.shiv.springboot_security_jwt_rbac_app1.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shiv.springboot_security_jwt_rbac_app1.entity.AuditLog;
import com.shiv.springboot_security_jwt_rbac_app1.model.ApiResponse;
import com.shiv.springboot_security_jwt_rbac_app1.service.AuditQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/audit")
public class AuditController {

	private final AuditQueryService auditQueryService;

	// ======================== 📋 AUDIT LOGS ========================
	@PreAuthorize("hasAuthority('VIEW_AUDIT_LOGS')")
	@GetMapping("/logs")
	public ResponseEntity<ApiResponse<?>> getAuditLogs(

			@PageableDefault(page = 0, size = 20, sort = "createdAt") Pageable pageable) {

		log.info("AUDIT API | Fetch all audit logs");

		Page<AuditLog> response = auditQueryService.getAllAuditLogs(pageable);

		return ResponseEntity.ok(ApiResponse.success("Audit logs fetched successfully", response));
	}

	// ======================== 🔍 AUDIT LOG BY ID ========================

	@PreAuthorize("hasAuthority('VIEW_AUDIT_LOGS')")
	@GetMapping("/logs/{id}")
	public ResponseEntity<ApiResponse<?>> getAuditLogById(@PathVariable Long id) {

		log.info("AUDIT API | Fetch audit log | id={}", id);

		return ResponseEntity
				.ok(ApiResponse.success("Audit log fetched successfully", auditQueryService.getAuditLogById(id)));
	}

	// ======================== 👤 USER AUDIT HISTORY ========================

	@PreAuthorize("hasAuthority('VIEW_AUDIT_LOGS')")
	@GetMapping("/users/{userId}")
	public ResponseEntity<ApiResponse<?>> getUserAuditHistory(@PathVariable Long userId, Pageable pageable) {

		log.info("AUDIT API | Fetch user audit history | userId={}", userId);

		return ResponseEntity.ok(ApiResponse.success("User audit history fetched successfully",
				auditQueryService.getUserAuditHistory(userId, pageable)));
	}

	// ======================== 🚨 FAILED LOGIN EVENTS ========================

	@PreAuthorize("hasAuthority('VIEW_SECURITY_EVENTS')")
	@GetMapping("/security/failed-logins")
	public ResponseEntity<ApiResponse<?>> getFailedLoginAttempts(Pageable pageable) {

		log.info("AUDIT API | Fetch failed login attempts");

		return ResponseEntity.ok(ApiResponse.success("Failed login attempts fetched successfully",
				auditQueryService.getFailedLoginAttempts(pageable)));
	}

	// ======================== ⚠️ SUSPICIOUS ACTIVITIES ========================

	@PreAuthorize("hasAuthority('VIEW_SECURITY_EVENTS')")
	@GetMapping("/security/suspicious")
	public ResponseEntity<ApiResponse<?>> getSuspiciousActivities(Pageable pageable) {

		log.info("AUDIT API | Fetch suspicious activities");

		return ResponseEntity.ok(ApiResponse.success("Suspicious activities fetched successfully",
				auditQueryService.getSuspiciousActivities(pageable)));
	}

	// ======================== 📊 AUDIT STATISTICS ========================

	@PreAuthorize("hasAuthority('VIEW_AUDIT_DASHBOARD')")
	@GetMapping("/statistics")
	public ResponseEntity<ApiResponse<?>> getAuditStatistics() {

		log.info("AUDIT API | Fetch audit statistics");

		return ResponseEntity.ok(
				ApiResponse.success("Audit statistics fetched successfully", auditQueryService.getAuditStatistics()));
	}
}