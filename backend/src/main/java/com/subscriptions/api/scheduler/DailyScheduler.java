package com.subscriptions.api.scheduler;

import com.subscriptions.api.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyScheduler {

    private final ReminderService reminderService;

    @Scheduled(cron = "0 35 5 * * *")
    public void runDailyChecks() {
        log.info("Daily scheduler running at {}", LocalDateTime.now());
        reminderService.checkUpcomingRenewals();
        reminderService.checkExpiringTrials();
        reminderService.updateOverdueRenewalDates();
        log.info("Daily scheduler complete");
    }
}
