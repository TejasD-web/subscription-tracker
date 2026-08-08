package com.subscriptions.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.subscriptions.api.dto.SubscriptionRequest;
import com.subscriptions.api.dto.SubscriptionResponse;
import com.subscriptions.api.model.BillingCycle;
import com.subscriptions.api.model.Category;
import com.subscriptions.api.model.SubscriptionType;
import com.subscriptions.api.service.AnalyticsService;
import com.subscriptions.api.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscriptionService subscriptionService;

    @MockBean
    private AnalyticsService analyticsService;

    private ObjectMapper objectMapper;
    private SubscriptionResponse netflixResponse;
    private SubscriptionRequest netflixRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        netflixResponse = SubscriptionResponse.builder()
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

        netflixRequest = new SubscriptionRequest();
        netflixRequest.setName("Netflix");
        netflixRequest.setCost(new BigDecimal("15.99"));
        netflixRequest.setBillingCycle(BillingCycle.MONTHLY);
        netflixRequest.setCategory(Category.STREAMING);
        netflixRequest.setSubscriptionType(SubscriptionType.ACTIVE);
        netflixRequest.setStartDate(LocalDate.now().minusMonths(1));
        netflixRequest.setNextRenewalDate(LocalDate.now().plusDays(10));
    }

    @Test
    void POST_subscriptions_returns201WithBody() throws Exception {
        when(subscriptionService.create(any())).thenReturn(netflixResponse);

        mockMvc.perform(post("/api/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(netflixRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Netflix"))
                .andExpect(jsonPath("$.cost").value(15.99));
    }

    @Test
    void GET_subscriptions_returns200WithList() throws Exception {
        when(subscriptionService.getAll()).thenReturn(List.of(netflixResponse));

        mockMvc.perform(get("/api/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Netflix"));
    }

    @Test
    void GET_subscriptionById_returns200() throws Exception {
        when(subscriptionService.getById(1L)).thenReturn(netflixResponse);

        mockMvc.perform(get("/api/subscriptions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void PUT_subscription_returns200WithUpdated() throws Exception {
        when(subscriptionService.update(eq(1L), any())).thenReturn(netflixResponse);

        mockMvc.perform(put("/api/subscriptions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(netflixRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Netflix"));
    }

    @Test
    void DELETE_subscription_returns204() throws Exception {
        mockMvc.perform(delete("/api/subscriptions/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void POST_subscriptions_missingName_returns400() throws Exception {
        netflixRequest.setName(null);

        mockMvc.perform(post("/api/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(netflixRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_activeSubscriptions_returns200() throws Exception {
        when(subscriptionService.getActive()).thenReturn(List.of(netflixResponse));

        mockMvc.perform(get("/api/subscriptions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isActive").value(true));
    }
}
