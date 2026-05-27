package com.subscriptions.api.service;

import com.subscriptions.api.dto.SubscriptionRequest;
import com.subscriptions.api.dto.SubscriptionResponse;
import com.subscriptions.api.exception.ResourceNotFoundException;
import com.subscriptions.api.model.Subscription;
import com.subscriptions.api.model.Category;
import com.subscriptions.api.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository repository;

    // ── Create ───────────────────────────────────────────────

    public SubscriptionResponse create(SubscriptionRequest request) {
        Subscription subscription = mapToEntity(request);
        return mapToResponse(repository.save(subscription));
    }

    // ── Read ─────────────────────────────────────────────────

    public List<SubscriptionResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public SubscriptionResponse getById(Long id) {
        return mapToResponse(findOrThrow(id));
    }

    public List<SubscriptionResponse> getActive() {
        return repository.findByIsActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<SubscriptionResponse> getByCategory(Category category) {
        return repository.findByCategory(category)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Update ───────────────────────────────────────────────

    public SubscriptionResponse update(Long id, SubscriptionRequest request) {
        Subscription existing = findOrThrow(id);

        existing.setName(request.getName());
        existing.setCost(request.getCost());
        existing.setBillingCycle(request.getBillingCycle());
        existing.setCategory(request.getCategory());
        existing.setSubscriptionType(request.getSubscriptionType());
        existing.setStartDate(request.getStartDate());
        existing.setNextRenewalDate(request.getNextRenewalDate());
        existing.setTrialEndDate(request.getTrialEndDate());
        existing.setReminderDaysBefore(request.getReminderDaysBefore());
        existing.setCancellationUrl(request.getCancellationUrl());
        existing.setPaymentMethod(request.getPaymentMethod());
        existing.setDescription(request.getDescription());

        return mapToResponse(repository.save(existing));
    }

    // ── Delete ───────────────────────────────────────────────

    public void delete(Long id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    // ── Cancel / Reactivate ──────────────────────────────────

    public SubscriptionResponse cancel(Long id) {
        Subscription subscription = findOrThrow(id);
        subscription.setIsActive(false);
        return mapToResponse(repository.save(subscription));
    }

    public SubscriptionResponse reactivate(Long id) {
        Subscription subscription = findOrThrow(id);
        subscription.setIsActive(true);
        return mapToResponse(repository.save(subscription));
    }

    // ── Trial Confirm (email link actions) ───────────────────

    public SubscriptionResponse confirmKeep(Long id) {
        Subscription subscription = findOrThrow(id);
        subscription.setSubscriptionType(com.subscriptions.api.model.SubscriptionType.ACTIVE);
        subscription.setTrialEndDate(null);
        return mapToResponse(repository.save(subscription));
    }

    public void confirmCancel(Long id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    // ── Helpers ──────────────────────────────────────────────

    public Subscription findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subscription not found with id: " + id));
    }

    public SubscriptionResponse mapToResponse(Subscription s) {
        return SubscriptionResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .cost(s.getCost())
                .billingCycle(s.getBillingCycle())
                .category(s.getCategory())
                .subscriptionType(s.getSubscriptionType())
                .startDate(s.getStartDate())
                .nextRenewalDate(s.getNextRenewalDate())
                .trialEndDate(s.getTrialEndDate())
                .reminderDaysBefore(s.getReminderDaysBefore())
                .cancellationUrl(s.getCancellationUrl())
                .isActive(s.getIsActive())
                .paymentMethod(s.getPaymentMethod())
                .description(s.getDescription())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private Subscription mapToEntity(SubscriptionRequest request) {
        return Subscription.builder()
                .name(request.getName())
                .cost(request.getCost())
                .billingCycle(request.getBillingCycle())
                .category(request.getCategory())
                .subscriptionType(request.getSubscriptionType())
                .startDate(request.getStartDate())
                .nextRenewalDate(request.getNextRenewalDate())
                .trialEndDate(request.getTrialEndDate())
                .reminderDaysBefore(
                        request.getReminderDaysBefore() != null
                                ? request.getReminderDaysBefore()
                                : 7)
                .cancellationUrl(request.getCancellationUrl())
                .paymentMethod(request.getPaymentMethod())
                .description(request.getDescription())
                .build();
    }
}
