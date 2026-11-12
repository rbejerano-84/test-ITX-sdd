package com.example.prices.application;

import com.example.prices.domain.Price;
import com.example.prices.domain.PriceNotFoundException;
import com.example.prices.domain.PriceQuery;
import com.example.prices.domain.PriceRepository;
import com.example.prices.domain.PriceSelector;
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
  public Price getApplicablePrice(PriceQuery query) {
    Objects.requireNonNull(query, "Price query is required");
    List<Price> candidates = priceRepository.findApplicablePrices(query);

    return priceSelector
        .selectApplicablePrice(candidates, query.applicationDate())
        .orElseThrow(
            () ->
                new PriceNotFoundException(
                    query.brandId(), query.productId(), query.applicationDate()));
  }
}
