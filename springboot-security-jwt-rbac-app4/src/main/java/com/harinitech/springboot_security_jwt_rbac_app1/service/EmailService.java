package com.harinitech.springboot_security_jwt_rbac_app1.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.harinitech.springboot_security_jwt_rbac_app1.entity.User;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

@Service
public class EmailService {

	@Value("${sendgrid.api.key}")
	private String apiKey;

	@Value("${sendgrid.sender.email}")
	private String senderEmail;

	@Value("${sendgrid.sender.name}")
	private String senderName;

	// ======================== OTP ========================

	public void sendOtp(String toEmail, String otp) {
		Email from = new Email(senderEmail, senderName);
		Email to = new Email(toEmail);
		String subject = "🔐 Password Reset OTP - Secure Access";
		Content content = new Content("text/html", buildOtpTemplate(otp));
		send(from, to, subject, content);
	}

	// ======================== EMPLOYEE REGISTRATION NOTIFICATIONS
	// ========================

	/**
	 * Notify an admin about a new pending registration.
	 */
	public void sendPendingApprovalNotification(String adminEmail, User pendingUser) {
		Email from = new Email(senderEmail, senderName);
		Email to = new Email(adminEmail);
		String subject = "🆕 New Employee Registration Pending Approval";
		Content content = new Content("text/html",
				buildPendingApprovalTemplate(pendingUser.getUsername(), pendingUser.getRequestedRole()));
		send(from, to, subject, content);
	}

	/**
	 * Notify the employee that their registration has been approved.
	 */
	public void sendApprovalConfirmation(String toEmail, String assignedRole) {
		Email from = new Email(senderEmail, senderName);
		Email to = new Email(toEmail);
		String subject = "✅ Your Account Has Been Approved";
		Content content = new Content("text/html", buildApprovalTemplate(assignedRole));
		send(from, to, subject, content);
	}

	/**
	 * Notify the employee that their registration was rejected.
	 */
	public void sendRejectionNotice(String toEmail, String reason) {
		Email from = new Email(senderEmail, senderName);
		Email to = new Email(toEmail);
		String subject = "❌ Registration Status Update";
		Content content = new Content("text/html", buildRejectionTemplate(reason));
		send(from, to, subject, content);
	}

	// ======================== PRIVATE SEND HELPER ========================

	private void send(Email from, Email to, String subject, Content content) {
		Mail mail = new Mail(from, subject, to, content);
		SendGrid sg = new SendGrid(apiKey);
		Request request = new Request();
		try {
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());
			Response response = sg.api(request);
			if (response.getStatusCode() >= 400) {
				throw new RuntimeException("SendGrid Error: " + response.getBody());
			}
		} catch (Exception e) {
			throw new RuntimeException("Error sending email via SendGrid", e);
		}
	}

	// ======================== HTML TEMPLATES ========================

	private String buildOtpTemplate(String otp) {
		return "<!DOCTYPE html>" + "<html>"
				+ "<body style='font-family: Arial, sans-serif; background-color:#f4f4f4; padding:20px;'>"
				+ "<div style='max-width:600px; margin:auto; background:white; padding:20px; border-radius:10px;'>"
				+ "<div style='text-align:center;'>" + "<h2 style='color:#333;'>Security Verification</h2>" + "</div>"
				+ "<p>Hello User,</p>" + "<p>Your One-Time Password (OTP) for password reset is:</p>"
				+ "<div style='text-align:center; margin:20px;'>"
				+ "<span style='font-size:28px; font-weight:bold; color:#4CAF50; letter-spacing:5px;'>" + otp
				+ "</span>" + "</div>" + "<p>This OTP is valid for <b>5 minutes</b>.</p>"
				+ "<p>If you did not request this, please ignore this email.</p>" + "<hr/>"
				+ "<p style='font-size:12px; color:gray;'>© 2026 Secure Auth System</p>" + "</div>" + "</body></html>";
	}

	private String buildPendingApprovalTemplate(String email, String requestedRole) {
		return "<!DOCTYPE html>" + "<html>"
				+ "<body style='font-family: Arial, sans-serif; background-color:#f4f4f4; padding:20px;'>"
				+ "<div style='max-width:600px; margin:auto; background:white; padding:20px; border-radius:10px;'>"
				+ "<h2 style='color:#333;'>🔔 New Registration Request</h2>"
				+ "<p>A new employee has requested access to the system.</p>"
				+ "<table style='width:100%; border-collapse:collapse; margin:20px 0;'>"
				+ "<tr><td style='padding:8px; border-bottom:1px solid #ddd;'><strong>Email:</strong></td>"
				+ "<td style='padding:8px; border-bottom:1px solid #ddd;'>" + email + "</td></tr>"
				+ "<tr><td style='padding:8px; border-bottom:1px solid #ddd;'><strong>Requested Role:</strong></td>"
				+ "<td style='padding:8px; border-bottom:1px solid #ddd;'>" + requestedRole + "</td></tr>" + "</table>"
				+ "<p>Please review and approve or reject this request from the admin panel.</p>" + "<hr/>"
				+ "<p style='font-size:12px; color:gray;'>© 2026 Secure Auth System</p>" + "</div>" + "</body></html>";
	}

	private String buildApprovalTemplate(String role) {
		return "<!DOCTYPE html>" + "<html>"
				+ "<body style='font-family: Arial, sans-serif; background-color:#f4f4f4; padding:20px;'>"
				+ "<div style='max-width:600px; margin:auto; background:white; padding:20px; border-radius:10px;'>"
				+ "<h2 style='color:#333; text-align:center;'>✅ Account Approved</h2>"
				+ "<p>Congratulations! Your registration has been <strong>approved</strong>.</p>"
				+ "<p>You have been assigned the role: <strong style='color:#4CAF50;'>" + role + "</strong></p>"
				+ "<p>You can now <a href='#' style='color:#1a73e8;'>log in to your account</a> and start using the system.</p>"
				+ "<hr/>" + "<p style='font-size:12px; color:gray;'>© 2026 Secure Auth System</p>" + "</div>"
				+ "</body></html>";
	}

	private String buildRejectionTemplate(String reason) {
		return "<!DOCTYPE html>" + "<html>"
				+ "<body style='font-family: Arial, sans-serif; background-color:#f4f4f4; padding:20px;'>"
				+ "<div style='max-width:600px; margin:auto; background:white; padding:20px; border-radius:10px;'>"
				+ "<h2 style='color:#333; text-align:center;'>❌ Registration Status</h2>"
				+ "<p>We regret to inform you that your registration request has been <strong>rejected</strong>.</p>"
				+ "<p><strong>Reason:</strong> " + reason + "</p>"
				+ "<p>If you believe this is an error, please contact the system administrator.</p>" + "<hr/>"
				+ "<p style='font-size:12px; color:gray;'>© 2026 Secure Auth System</p>" + "</div>" + "</body></html>";
	}
}