package com.subscriptions.api.plaid;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "plaid_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaidItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String itemId;

    /** Plaid access token — sensitive, never expose via any API response */
    @Column(nullable = false)
    private String accessToken;

    private String institutionName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
