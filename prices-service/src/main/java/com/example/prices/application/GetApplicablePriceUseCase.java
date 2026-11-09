package com.example.prices.application;

import com.example.prices.domain.BrandId;
import com.example.prices.domain.Price;
import com.example.prices.domain.ProductId;
import java.time.LocalDateTime;

public interface GetApplicablePriceUseCase {

  Price getApplicablePrice(BrandId brandId, ProductId productId, LocalDateTime applicationDate);
}
