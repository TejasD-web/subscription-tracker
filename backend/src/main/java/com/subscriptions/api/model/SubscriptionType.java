package com.subscriptions.api.model;

public enum SubscriptionType {

    /** Fully subscribed — being charged normally */
    ACTIVE,

    /** Free trial — not being charged yet */
    TRIAL,

    /** Paying but undecided — will be prompted at period end */
    TRYING_OUT,

    /** Detected via Plaid as a non-zero recurring charge, awaiting user confirmation */
    PENDING_REVIEW
}
