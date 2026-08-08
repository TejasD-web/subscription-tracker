package com.subscriptions.api.repository;

import com.subscriptions.api.model.Category;
import com.subscriptions.api.model.Subscription;
import com.subscriptions.api.model.SubscriptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByIsActiveTrue();

    List<Subscription> findByCategory(Category category);

    List<Subscription> findByIsActiveTrueAndNextRenewalDateLessThanEqual(LocalDate date);

    List<Subscription> findBySubscriptionType(SubscriptionType subscriptionType);

    List<Subscription> findByIsActiveTrueAndTrialEndDateLessThanEqual(LocalDate date);

    Optional<Subscription> findByPlaidStreamId(String plaidStreamId);
}
