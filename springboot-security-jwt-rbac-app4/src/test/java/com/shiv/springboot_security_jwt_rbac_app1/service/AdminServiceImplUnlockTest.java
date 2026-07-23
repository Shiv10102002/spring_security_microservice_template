package com.shiv.springboot_security_jwt_rbac_app1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.shiv.springboot_security_jwt_rbac_app1.client.NotificationFacade;
import com.shiv.springboot_security_jwt_rbac_app1.dto.NotificationType;
import com.shiv.springboot_security_jwt_rbac_app1.entity.User;
import com.shiv.springboot_security_jwt_rbac_app1.repo.RoleRepository;
import com.shiv.springboot_security_jwt_rbac_app1.repo.UserRepository;
import com.shiv.springboot_security_jwt_rbac_app1.repo.UserTokenRepository;

/**
 * Regression tests for issue #58 — admin unlock must clear the Redis failed
 * login counter, not just the database lock fields. Otherwise the user relocks
 * on the first failed login after unlock.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceImplUnlockTest {

	private static final long ADMIN_ID = 1L;
	private static final long TARGET_USER_ID = 42L;
	private static final String TARGET_USERNAME = "locked.user@example.com";

	@Mock
	private UserRepository userRepository;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private UserTokenRepository userTokenRepository;

	@Mock
	private AuditService auditService;

	@Mock
	private NotificationFacade notificationFacade;

	@Mock
	private RedisLoginAttemptService redisLoginAttemptService;

	@InjectMocks
	private AdminServiceImpl adminService;

	@BeforeEach
	void authenticateAsAdmin() {
		TestingAuthenticationToken auth = new TestingAuthenticationToken(String.valueOf(ADMIN_ID), null,
				List.of(new SimpleGrantedAuthority("ACCOUNT_UNLOCK")));
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	private User lockedUser() {
		User user = new User();
		user.setId(TARGET_USER_ID);
		user.setUsername(TARGET_USERNAME);
		user.setAccountLocked(true);
		user.setFailedLoginAttempts(5);
		user.setLockTime(Instant.now());
		return user;
	}

	@Test
	void unlockClearsRedisCounterAndDatabaseLockFields() {
		User user = lockedUser();
		when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));

		adminService.unlockUserAccount(TARGET_USER_ID);

		verify(redisLoginAttemptService).reset(TARGET_USERNAME);
		verify(userRepository).save(user);
		assertThat(user.isAccountLocked()).isFalse();
		assertThat(user.getFailedLoginAttempts()).isZero();
		assertThat(user.getLockTime()).isNull();
	}

	@Test
	void unlockStillNotifiesUserAndAudits() {
		User user = lockedUser();
		when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));

		adminService.unlockUserAccount(TARGET_USER_ID);

		verify(notificationFacade).sendNotification(TARGET_USERNAME, NotificationType.ACCOUNT_UNLOCKED);
		verify(auditService).log(any(), any(), any(), any());
	}

	@Test
	void unlockOfAlreadyUnlockedAccountFailsWithoutTouchingRedis() {
		User user = lockedUser();
		user.setAccountLocked(false);
		when(userRepository.findById(TARGET_USER_ID)).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> adminService.unlockUserAccount(TARGET_USER_ID))
				.isInstanceOf(RuntimeException.class).hasMessageContaining("already unlocked");

		verify(redisLoginAttemptService, never()).reset(any());
		verify(userRepository, never()).save(any());
	}
}
