package com.example.prices.application;

public class PriceNotFoundException extends RuntimeException {

  public PriceNotFoundException() {
    super("No applicable price found");
  }
}
