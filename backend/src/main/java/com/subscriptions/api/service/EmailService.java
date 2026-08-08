package com.subscriptions.api.service;

import com.subscriptions.api.model.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:not-configured}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public void sendRenewalReminder(Subscription subscription, String toEmail) {
        String subject = "Reminder: " + subscription.getName() + " renews on "
                + subscription.getNextRenewalDate();

        String body = """
                <h2>Upcoming Renewal Reminder</h2>
                <p>Your subscription to <strong>%s</strong> renews on <strong>%s</strong>.</p>
                <p>Cost: <strong>$%s / %s</strong></p>
                %s
                <p>If you'd like to cancel, visit: <a href="%s">%s</a></p>
                <p>— Subscription Tracker</p>
                """.formatted(
                subscription.getName(),
                subscription.getNextRenewalDate(),
                subscription.getCost(),
                subscription.getBillingCycle().toString().toLowerCase(),
                subscription.getPaymentMethod() != null
                        ? "<p>Payment method: " + subscription.getPaymentMethod() + "</p>" : "",
                subscription.getCancellationUrl() != null ? subscription.getCancellationUrl() : "#",
                subscription.getCancellationUrl() != null ? subscription.getCancellationUrl() : "N/A"
        );

        sendHtmlEmail(toEmail, subject, body);
    }

    public void sendTrialExpiryPrompt(Subscription subscription, String toEmail) {
        String keepUrl = baseUrl + "/api/subscriptions/" + subscription.getId() + "/confirm?action=keep";
        String cancelUrl = baseUrl + "/api/subscriptions/" + subscription.getId() + "/confirm?action=cancel";

        String subject = "Your " + subscription.getName() + " trial ends on " + subscription.getTrialEndDate();

        String body = """
                <h2>Trial Ending Soon</h2>
                <p>Your %s trial for <strong>%s</strong> ends on <strong>%s</strong>.</p>
                <p>Cost if you keep it: <strong>$%s / %s</strong></p>
                <br>
                <p>
                  <a href="%s" style="background:#22c55e;color:white;padding:10px 20px;text-decoration:none;border-radius:4px;">
                    ✅ Yes, keep it
                  </a>
                  &nbsp;&nbsp;
                  <a href="%s" style="background:#ef4444;color:white;padding:10px 20px;text-decoration:none;border-radius:4px;">
                    ❌ No, cancel it
                  </a>
                </p>
                <br>
                <p><em>If you need more time, just update the trial end date in the app.</em></p>
                <p>— Subscription Tracker</p>
                """.formatted(
                subscription.getSubscriptionType().toString().toLowerCase().replace("_", " "),
                subscription.getName(),
                subscription.getTrialEndDate(),
                subscription.getCost(),
                subscription.getBillingCycle().toString().toLowerCase(),
                keepUrl,
                cancelUrl
        );

        sendHtmlEmail(toEmail, subject, body);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (mailSender == null || "not-configured".equals(fromEmail)) {
            log.warn("Email not configured — skipping send to {}. Subject: {}", to, subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} — {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
