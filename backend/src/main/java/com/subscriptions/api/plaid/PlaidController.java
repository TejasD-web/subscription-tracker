package com.subscriptions.api.plaid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/plaid")
@RequiredArgsConstructor
public class PlaidController {

    private final PlaidService plaidService;

    @PostMapping("/link-token")
    public ResponseEntity<LinkTokenResponse> createLinkToken() {
        String linkToken = plaidService.createLinkToken("subscription-tracker-user");
        return ResponseEntity.ok(new LinkTokenResponse(linkToken));
    }

    @PostMapping("/exchange-token")
    public ResponseEntity<ExchangeTokenResponse> exchangeToken(
            @Valid @RequestBody ExchangeTokenRequest request) {
        PlaidItem item = plaidService.exchangePublicToken(request.getPublicToken());
        return ResponseEntity.ok(new ExchangeTokenResponse(item.getId(), item.getItemId()));
    }

    @PostMapping("/check-now")
    public ResponseEntity<CheckNowResponse> checkNow() {
        return ResponseEntity.ok(plaidService.checkBankNow());
    }
}
