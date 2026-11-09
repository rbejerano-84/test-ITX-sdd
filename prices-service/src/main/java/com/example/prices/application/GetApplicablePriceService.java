package com.example.prices.application;

import com.example.prices.domain.BrandId;
import com.example.prices.domain.Price;
import com.example.prices.domain.PriceSelector;
import com.example.prices.domain.ProductId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class GetApplicablePriceService implements GetApplicablePriceUseCase {

  private final PriceRepository priceRepository;
  private final PriceSelector priceSelector;

  public GetApplicablePriceService(PriceRepository priceRepository, PriceSelector priceSelector) {
    this.priceRepository = Objects.requireNonNull(priceRepository, "Price repository is required");
    this.priceSelector = Objects.requireNonNull(priceSelector, "Price selector is required");
  }

  @Override
  public Price getApplicablePrice(
      BrandId brandId, ProductId productId, LocalDateTime applicationDate) {
    List<Price> candidates =
        priceRepository.findApplicablePrices(brandId, productId, applicationDate);

    return priceSelector
        .selectApplicablePrice(candidates, applicationDate)
        .orElseThrow(() -> new PriceNotFoundException(brandId, productId, applicationDate));
  }
}
