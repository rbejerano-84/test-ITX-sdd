package com.example.prices.domain;

public record BrandId(long value) {

  public BrandId {
    if (value <= 0) {
      throw new IllegalArgumentException("Brand ID must be positive");
    }
  }
}
