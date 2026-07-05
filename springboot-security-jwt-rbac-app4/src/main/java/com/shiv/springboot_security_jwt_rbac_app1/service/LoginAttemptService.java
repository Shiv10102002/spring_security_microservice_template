package com.shiv.springboot_security_jwt_rbac_app1.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.shiv.springboot_security_jwt_rbac_app1.entity.User;
import com.shiv.springboot_security_jwt_rbac_app1.model.AuditAction;
import com.shiv.springboot_security_jwt_rbac_app1.model.AuditStatus;
import com.shiv.springboot_security_jwt_rbac_app1.repo.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LoginAttemptService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AuditService auditService;

	@Autowired
	private RedisLoginAttemptService redisLoginAttemptService;

	@Value("${security.account.max-login-attempts}")
	private int maxLoginAttempts;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordFailedAttempt(User user, String username) {

		long attempts = redisLoginAttemptService.increment(username);

		boolean accountLocked = user.isAccountLocked();

		if (!accountLocked && attempts >= maxLoginAttempts) {

			user.setAccountLocked(true);
			user.setLockTime(Instant.now());

			accountLocked = true;

			userRepository.saveAndFlush(user);

			log.error("ACCOUNT LOCKED | userId={} | username={} | attempts={}", user.getId(), username, attempts);

			auditService.log(AuditAction.ACCOUNT_LOCKED, AuditStatus.BLOCKED,
					"Account locked due to multiple failed login attempts", null);
		}

		log.warn("LOGIN FAILED | userId={} | username={} | attempts={} | locked={}", user.getId(), username, attempts,
				accountLocked);

		auditService.log(AuditAction.LOGIN, AuditStatus.FAILED, "Invalid username or password", null);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void resetFailedAttempts(User user, String username) {

		redisLoginAttemptService.reset(username);

		if (user.isAccountLocked()) {

			user.setAccountLocked(false);
			user.setLockTime(null);

			userRepository.saveAndFlush(user);
		}

		log.info("FAILED LOGIN ATTEMPTS RESET | userId={} | username={}", user.getId(), username);
	}
}