package com.subscriptions.api.service;

import com.subscriptions.api.model.Subscription;
import com.subscriptions.api.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final SubscriptionRepository repository;
    private final EmailService emailService;

    @Value("${app.reminder.email:not-configured}")
    private String reminderEmail;

    public void checkUpcomingRenewals() {
        if ("not-configured".equals(reminderEmail)) {
            log.warn("Reminder email not configured — skipping renewal checks");
            return;
        }

        List<Subscription> active = repository.findByIsActiveTrue();
        LocalDate today = LocalDate.now();

        for (Subscription sub : active) {
            LocalDate reminderDate = sub.getNextRenewalDate()
                    .minusDays(sub.getReminderDaysBefore());
            if (!today.isBefore(reminderDate) && !today.isAfter(sub.getNextRenewalDate())) {
                log.info("Sending renewal reminder for: {}", sub.getName());
                emailService.sendRenewalReminder(sub, reminderEmail);
            }
        }
    }

    public void checkExpiringTrials() {
        if ("not-configured".equals(reminderEmail)) {
            log.warn("Reminder email not configured — skipping trial checks");
            return;
        }

        LocalDate cutoff = LocalDate.now().plusDays(7);
        List<Subscription> expiring = repository
                .findByIsActiveTrueAndTrialEndDateLessThanEqual(cutoff);

        for (Subscription sub : expiring) {
            log.info("Sending trial expiry prompt for: {}", sub.getName());
            emailService.sendTrialExpiryPrompt(sub, reminderEmail);
        }
    }

    public void updateOverdueRenewalDates() {
        List<Subscription> active = repository.findByIsActiveTrue();
        LocalDate today = LocalDate.now();

        for (Subscription sub : active) {
            if (sub.getNextRenewalDate().isBefore(today)) {
                LocalDate updated = advanceToNextRenewal(sub.getNextRenewalDate(), sub);
                sub.setNextRenewalDate(updated);
                repository.save(sub);
                log.info("Advanced renewal date for {} to {}", sub.getName(), updated);
            }
        }
    }

    private LocalDate advanceToNextRenewal(LocalDate date, Subscription sub) {
        LocalDate next = date;
        LocalDate today = LocalDate.now();
        while (!next.isAfter(today)) {
            next = switch (sub.getBillingCycle()) {
                case WEEKLY -> next.plusWeeks(1);
                case MONTHLY -> next.plusMonths(1);
                case YEARLY -> next.plusYears(1);
            };
        }
        return next;
    }
}
