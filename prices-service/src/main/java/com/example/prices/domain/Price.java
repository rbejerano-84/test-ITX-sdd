package com.example.prices.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record Price(
    BrandId brandId,
    DateRange validityPeriod,
    PriceListId priceList,
    ProductId productId,
    Priority priority,
    Money money) {

  public Price {
    Objects.requireNonNull(brandId, "Brand ID is required");
    Objects.requireNonNull(validityPeriod, "Validity period is required");
    Objects.requireNonNull(priceList, "Price list is required");
    Objects.requireNonNull(productId, "Product ID is required");
    Objects.requireNonNull(priority, "Priority is required");
    Objects.requireNonNull(money, "Money is required");
  }

  public boolean isApplicableAt(LocalDateTime applicationDate) {
    return validityPeriod.contains(applicationDate);
  }
}
