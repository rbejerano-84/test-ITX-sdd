package com.example.prices.domain;

import java.time.LocalDateTime;
import java.util.List;

public interface PriceRepository {

  List<Price> findApplicablePrices(
      BrandId brandId, ProductId productId, LocalDateTime applicationDate);
}
