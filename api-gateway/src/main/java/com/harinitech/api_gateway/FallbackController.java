package com.harinitech.api_gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FallbackController {

	@RequestMapping("/fallback")
	public ResponseEntity<Map<String, Object>> fallback() {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("success", false, "message",
				"Security service is currently unavailable. Please try again later.", "status", 503));
	}
}