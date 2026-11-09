package com.example.prices.infrastructure.adapter.out.persistence;

import com.example.prices.domain.BrandId;
import com.example.prices.domain.DateRange;
import com.example.prices.domain.Money;
import com.example.prices.domain.Price;
import com.example.prices.domain.PriceListId;
import com.example.prices.domain.ProductId;
import java.util.Currency;
import org.springframework.stereotype.Component;

@Component
public class PriceEntityMapper {

  public Price toDomain(PriceEntity entity) {
    return new Price(
        new BrandId(entity.getBrandId()),
        new DateRange(entity.getStartDate(), entity.getEndDate()),
        new PriceListId(entity.getPriceList()),
        new ProductId(entity.getProductId()),
        entity.getPriority(),
        new Money(entity.getPrice(), Currency.getInstance(entity.getCurrency().trim())));
  }
}
