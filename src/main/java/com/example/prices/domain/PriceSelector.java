package com.example.prices.domain;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

public class PriceSelector {

  private static final Comparator<Price> SELECTION_ORDER =
      Comparator.comparingInt((Price price) -> price.priority().value())
          .thenComparingLong(price -> price.priceList().value());

  public Optional<Price> selectApplicablePrice(
      Collection<Price> candidates, LocalDateTime applicationDate) {
    Objects.requireNonNull(candidates, "Price candidates are required");
    Objects.requireNonNull(applicationDate, "Application date is required");

    return candidates.stream()
        .filter(price -> price.isApplicableAt(applicationDate))
        .max(SELECTION_ORDER);
  }
}
