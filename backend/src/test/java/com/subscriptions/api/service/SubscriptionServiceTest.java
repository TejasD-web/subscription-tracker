package com.subscriptions.api.service;

import com.subscriptions.api.dto.SubscriptionRequest;
import com.subscriptions.api.dto.SubscriptionResponse;
import com.subscriptions.api.exception.ResourceNotFoundException;
import com.subscriptions.api.model.*;
import com.subscriptions.api.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository repository;

    @InjectMocks
    private SubscriptionService service;

    private Subscription netflix;
    private SubscriptionRequest request;

    @BeforeEach
    void setUp() {
        netflix = Subscription.builder()
                .id(1L)
                .name("Netflix")
                .cost(new BigDecimal("15.99"))
                .billingCycle(BillingCycle.MONTHLY)
                .category(Category.STREAMING)
                .subscriptionType(SubscriptionType.ACTIVE)
                .startDate(LocalDate.now().minusMonths(1))
                .nextRenewalDate(LocalDate.now().plusDays(10))
                .reminderDaysBefore(7)
                .isActive(true)
                .build();

        request = new SubscriptionRequest();
        request.setName("Netflix");
        request.setCost(new BigDecimal("15.99"));
        request.setBillingCycle(BillingCycle.MONTHLY);
        request.setCategory(Category.STREAMING);
        request.setSubscriptionType(SubscriptionType.ACTIVE);
        request.setStartDate(LocalDate.now().minusMonths(1));
        request.setNextRenewalDate(LocalDate.now().plusDays(10));
    }

    @Test
    void create_savesAndReturnsResponse() {
        when(repository.save(any(Subscription.class))).thenReturn(netflix);
        SubscriptionResponse response = service.create(request);
        assertThat(response.getName()).isEqualTo("Netflix");
        assertThat(response.getCost()).isEqualByComparingTo("15.99");
        verify(repository, times(1)).save(any(Subscription.class));
    }

    @Test
    void getAll_returnsAllSubscriptions() {
        when(repository.findAll()).thenReturn(List.of(netflix));
        List<SubscriptionResponse> result = service.getAll();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Netflix");
    }

    @Test
    void getById_existingId_returnsResponse() {
        when(repository.findById(1L)).thenReturn(Optional.of(netflix));
        SubscriptionResponse response = service.getById(1L);
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getById_missingId_throwsNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void update_existingId_updatesAndReturns() {
        when(repository.findById(1L)).thenReturn(Optional.of(netflix));
        when(repository.save(any(Subscription.class))).thenReturn(netflix);
        SubscriptionResponse response = service.update(1L, request);
        assertThat(response.getName()).isEqualTo("Netflix");
        verify(repository).save(any(Subscription.class));
    }

    @Test
    void delete_existingId_deletesSuccessfully() {
        when(repository.findById(1L)).thenReturn(Optional.of(netflix));
        service.delete(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void delete_missingId_throwsNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancel_setsIsActiveFalse() {
        when(repository.findById(1L)).thenReturn(Optional.of(netflix));
        when(repository.save(any(Subscription.class))).thenReturn(netflix);
        service.cancel(1L);
        verify(repository).save(argThat(s -> !s.getIsActive()));
    }

    @Test
    void confirmKeep_convertsToActive() {
        netflix.setSubscriptionType(SubscriptionType.TRIAL);
        netflix.setTrialEndDate(LocalDate.now().plusDays(3));
        when(repository.findById(1L)).thenReturn(Optional.of(netflix));
        when(repository.save(any(Subscription.class))).thenReturn(netflix);
        service.confirmKeep(1L);
        verify(repository).save(argThat(s ->
                s.getSubscriptionType() == SubscriptionType.ACTIVE
                && s.getTrialEndDate() == null));
    }

    @Test
    void confirmCancel_deletesSubscription() {
        when(repository.findById(1L)).thenReturn(Optional.of(netflix));
        service.confirmCancel(1L);
        verify(repository).deleteById(1L);
    }
}
