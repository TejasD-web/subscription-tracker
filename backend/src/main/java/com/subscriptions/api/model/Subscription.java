package com.subscriptions.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Cost is required")
    @DecimalMin(value = "0.00", message = "Cost must be zero or greater")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cost;

    @NotNull(message = "Billing cycle is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingCycle billingCycle;

    @NotNull(message = "Category is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @NotNull(message = "Subscription type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionType subscriptionType;

    @NotNull(message = "Start date is required")
    @Column(nullable = false)
    private LocalDate startDate;

    @NotNull(message = "Next renewal date is required")
    @Column(nullable = false)
    private LocalDate nextRenewalDate;

    /** Only set for TRIAL or TRYING_OUT subscriptions */
    private LocalDate trialEndDate;

    /** How many days before renewal to send a reminder email (defaults to 7) */
    @Builder.Default
    @Column(nullable = false)
    private Integer reminderDaysBefore = 7;

    /** Direct link to cancel this service — shown to user before navigating away */
    private String cancellationUrl;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    /** e.g. "Credit Card ending in 1234" */
    private String paymentMethod;

    /** Optional notes */
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
