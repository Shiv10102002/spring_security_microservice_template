package com.shiv.notification_service.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.shiv.notification_service.entity.NotificationType;
import com.shiv.notification_service.service.EmailProvider;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerIntegrationTest {

	private static final String API_KEY = "test-internal-api-key-minimum-32-chars-long";

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private EmailProvider emailProvider;

	@Test
	@DisplayName("GET /api/v1/notifications/health requires internal API key")
	void healthEndpointRequiresApiKey() throws Exception {

		mockMvc.perform(get("/api/v1/notifications/health")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("GET /api/v1/notifications/health returns UP with API key")
	void healthEndpointWithApiKey() throws Exception {

		mockMvc.perform(get("/api/v1/notifications/health").header("X-Internal-Api-Key", API_KEY))
				.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value("UP"));
	}

	@Test
	@DisplayName("GET /actuator/health is public")
	void actuatorHealthIsPublic() throws Exception {

		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	@DisplayName("POST /api/v1/notifications requires internal API key")
	void sendNotificationRequiresApiKey() throws Exception {

		String body = """
				{
				  "recipient": "user@example.com",
				  "type": "REGISTRATION_OTP",
				  "otp": "123456"
				}
				""";

		mockMvc.perform(post("/api/v1/notifications").contentType(APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("POST /api/v1/notifications accepts valid request with API key")
	void sendNotificationWithApiKey() throws Exception {

		String body = """
				{
				  "recipient": "user@example.com",
				  "type": "REGISTRATION_OTP",
				  "otp": "123456"
				}
				""";

		mockMvc.perform(post("/api/v1/notifications").contentType(APPLICATION_JSON).content(body)
				.header("X-Internal-Api-Key", API_KEY)).andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.notificationId").exists());
	}

	@Test
	@DisplayName("GET /api/v1/notifications/statistics returns counts")
	void getStatistics() throws Exception {

		mockMvc.perform(get("/api/v1/notifications/statistics").header("X-Internal-Api-Key", API_KEY))
				.andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.totalNotifications").exists());
	}
}
