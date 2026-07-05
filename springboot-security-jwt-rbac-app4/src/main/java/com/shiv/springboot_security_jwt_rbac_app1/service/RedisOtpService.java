package com.shiv.springboot_security_jwt_rbac_app1.service;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiv.springboot_security_jwt_rbac_app1.model.OtpPurpose;
import com.shiv.springboot_security_jwt_rbac_app1.model.RedisOtpData;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisOtpService {

	private static final Duration OTP_TTL = Duration.ofMinutes(5);

	private final RedisTemplate<String, Object> redisTemplate;

	private final ObjectMapper objectMapper;

	private String buildKey(String email, OtpPurpose purpose) {

		return "otp:%s:%s".formatted(purpose.name(), email.trim().toLowerCase());
	}

	public void saveOtp(RedisOtpData otpData) {

		redisTemplate.opsForValue().set(buildKey(otpData.getUsername(), otpData.getPurpose()), otpData, OTP_TTL);
	}

	public Optional<RedisOtpData> getOtp(String email, OtpPurpose purpose) {

		Object value = redisTemplate.opsForValue().get(buildKey(email, purpose));

		if (value == null) {
			return Optional.empty();
		}

		RedisOtpData otpData = objectMapper.convertValue(value, RedisOtpData.class);

		return Optional.of(otpData);
	}

	public void updateOtp(RedisOtpData otpData) {

		saveOtp(otpData);
	}

	public void deleteOtp(String email, OtpPurpose purpose) {

		redisTemplate.delete(buildKey(email, purpose));
	}
}