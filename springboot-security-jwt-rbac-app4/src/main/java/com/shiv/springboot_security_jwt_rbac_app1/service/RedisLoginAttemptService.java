package com.shiv.springboot_security_jwt_rbac_app1.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisLoginAttemptService {

	private static final String PREFIX = "login:attempts:";

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@Value("${security.account.failed-attempt-expiry-minutes}")
	private long failedAttemptExpiryMinutes;

	private String key(String username) {
		return PREFIX + username.trim().toLowerCase();
	}

	public long increment(String username) {

		String key = key(username);

		Long attempts = redisTemplate.opsForValue().increment(key);

		if (attempts != null && attempts == 1) {

			redisTemplate.expire(key, Duration.ofMinutes(failedAttemptExpiryMinutes));
		}

		return attempts == null ? 0 : attempts;
	}

	public long getAttempts(String username) {

		Object value = redisTemplate.opsForValue().get(key(username));

		if (value == null) {
			return 0;
		}

		return Long.parseLong(value.toString());
	}

	public void reset(String username) {
		redisTemplate.delete(key(username));
	}
}