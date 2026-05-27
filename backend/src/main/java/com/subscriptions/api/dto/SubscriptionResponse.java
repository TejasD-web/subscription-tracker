package com.subscriptions.api.dto;

import com.subscriptions.api.model.BillingCycle;
import com.subscriptions.api.model.Category;
import com.subscriptions.api.model.SubscriptionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionResponse {

    private Long id;
    private String name;
    private BigDecimal cost;
    private BillingCycle billingCycle;
    private Category category;
    private SubscriptionType subscriptionType;
    private LocalDate startDate;
    private LocalDate nextRenewalDate;
    private LocalDate trialEndDate;
    private Integer reminderDaysBefore;
    private String cancellationUrl;
    private Boolean isActive;
    private String paymentMethod;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
