package com.subscriptions.api.plaid;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExchangeTokenResponse {
    private Long plaidItemId;
    private String itemId;
}
