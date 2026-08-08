package com.subscriptions.api.service;

import com.subscriptions.api.model.*;
import com.subscriptions.api.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private SubscriptionRepository repository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ReminderService reminderService;

    private Subscription renewingSoon;
    private Subscription trialExpiring;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reminderService, "reminderEmail", "test@example.com");

        renewingSoon = Subscription.builder()
                .id(1L)
                .name("Netflix")
                .cost(new BigDecimal("15.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .category(Category.STREAMING)
                .subscriptionType(SubscriptionType.ACTIVE)
                .startDate(LocalDate.now().minusMonths(1))
                .nextRenewalDate(LocalDate.now().plusDays(3))
                .reminderDaysBefore(7)
                .isActive(true)
                .build();

        trialExpiring = Subscription.builder()
                .id(2L)
                .name("Spotify")
                .cost(new BigDecimal("11.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .category(Category.MUSIC)
                .subscriptionType(SubscriptionType.TRIAL)
                .startDate(LocalDate.now().minusDays(20))
                .nextRenewalDate(LocalDate.now().plusDays(10))
                .trialEndDate(LocalDate.now().plusDays(4))
                .reminderDaysBefore(7)
                .isActive(true)
                .build();
    }

    @Test
    void checkUpcomingRenewals_sendsEmailForRenewingSoon() {
        when(repository.findByIsActiveTrue()).thenReturn(List.of(renewingSoon));
        reminderService.checkUpcomingRenewals();
        verify(emailService).sendRenewalReminder(renewingSoon, "test@example.com");
    }

    @Test
    void checkUpcomingRenewals_doesNotSendEmailTooEarly() {
        renewingSoon.setNextRenewalDate(LocalDate.now().plusDays(30));
        when(repository.findByIsActiveTrue()).thenReturn(List.of(renewingSoon));
        reminderService.checkUpcomingRenewals();
        verify(emailService, never()).sendRenewalReminder(any(), any());
    }

    @Test
    void checkExpiringTrials_sendsTrialPrompt() {
        when(repository.findByIsActiveTrueAndTrialEndDateLessThanEqual(any()))
                .thenReturn(List.of(trialExpiring));
        reminderService.checkExpiringTrials();
        verify(emailService).sendTrialExpiryPrompt(trialExpiring, "test@example.com");
    }

    @Test
    void checkExpiringTrials_noExpiring_doesNotSendEmail() {
        when(repository.findByIsActiveTrueAndTrialEndDateLessThanEqual(any()))
                .thenReturn(List.of());
        reminderService.checkExpiringTrials();
        verify(emailService, never()).sendTrialExpiryPrompt(any(), any());
    }

    @Test
    void updateOverdueRenewalDates_advancesOverdueDate() {
        renewingSoon.setNextRenewalDate(LocalDate.now().minusDays(5));
        when(repository.findByIsActiveTrue()).thenReturn(List.of(renewingSoon));
        reminderService.updateOverdueRenewalDates();
        verify(repository).save(argThat(s -> s.getNextRenewalDate().isAfter(LocalDate.now())));
    }
}
