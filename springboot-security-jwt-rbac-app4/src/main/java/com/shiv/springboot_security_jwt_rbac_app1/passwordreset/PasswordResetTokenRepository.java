package com.shiv.springboot_security_jwt_rbac_app1.passwordreset;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.shiv.springboot_security_jwt_rbac_app1.entity.User;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

	Optional<PasswordResetToken> findByUser(User user);

	Optional<PasswordResetToken> findByOtp(String otp);
}
