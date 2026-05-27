package com.subscriptions.api.dto;

import com.subscriptions.api.model.Category;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AnalyticsResponse {

    private BigDecimal totalMonthlySpend;
    private BigDecimal totalYearlySpend;
    private Integer activeSubscriptionsCount;
    private Map<Category, Integer> subscriptionsByCategory;
    private SubscriptionResponse mostExpensiveSubscription;
    private List<SubscriptionResponse> upcomingRenewals;
}
