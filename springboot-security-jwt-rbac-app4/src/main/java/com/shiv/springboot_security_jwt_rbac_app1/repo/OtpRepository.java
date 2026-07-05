package com.shiv.springboot_security_jwt_rbac_app1.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.shiv.springboot_security_jwt_rbac_app1.entity.OtpToken;

public interface OtpRepository extends JpaRepository<OtpToken, Long> {

	Optional<OtpToken> findTopByUsernameOrderByIdDesc(String username);

	@Modifying
	@Query("""
			UPDATE OtpToken o
			SET o.used = true
			WHERE o.username = :username
			AND o.used = false
			""")
	void invalidateAllActiveOtps(String username);
}
