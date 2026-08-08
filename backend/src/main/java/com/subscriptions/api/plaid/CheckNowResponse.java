package com.subscriptions.api.plaid;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckNowResponse {
    private int streamsFound;
    private int createdAsTrial;
    private int createdAsPendingReview;
    private int skippedExisting;
}
