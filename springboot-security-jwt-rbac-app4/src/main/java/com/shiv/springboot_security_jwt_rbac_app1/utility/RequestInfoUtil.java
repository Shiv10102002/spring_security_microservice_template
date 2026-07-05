package com.shiv.springboot_security_jwt_rbac_app1.utility;

import jakarta.servlet.http.HttpServletRequest;

public class RequestInfoUtil {

	// ✅ Get real client IP (proxy-safe)
	public static String getClientIp(HttpServletRequest request) {

		String ip = request.getHeader("X-Forwarded-For");

		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}

		// If multiple IPs → take first
		if (ip != null && ip.contains(",")) {
			ip = ip.split(",")[0].trim();
		}

		return ip;
	}

	// ✅ Get raw device info (User-Agent)
	public static String getDeviceInfo(HttpServletRequest request) {
		return request.getHeader("User-Agent");
	}
}