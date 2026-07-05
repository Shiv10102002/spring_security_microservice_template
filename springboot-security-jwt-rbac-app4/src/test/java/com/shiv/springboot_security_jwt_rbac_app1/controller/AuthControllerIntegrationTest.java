package com.shiv.springboot_security_jwt_rbac_app1.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.shiv.springboot_security_jwt_rbac_app1.support.AbstractSecurityIntegrationTest;

class AuthControllerIntegrationTest extends AbstractSecurityIntegrationTest {

	private static final String ADMIN_EMAIL = "admin1@test.com";
	private static final String ADMIN_PASSWORD = "Password@123";

	@Test
	@DisplayName("POST /api/v1/auth/login returns access and refresh tokens for valid admin credentials")
	void loginWithValidCredentials() throws Exception {

		String body = """
				{
				  "username": "%s",
				  "password": "%s"
				}
				""".formatted(ADMIN_EMAIL, ADMIN_PASSWORD);

		mockMvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.accessToken").value(notNullValue()))
				.andExpect(jsonPath("$.data.refreshToken").value(notNullValue()))
				.andExpect(jsonPath("$.data.role").value("ADMIN"));
	}

	@Test
	@DisplayName("POST /api/v1/auth/login rejects invalid credentials")
	void loginWithInvalidCredentials() throws Exception {

		String body = """
				{
				  "username": "admin1@test.com",
				  "password": "wrong-password"
				}
				""";

		mockMvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("POST /api/v1/auth/validate-token accepts a freshly issued access token")
	void validateFreshAccessToken() throws Exception {

		String loginBody = """
				{
				  "username": "%s",
				  "password": "%s"
				}
				""".formatted(ADMIN_EMAIL, ADMIN_PASSWORD);

		String loginResponse = mockMvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
				.content(loginBody)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		String accessToken = extractJsonField(loginResponse, "accessToken");

		String validateBody = """
				{
				  "token": "%s"
				}
				""".formatted(accessToken);

		mockMvc.perform(post("/api/v1/auth/validate-token").contentType(APPLICATION_JSON).content(validateBody))
				.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
	}

	@Test
	@DisplayName("GET /actuator/health is publicly accessible")
	void actuatorHealthIsPublic() throws Exception {

		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	private String extractJsonField(String json, String fieldName) {

		int fieldIndex = json.indexOf("\"" + fieldName + "\"");
		if (fieldIndex < 0) {
			throw new IllegalStateException("Field not found in response: " + fieldName);
		}

		int colonIndex = json.indexOf(':', fieldIndex);
		int startQuote = json.indexOf('"', colonIndex + 1);
		int endQuote = json.indexOf('"', startQuote + 1);

		return json.substring(startQuote + 1, endQuote);
	}
}
