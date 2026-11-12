package com.example.prices.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class PriceNotFoundException extends RuntimeException {

  public PriceNotFoundException(
      BrandId brandId, ProductId productId, LocalDateTime applicationDate) {
    super(
        "No applicable price for brand "
            + Objects.requireNonNull(brandId, "Brand ID is required").value()
            + ", product "
            + Objects.requireNonNull(productId, "Product ID is required").value()
            + " at "
            + Objects.requireNonNull(applicationDate, "Application date is required"));
  }
}
