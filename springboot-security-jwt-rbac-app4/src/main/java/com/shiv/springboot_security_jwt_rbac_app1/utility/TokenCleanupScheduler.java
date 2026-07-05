package com.shiv.springboot_security_jwt_rbac_app1.utility;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.shiv.springboot_security_jwt_rbac_app1.entity.UserToken;
import com.shiv.springboot_security_jwt_rbac_app1.repo.UserTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

	private final UserTokenRepository userTokenRepository;

	/**
	 * Runs every 5 minutes. Marks expired tokens automatically.
	 */
	@Scheduled(fixedRate = 300000)
	public void markExpiredTokens() {

		Instant now = Instant.now();

		List<UserToken> tokens = userTokenRepository.findAllByExpiredFalse();

		int updated = 0;

		for (UserToken token : tokens) {

			boolean accessExpired = token.getAccessExpiry() != null && token.getAccessExpiry().isBefore(now);

			boolean refreshExpired = token.getRefreshExpiry() != null && token.getRefreshExpiry().isBefore(now);

			if (accessExpired && refreshExpired) {

				token.setExpired(true);

				if (!token.isRevoked()) {
					token.setRevoked(true);
				}

				updated++;
			}
		}

		if (updated > 0) {
			userTokenRepository.saveAll(tokens);

			log.info("TOKEN CLEANUP COMPLETED | expiredTokens={}", updated);
		}
	}
}
