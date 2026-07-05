package com.shiv.springboot_security_jwt_rbac_app1.passwordreset;


import java.util.Date;

import com.shiv.springboot_security_jwt_rbac_app1.entity.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Data;

@Data
@Entity
public class PasswordResetToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String otp;

	private Date expiryTime;

	@OneToOne
	private User user;

	// getters & setters
}
