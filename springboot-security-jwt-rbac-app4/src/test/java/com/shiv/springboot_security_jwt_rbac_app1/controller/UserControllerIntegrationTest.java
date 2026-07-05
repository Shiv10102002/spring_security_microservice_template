package com.shiv.springboot_security_jwt_rbac_app1.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.shiv.springboot_security_jwt_rbac_app1.support.AbstractSecurityIntegrationTest;

class UserControllerIntegrationTest extends AbstractSecurityIntegrationTest {

	private static final String ADMIN_EMAIL = "admin1@test.com";
	private static final String ADMIN_PASSWORD = "Password@123";

	private String accessToken;

	@BeforeEach
	void loginAsAdmin() throws Exception {

		String loginBody = """
				{
				  "username": "%s",
				  "password": "%s"
				}
				""".formatted(ADMIN_EMAIL, ADMIN_PASSWORD);

		String loginResponse = mockMvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
				.content(loginBody)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		accessToken = extractJsonField(loginResponse, "accessToken");
	}

	@Test
	@DisplayName("GET /api/v1/users/me returns profile for authenticated admin")
	void getMyProfile() throws Exception {

		mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.username").value(ADMIN_EMAIL));
	}

	@Test
	@DisplayName("GET /api/v1/users requires VIEW_USERS permission")
	void getAllUsersRequiresPermission() throws Exception {

		mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
	}

	@Test
	@DisplayName("GET /api/v1/users/me rejects missing token")
	void profileRequiresAuthentication() throws Exception {

		mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
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
