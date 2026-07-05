package com.shiv.springboot_security_jwt_rbac_app1.config;

import java.util.TimeZone;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class TimeZoneConfig {

	@PostConstruct
	public void init() {

		// ✅ FIXED: Aligned with application.properties UTC configuration
		// This ensures consistent timezone across JVM, Hibernate, and Jackson
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

		log.info("Application TimeZone set to: {}", TimeZone.getDefault().getID());
	}
}