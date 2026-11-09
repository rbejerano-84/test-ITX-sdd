package com.example.prices.domain;

public record Priority(int value) {

  public Priority {
    if (value < 0) {
      throw new IllegalArgumentException("Priority must not be negative");
    }
  }
}
