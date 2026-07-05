package com.harinitech.config_server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class StartupLogger implements ApplicationRunner {

	@Value("${server.port}")
	private String serverPort;

	@Value("${spring.application.name}")
	private String applicationName;

	@Override
	public void run(ApplicationArguments args) {

		log.info("====================================================");
		log.info("{} STARTED SUCCESSFULLY", applicationName.toUpperCase());
		log.info("====================================================");
		log.info("Application Name : {}", applicationName);
		log.info("Server Port      : {}", serverPort);
		log.info("Config Endpoint  : http://localhost:{}", serverPort);
		log.info("Health Endpoint  : http://localhost:{}/actuator/health", serverPort);
		log.info("Info Endpoint    : http://localhost:{}/actuator/info", serverPort);
		log.info("====================================================");
	}
}