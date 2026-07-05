package com.shiv.springboot_security_jwt_rbac_app1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

	private static final String SECURITY_SCHEME_NAME = "bearerAuth";

	@Bean
	public OpenAPI customOpenAPI() {

		return new OpenAPI()

				// ✅ API Info
				.info(new Info().title("RBAC Security API").version("1.0.0")
						.description("Spring Boot 3 JWT Role-Based Access Control API")
						.contact(new Contact().name("HariniTech").email("support@shiv.com")))

				// ✅ Register JWT security scheme
				.components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
						new SecurityScheme().name(SECURITY_SCHEME_NAME).type(SecurityScheme.Type.HTTP).scheme("bearer")
								.bearerFormat("JWT")
								.description("Paste your JWT access token here. Example: eyJhbGci...")))
				// ✅ Apply JWT globally to ALL endpoints
				.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))

				// 🟢 ADD THIS LINE:
				.addServersItem(new Server().url("/").description("Default Server (Relative Path)"));
	}
}