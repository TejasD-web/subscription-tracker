package com.subscriptions.api.plaid;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExchangeTokenRequest {

    @NotBlank(message = "publicToken is required")
    private String publicToken;
}
