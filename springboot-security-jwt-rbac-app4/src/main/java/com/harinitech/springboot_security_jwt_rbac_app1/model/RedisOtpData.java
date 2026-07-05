package com.harinitech.springboot_security_jwt_rbac_app1.model;

import java.io.Serializable;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisOtpData implements Serializable {

	private String username;

	private String otpHash;

	private OtpPurpose purpose;

	private Instant expiryTime;

	private int attempts;

	private Instant lastAttemptAt;
}