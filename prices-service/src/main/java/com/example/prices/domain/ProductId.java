package com.example.prices.domain;

public record ProductId(long value) {

  public ProductId {
    if (value <= 0) {
      throw new IllegalArgumentException("Product ID must be positive");
    }
  }
}
