package com.example.prices.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record Price(
        BrandId brandId,
        DateRange validity,
        PriceListId priceList,
        ProductId productId,
        int priority,
        Money money) {

    public Price {
        Objects.requireNonNull(brandId, "Brand ID is required");
        Objects.requireNonNull(validity, "Validity period is required");
        Objects.requireNonNull(priceList, "Price list is required");
        Objects.requireNonNull(productId, "Product ID is required");
        Objects.requireNonNull(money, "Money is required");
    }

    public boolean isApplicableAt(LocalDateTime applicationDate) {
        return validity.contains(applicationDate);
    }
}
