package com.subscriptions.api.plaid;

import com.plaid.client.ApiClient;
import com.plaid.client.request.PlaidApi;
import com.plaid.client.model.*;
import com.subscriptions.api.model.BillingCycle;
import com.subscriptions.api.model.Category;
import com.subscriptions.api.model.Subscription;
import com.subscriptions.api.model.SubscriptionType;
import com.subscriptions.api.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PlaidService {

    @Value("${plaid.client-id}")
    private String clientId;

    @Value("${plaid.secret}")
    private String secret;

    @Value("${plaid.env:sandbox}")
    private String environment;

    private final PlaidItemRepository plaidItemRepository;
    private final SubscriptionRepository subscriptionRepository;

    public PlaidService(PlaidItemRepository plaidItemRepository,
                         SubscriptionRepository subscriptionRepository) {
        this.plaidItemRepository = plaidItemRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    private PlaidApi client() {
        if ("not-configured".equals(clientId) || "not-configured".equals(secret)) {
            throw new IllegalStateException(
                    "Plaid credentials are not configured. Set PLAID_CLIENT_ID and PLAID_SECRET environment variables.");
        }
        Map<String, String> apiKeys = new HashMap<>();
        apiKeys.put("clientId", clientId);
        apiKeys.put("secret", secret);

        ApiClient apiClient = new ApiClient(apiKeys);
        apiClient.setPlaidAdapter(ApiClient.Sandbox);
        return apiClient.createService(PlaidApi.class);
    }

    public String createLinkToken(String userId) {
        try {
            LinkTokenCreateRequestUser user = new LinkTokenCreateRequestUser().clientUserId(userId);

            LinkTokenCreateRequest request = new LinkTokenCreateRequest()
                    .user(user)
                    .clientName("Subscription Tracker")
                    .products(List.of(Products.TRANSACTIONS))
                    .countryCodes(List.of(CountryCode.US))
                    .language("en");

            Response<LinkTokenCreateResponse> response = client().linkTokenCreate(request).execute();

            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("Plaid link token creation failed: "
                        + (response.errorBody() != null ? response.errorBody().string() : "unknown error"));
            }

            return response.body().getLinkToken();
        } catch (Exception e) {
            log.error("Failed to create Plaid link token", e);
            throw new RuntimeException("Failed to create Plaid link token: " + e.getMessage(), e);
        }
    }

    public PlaidItem exchangePublicToken(String publicToken) {
        try {
            ItemPublicTokenExchangeRequest request =
                    new ItemPublicTokenExchangeRequest().publicToken(publicToken);

            Response<ItemPublicTokenExchangeResponse> response =
                    client().itemPublicTokenExchange(request).execute();

            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("Plaid token exchange failed: "
                        + (response.errorBody() != null ? response.errorBody().string() : "unknown error"));
            }

            String accessToken = response.body().getAccessToken();
            String itemId = response.body().getItemId();

            PlaidItem item = PlaidItem.builder()
                    .itemId(itemId)
                    .accessToken(accessToken)
                    .institutionName("Sandbox Institution")
                    .build();

            return plaidItemRepository.save(item);
        } catch (Exception e) {
            log.error("Failed to exchange Plaid public token", e);
            throw new RuntimeException("Failed to exchange Plaid public token: " + e.getMessage(), e);
        }
    }

    public CheckNowResponse checkBankNow() {
        int streamsFound = 0;
        int createdTrial = 0;
        int createdPending = 0;
        int skipped = 0;

        List<PlaidItem> items = plaidItemRepository.findAll();

        for (PlaidItem item : items) {
            try {
                TransactionsRecurringGetRequest request =
                        new TransactionsRecurringGetRequest().accessToken(item.getAccessToken());

                Response<TransactionsRecurringGetResponse> response =
                        client().transactionsRecurringGet(request).execute();

                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("Recurring transactions fetch failed for item {}: {}",
                            item.getItemId(),
                            response.errorBody() != null ? response.errorBody().string() : "unknown error");
                    continue;
                }

                List<TransactionStream> outflows = response.body().getOutflowStreams();
                streamsFound += outflows.size();

                for (TransactionStream stream : outflows) {
                    String streamId = stream.getStreamId();

                    if (subscriptionRepository.findByPlaidStreamId(streamId).isPresent()) {
                        skipped++;
                        continue;
                    }

                    BigDecimal amount = BigDecimal.valueOf(
                            stream.getAverageAmount() != null ? stream.getAverageAmount().getAmount() : 0.0
                    ).abs();

                    boolean isZeroCharge = amount.compareTo(BigDecimal.ZERO) == 0;

                    Subscription subscription = Subscription.builder()
                            .name(stream.getMerchantName() != null
                                    ? stream.getMerchantName() : stream.getDescription())
                            .cost(amount)
                            .billingCycle(mapFrequency(stream.getFrequency()))
                            .category(Category.OTHER)
                            .subscriptionType(isZeroCharge ? SubscriptionType.TRIAL : SubscriptionType.PENDING_REVIEW)
                            .startDate(LocalDate.now())
                            .nextRenewalDate(LocalDate.now().plusMonths(1))
                            .reminderDaysBefore(7)
                            .isActive(true)
                            .plaidStreamId(streamId)
                            .build();

                    subscriptionRepository.save(subscription);

                    if (isZeroCharge) {
                        createdTrial++;
                    } else {
                        createdPending++;
                    }
                }
            } catch (Exception e) {
                log.error("Error checking bank item {}: {}", item.getItemId(), e.getMessage(), e);
            }
        }

        return new CheckNowResponse(streamsFound, createdTrial, createdPending, skipped);
    }

    private BillingCycle mapFrequency(RecurringTransactionFrequency frequency) {
        if (frequency == null) return BillingCycle.MONTHLY;
        return switch (frequency) {
            case WEEKLY, BIWEEKLY -> BillingCycle.WEEKLY;
            case ANNUALLY -> BillingCycle.YEARLY;
            default -> BillingCycle.MONTHLY;
        };
    }
}
