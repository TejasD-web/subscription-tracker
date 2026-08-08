package com.subscriptions.api.service;

import com.subscriptions.api.dto.AnalyticsResponse;
import com.subscriptions.api.dto.SubscriptionResponse;
import com.subscriptions.api.model.BillingCycle;
import com.subscriptions.api.model.Category;
import com.subscriptions.api.model.Subscription;
import com.subscriptions.api.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final SubscriptionRepository repository;
    private final SubscriptionService subscriptionService;

    public AnalyticsResponse getAnalytics() {
        List<Subscription> active = repository.findByIsActiveTrue();

        BigDecimal monthlyTotal = active.stream()
                .map(s -> toMonthly(s.getCost(), s.getBillingCycle()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal yearlyTotal = monthlyTotal.multiply(BigDecimal.valueOf(12))
                .setScale(2, RoundingMode.HALF_UP);

        Map<Category, Integer> byCategory = active.stream()
                .collect(Collectors.groupingBy(
                        Subscription::getCategory,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        Subscription mostExpensive = active.stream()
                .max((a, b) -> toMonthly(a.getCost(), a.getBillingCycle())
                        .compareTo(toMonthly(b.getCost(), b.getBillingCycle())))
                .orElse(null);

        List<SubscriptionResponse> upcoming = getUpcomingRenewals();

        return AnalyticsResponse.builder()
                .totalMonthlySpend(monthlyTotal.setScale(2, RoundingMode.HALF_UP))
                .totalYearlySpend(yearlyTotal)
                .activeSubscriptionsCount(active.size())
                .subscriptionsByCategory(byCategory)
                .mostExpensiveSubscription(mostExpensive != null
                        ? subscriptionService.mapToResponse(mostExpensive) : null)
                .upcomingRenewals(upcoming)
                .build();
    }

    public List<SubscriptionResponse> getUpcomingRenewals() {
        LocalDate cutoff = LocalDate.now().plusDays(30);
        return repository.findByIsActiveTrueAndNextRenewalDateLessThanEqual(cutoff)
                .stream()
                .map(subscriptionService::mapToResponse)
                .collect(Collectors.toList());
    }

    public Map<String, Object> previewAdd(BigDecimal cost, BillingCycle billingCycle) {
        BigDecimal currentMonthly = getCurrentMonthlyTotal();
        BigDecimal addMonthly = toMonthly(cost, billingCycle);
        BigDecimal newMonthly = currentMonthly.add(addMonthly).setScale(2, RoundingMode.HALF_UP);

        return Map.of(
                "currentMonthlyTotal", currentMonthly.setScale(2, RoundingMode.HALF_UP),
                "addedMonthlyCost", addMonthly.setScale(2, RoundingMode.HALF_UP),
                "newMonthlyTotal", newMonthly,
                "newYearlyTotal", newMonthly.multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP)
        );
    }

    public Map<String, Object> previewRemove(Long id) {
        Subscription sub = subscriptionService.findOrThrow(id);
        BigDecimal currentMonthly = getCurrentMonthlyTotal();
        BigDecimal removedMonthly = toMonthly(sub.getCost(), sub.getBillingCycle());
        BigDecimal newMonthly = currentMonthly.subtract(removedMonthly).setScale(2, RoundingMode.HALF_UP);

        return Map.of(
                "currentMonthlyTotal", currentMonthly.setScale(2, RoundingMode.HALF_UP),
                "removedMonthlySavings", removedMonthly.setScale(2, RoundingMode.HALF_UP),
                "removedYearlySavings", removedMonthly.multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP),
                "newMonthlyTotal", newMonthly,
                "newYearlyTotal", newMonthly.multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP)
        );
    }

    // ── Helpers ──────────────────────────────────────────────

    public BigDecimal toMonthly(BigDecimal cost, BillingCycle cycle) {
        return switch (cycle) {
            case WEEKLY -> cost.multiply(BigDecimal.valueOf(52))
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            case MONTHLY -> cost.setScale(2, RoundingMode.HALF_UP);
            case YEARLY -> cost.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        };
    }

    private BigDecimal getCurrentMonthlyTotal() {
        return repository.findByIsActiveTrue().stream()
                .map(s -> toMonthly(s.getCost(), s.getBillingCycle()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
