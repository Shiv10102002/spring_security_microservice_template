package com.shiv.springboot_security_jwt_rbac_app1.service;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shiv.springboot_security_jwt_rbac_app1.entity.UserToken;
import com.shiv.springboot_security_jwt_rbac_app1.repo.UserTokenRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TokenCleanupService {

	private final UserTokenRepository userTokenRepository;

	// ✅ Constructor Injection (Best Practice)
	public TokenCleanupService(UserTokenRepository userTokenRepository) {
		this.userTokenRepository = userTokenRepository;
	}

	@Scheduled(fixedRate = 5 * 60 * 1000) // every 5 minutes
	@Transactional
	public void cleanupExpiredRefreshTokens() {

		Instant now = Instant.now();
		log.info("Token cleanup job started at: {}", now);

		// ===============================
		// MARK ACCESS TOKENS AS EXPIRED
		// ===============================

		List<UserToken> expiredAccessTokens = userTokenRepository.findAllByExpiredFalseAndAccessExpiryBefore(now);

		for (UserToken token : expiredAccessTokens) {

			token.setExpired(true);

			token.setRevoked(true);
		}

		userTokenRepository.saveAll(expiredAccessTokens);

		// ===============================
		// DELETE OLD REFRESH TOKENS
		// ===============================

		userTokenRepository.deleteAllByRefreshExpiryBefore(now);

		log.info("Token cleanup job completed | tokensMarkedExpired: {}", expiredAccessTokens.size());
	}
}
