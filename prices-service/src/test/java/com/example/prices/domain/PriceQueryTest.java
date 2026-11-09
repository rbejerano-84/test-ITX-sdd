package com.example.prices.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PriceQueryTest {

  private final BrandId brandId = new BrandId(1);
  private final ProductId productId = new ProductId(35455);
  private final LocalDateTime applicationDate = LocalDateTime.parse("2020-06-14T10:00:00");

  @Test
  void storesTheQueryParameters() {
    PriceQuery query = new PriceQuery(brandId, productId, applicationDate);

    assertEquals(brandId, query.brandId());
    assertEquals(productId, query.productId());
    assertEquals(applicationDate, query.applicationDate());
  }

  @Test
  void requiresABrandId() {
    assertThrows(
        NullPointerException.class, () -> new PriceQuery(null, productId, applicationDate));
  }

  @Test
  void requiresAProductId() {
    assertThrows(NullPointerException.class, () -> new PriceQuery(brandId, null, applicationDate));
  }

  @Test
  void requiresAnApplicationDate() {
    assertThrows(NullPointerException.class, () -> new PriceQuery(brandId, productId, null));
  }
}
