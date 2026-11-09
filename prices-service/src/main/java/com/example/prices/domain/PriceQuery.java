package com.example.prices.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record PriceQuery(BrandId brandId, ProductId productId, LocalDateTime applicationDate) {

  public PriceQuery {
    Objects.requireNonNull(brandId, "Brand ID is required");
    Objects.requireNonNull(productId, "Product ID is required");
    Objects.requireNonNull(applicationDate, "Application date is required");
  }
}
