package com.subscriptions.api.repository;

import com.subscriptions.api.model.BillingCycle;
import com.subscriptions.api.model.Category;
import com.subscriptions.api.model.Subscription;
import com.subscriptions.api.model.SubscriptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // All active subscriptions
    List<Subscription> findByIsActiveTrue();

    // All subscriptions in a given category
    List<Subscription> findByCategory(Category category);

    // Subscriptions renewing on or before a given date (for upcoming renewals)
    List<Subscription> findByIsActiveTrueAndNextRenewalDateLessThanEqual(LocalDate date);

    // Subscriptions by type (ACTIVE, TRIAL, TRYING_OUT)
    List<Subscription> findBySubscriptionType(SubscriptionType subscriptionType);

    // Trials/try-outs whose end date is on or before a given date (for expiry reminders)
    List<Subscription> findByIsActiveTrueAndTrialEndDateLessThanEqual(LocalDate date);
}
