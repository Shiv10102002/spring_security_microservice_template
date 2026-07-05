package com.harinitech.springboot_security_jwt_rbac_app1.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.harinitech.springboot_security_jwt_rbac_app1.entity.User;
import com.harinitech.springboot_security_jwt_rbac_app1.entity.UserToken;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

	// ======================== REFRESH TOKEN ========================

	// 🔥 Primary lookup (used in refresh flow)
	Optional<UserToken> findByRefreshToken(String refreshToken);

	// ✅ Find by access token (CRITICAL for JWT filter)
	Optional<UserToken> findByAccessToken(String accessToken);

	// ======================== USER TOKENS ========================

	// 🔥 Active tokens (not revoked & not expired)
	Page<UserToken> findAllByUserAndRevokedFalseAndExpiredFalse(User user, Pageable pageable);

	List<UserToken> findAllByUserAndRevokedFalseAndExpiredFalse(User user);

	// 🔥 All tokens (used for logout-all)
	List<UserToken> findAllByUser(User user);

	// ======================== CLEANUP ========================

	// 🔥 Find expired refresh tokens (for scheduler/logging if needed)
	List<UserToken> findAllByRefreshExpiryBefore(Instant now);

	// 🔥 Auto-delete expired tokens (BEST PRACTICE)
	void deleteAllByRefreshExpiryBefore(Instant now);

//	This method is used for token clean up service every 5 minutes explicitly
	List<UserToken> findAllByExpiredFalse();

//	this method is added for token expiry = 1 in db without depending on any user-request/
	List<UserToken> findAllByExpiredFalseAndAccessExpiryBefore(Instant now);

	// ======================== STATISTICS ========================

	long countByRevokedTrue();

	long countByRevokedFalse();
}