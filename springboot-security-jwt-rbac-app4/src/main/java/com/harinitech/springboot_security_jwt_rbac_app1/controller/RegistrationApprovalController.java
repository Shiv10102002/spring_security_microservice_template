package com.harinitech.springboot_security_jwt_rbac_app1.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.harinitech.springboot_security_jwt_rbac_app1.model.ApiResponse;
import com.harinitech.springboot_security_jwt_rbac_app1.service.IAdminService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/approvals/registrations")
public class RegistrationApprovalController {

	private final IAdminService adminService;

	@GetMapping("/pending")
	@PreAuthorize("hasAuthority('VIEW_PENDING_REGISTRATIONS')")
	public ResponseEntity<ApiResponse<?>> getPendingRegistrations(Pageable pageable) {
		log.info("APPROVAL API | FETCH PENDING REGISTRATIONS | page={} | size={} | sort={}", pageable.getPageNumber(),
				pageable.getPageSize(), pageable.getSort());

		return ResponseEntity.ok(ApiResponse.success("Pending registrations fetched",
				adminService.getPendingRegistrations(pageable).getBody()));
	}

	@PostMapping("/{id}/approve")
	@PreAuthorize("hasAuthority('APPROVE_REGISTRATION')")
	public ResponseEntity<ApiResponse<?>> approveRegistration(@PathVariable Long id) {
		log.warn("APPROVAL API | APPROVE REGISTRATION | userId={}", id);

		return ResponseEntity
				.ok(ApiResponse.success("Registration approved", adminService.approveRegistration(id).getBody()));
	}

	@PostMapping("/{id}/reject")
	@PreAuthorize("hasAuthority('REJECT_REGISTRATION')")
	public ResponseEntity<ApiResponse<?>> rejectRegistration(@PathVariable Long id,
			@RequestParam(defaultValue = "Rejected by approver") String reason) {
		log.warn("APPROVAL API | REJECT REGISTRATION | userId={} | reason={}", id, reason);

		return ResponseEntity.ok(
				ApiResponse.success("Registration rejected", adminService.rejectRegistration(id, reason).getBody()));
	}
}
