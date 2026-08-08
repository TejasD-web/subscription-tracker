package com.subscriptions.api.plaid;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaidItemRepository extends JpaRepository<PlaidItem, Long> {
}
