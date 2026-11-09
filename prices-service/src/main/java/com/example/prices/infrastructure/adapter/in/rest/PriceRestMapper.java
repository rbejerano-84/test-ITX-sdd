package com.example.prices.infrastructure.adapter.in.rest;

import com.example.prices.domain.Price;
import com.example.prices.infrastructure.adapter.in.rest.generated.model.PriceResponse;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PriceRestMapper {

  public PriceResponse toResponse(Price price) {
    Objects.requireNonNull(price, "Price is required");
    return new PriceResponse(
        price.productId().value(),
        price.brandId().value(),
        price.priceList().value(),
        price.validityPeriod().start(),
        price.validityPeriod().end(),
        price.money().amount(),
        price.money().currency().getCurrencyCode());
  }
}
