package com.harinitech.eureka_server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupLogger implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

	@Value("${server.port}")
	private String port;

	@Override
	public void run(ApplicationArguments args) {

		log.info("========================================================");
		log.info("EUREKA DISCOVERY SERVER STARTED SUCCESSFULLY");
		log.info("========================================================");
		log.info("Application Name : eureka-server");
		log.info("Server Port      : {}", port);
		log.info("Dashboard URL    : http://localhost:{}/", port);
		log.info("Health Endpoint  : http://localhost:{}/actuator/health", port);
		log.info("Info Endpoint    : http://localhost:{}/actuator/info", port);
		log.info("========================================================");
	}
}