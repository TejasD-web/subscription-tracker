package com.subscriptions.api.dto;

import com.subscriptions.api.model.BillingCycle;
import com.subscriptions.api.model.Category;
import com.subscriptions.api.model.SubscriptionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SubscriptionRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Cost is required")
    @DecimalMin(value = "0.00", message = "Cost must be zero or greater")
    private BigDecimal cost;

    @NotNull(message = "Billing cycle is required")
    private BillingCycle billingCycle;

    @NotNull(message = "Category is required")
    private Category category;

    @NotNull(message = "Subscription type is required")
    private SubscriptionType subscriptionType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "Next renewal date is required")
    private LocalDate nextRenewalDate;

    private LocalDate trialEndDate;

    private Integer reminderDaysBefore = 7;

    private String cancellationUrl;

    private String paymentMethod;

    private String description;
}
