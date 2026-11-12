package com.example.prices.domain;

public record PriceListId(long value) {

  public PriceListId {
    if (value <= 0) {
      throw new IllegalArgumentException("Price list ID must be positive");
    }
  }
}
