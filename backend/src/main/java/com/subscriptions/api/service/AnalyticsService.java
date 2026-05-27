package com.subscriptions.api.service;

import com.subscriptions.api.dto.AnalyticsResponse;
import com.subscriptions.api.dto.SubscriptionResponse;
import com.subscriptions.api.model.BillingCycle;
import com.subscriptions.api.model.Category;
import com.subscriptions.api.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final SubscriptionRepository repository;
    private final SubscriptionService subscriptionService;

    public AnalyticsResponse getAnalytics() {
        // Full implementation on Day 2
        return AnalyticsResponse.builder()
                .totalMonthlySpend(BigDecimal.ZERO)
                .totalYearlySpend(BigDecimal.ZERO)
                .activeSubscriptionsCount(0)
                .subscriptionsByCategory(Map.of())
                .upcomingRenewals(List.of())
                .build();
    }

    public List<SubscriptionResponse> getUpcomingRenewals() {
        // Full implementation on Day 2
        return List.of();
    }

    public Map<String, Object> previewAdd(BigDecimal cost, BillingCycle billingCycle) {
        // Full implementation on Day 2
        return Map.of("message", "Preview not yet implemented");
    }

    public Map<String, Object> previewRemove(Long id) {
        // Full implementation on Day 2
        return Map.of("message", "Preview not yet implemented");
    }
}
