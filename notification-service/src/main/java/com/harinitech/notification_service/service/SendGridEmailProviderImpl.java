package com.harinitech.notification_service.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SendGridEmailProviderImpl implements EmailProvider {

	private static final String SEND_ENDPOINT = "mail/send";
	private static final String CONTENT_TYPE = "text/plain; charset=utf-8";

	@Value("${sendgrid.api.key}")
	private String apiKey;

	@Value("${sendgrid.sender.email}")
	private String senderEmail;

	@Value("${sendgrid.sender.name}")
	private String senderName;

	@PostConstruct
	void validateConfiguration() {

		if (!StringUtils.hasText(apiKey)) {
			throw new IllegalStateException("SendGrid API key is not configured (sendgrid.api.key / SENDGRID_API_KEY)");
		}

		if (!StringUtils.hasText(senderEmail)) {
			throw new IllegalStateException(
					"SendGrid sender email is not configured (sendgrid.sender.email / SENDGRID_SENDER_MAIL)");
		}

		if (!StringUtils.hasText(senderName)) {
			throw new IllegalStateException(
					"SendGrid sender name is not configured (sendgrid.sender.name / SENDGRID_SENDER_NAME)");
		}

		log.info("SendGrid email provider initialized | senderEmail={}", senderEmail);
	}

	@Override
	public void sendEmail(String recipient, String subject, String body) {

		validateRequest(recipient, subject, body);

		log.info("Sending email via SendGrid | recipient={} | subject={}", recipient, subject);

		Email from = new Email(senderEmail.trim(), senderName.trim());
		Email to = new Email(recipient.trim());
		Content content = new Content("text/plain", body);
		Mail mail = new Mail(from, subject.trim(), to, content);

		try {

			SendGrid sendGrid = new SendGrid(apiKey.trim());
			Request request = new Request();

			request.setMethod(Method.POST);
			request.setEndpoint(SEND_ENDPOINT);
			request.setBody(mail.build());

			Response response = sendGrid.api(request);
			int statusCode = response.getStatusCode();

			if (statusCode < 200 || statusCode >= 300) {

				String errorDetail = truncate(response.getBody(), 500);

				log.error("SendGrid delivery failed | recipient={} | subject={} | status={} | response={}", recipient,
						subject, statusCode, errorDetail);

				throw new EmailDeliveryException(
						"SendGrid returned HTTP " + statusCode + (errorDetail.isBlank() ? "" : ": " + errorDetail));
			}

			log.info("Email sent successfully via SendGrid | recipient={} | subject={} | status={}", recipient, subject,
					statusCode);

		} catch (IOException ex) {

			log.error("SendGrid API call failed | recipient={} | subject={} | error={}", recipient, subject,
					ex.getMessage(), ex);

			throw new EmailDeliveryException("Failed to call SendGrid API", ex);
		}
	}

	private void validateRequest(String recipient, String subject, String body) {

		if (!StringUtils.hasText(recipient)) {
			throw new IllegalArgumentException("Recipient email must not be blank");
		}

		if (!StringUtils.hasText(subject)) {
			throw new IllegalArgumentException("Email subject must not be blank");
		}

		if (!StringUtils.hasText(body)) {
			throw new IllegalArgumentException("Email body must not be blank");
		}
	}

	private String truncate(String value, int maxLength) {

		if (value == null || value.isBlank()) {
			return "";
		}

		return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
	}

	/**
	 * Thrown when SendGrid rejects or fails to deliver an email.
	 * NotificationServiceImpl catches this and marks the notification as FAILED.
	 */
	public static class EmailDeliveryException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public EmailDeliveryException(String message) {
			super(message);
		}

		public EmailDeliveryException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}