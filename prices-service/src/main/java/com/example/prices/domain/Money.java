package com.example.prices.domain;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {

  public Money {
    Objects.requireNonNull(amount, "Amount is required");
    Objects.requireNonNull(currency, "Currency is required");
    if (amount.scale() != 2) {
      throw new IllegalArgumentException("Amount must have exactly two decimal places");
    }
  }
}
