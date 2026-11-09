package com.example.prices.application;

import com.example.prices.domain.BrandId;
import com.example.prices.domain.Price;
import com.example.prices.domain.ProductId;
import java.time.LocalDateTime;
import java.util.List;

public interface PriceRepository {

  List<Price> findApplicablePrices(
      BrandId brandId, ProductId productId, LocalDateTime applicationDate);
}
