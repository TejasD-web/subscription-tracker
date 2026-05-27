package com.subscriptions.api.controller;

import com.subscriptions.api.dto.AnalyticsResponse;
import com.subscriptions.api.dto.SubscriptionRequest;
import com.subscriptions.api.dto.SubscriptionResponse;
import com.subscriptions.api.model.Category;
import com.subscriptions.api.service.AnalyticsService;
import com.subscriptions.api.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final AnalyticsService analyticsService;

    // ── CRUD ─────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(
            @Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> getAll() {
        return ResponseEntity.ok(subscriptionService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        subscriptionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Filtering ────────────────────────────────────────────

    @GetMapping("/active")
    public ResponseEntity<List<SubscriptionResponse>> getActive() {
        return ResponseEntity.ok(subscriptionService.getActive());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<SubscriptionResponse>> getByCategory(
            @PathVariable Category category) {
        return ResponseEntity.ok(subscriptionService.getByCategory(category));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<SubscriptionResponse>> getUpcoming() {
        return ResponseEntity.ok(analyticsService.getUpcomingRenewals());
    }

    // ── Analytics ────────────────────────────────────────────

    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(analyticsService.getAnalytics());
    }

    @GetMapping("/analytics/preview-add")
    public ResponseEntity<?> previewAdd(
            @RequestParam java.math.BigDecimal cost,
            @RequestParam com.subscriptions.api.model.BillingCycle billingCycle) {
        return ResponseEntity.ok(analyticsService.previewAdd(cost, billingCycle));
    }

    @GetMapping("/analytics/preview-remove/{id}")
    public ResponseEntity<?> previewRemove(@PathVariable Long id) {
        return ResponseEntity.ok(analyticsService.previewRemove(id));
    }

    // ── Actions ──────────────────────────────────────────────

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SubscriptionResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.cancel(id));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<SubscriptionResponse> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.reactivate(id));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<?> confirm(
            @PathVariable Long id,
            @RequestParam String action) {
        return switch (action.toLowerCase()) {
            case "keep" -> ResponseEntity.ok(subscriptionService.confirmKeep(id));
            case "cancel" -> {
                subscriptionService.confirmCancel(id);
                yield ResponseEntity.ok("Subscription cancelled and removed.");
            }
            default -> ResponseEntity.badRequest()
                    .body("Invalid action. Use 'keep' or 'cancel'.");
        };
    }
}
