package com.shiv.config_server.config;

import java.util.Map;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class ApplicationInfoContributor implements InfoContributor {

	@Override
	public void contribute(Info.Builder builder) {

		builder.withDetail("application",
				Map.of("service", "CONFIG-SERVER", "type", "CENTRALIZED-CONFIGURATION", "version", "1.0.0"));
	}
}
