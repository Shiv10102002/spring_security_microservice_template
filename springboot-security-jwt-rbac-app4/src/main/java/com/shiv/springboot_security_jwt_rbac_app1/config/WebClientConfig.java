package com.shiv.springboot_security_jwt_rbac_app1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

	@Bean
	WebClient.Builder webClientBuilder() {
		return WebClient.builder();
	}
}