package com.shiv.eureka_server.monitoring;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class DiscoveryServerHealthIndicator implements HealthIndicator {

	@Override
	public Health health() {

		return Health.up().withDetail("service", "EUREKA-SERVER").withDetail("type", "SERVICE-REGISTRY")
				.withDetail("status", "RUNNING").build();
	}
}