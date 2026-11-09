package com.example.prices.application;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.prices.domain.BrandId;
import com.example.prices.domain.DateRange;
import com.example.prices.domain.Money;
import com.example.prices.domain.Price;
import com.example.prices.domain.PriceListId;
import com.example.prices.domain.PriceSelector;
import com.example.prices.domain.ProductId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetApplicablePriceServiceTest {

  private final PriceRepository priceRepository = mock(PriceRepository.class);
  private final PriceSelector priceSelector = mock(PriceSelector.class);
  private final GetApplicablePriceService service =
      new GetApplicablePriceService(priceRepository, priceSelector);

  private final BrandId brandId = new BrandId(1);
  private final ProductId productId = new ProductId(35455);
  private final LocalDateTime applicationDate = LocalDateTime.parse("2020-06-14T10:00:00");

  @Test
  void returnsThePriceSelectedByTheDomainSelector() {
    Price candidate = price(1, 1);
    Price winningPrice = price(2, 2);
    List<Price> candidates = List.of(candidate, winningPrice);
    when(priceRepository.findApplicablePrices(brandId, productId, applicationDate))
        .thenReturn(candidates);
    when(priceSelector.selectApplicablePrice(candidates, applicationDate))
        .thenReturn(Optional.of(winningPrice));

    Price result = service.getApplicablePrice(brandId, productId, applicationDate);

    assertSame(winningPrice, result);
    verify(priceRepository).findApplicablePrices(brandId, productId, applicationDate);
    verify(priceSelector).selectApplicablePrice(candidates, applicationDate);
  }

  @Test
  void throwsPriceNotFoundWhenThereAreNoCandidates() {
    when(priceRepository.findApplicablePrices(brandId, productId, applicationDate))
        .thenReturn(List.of());
    when(priceSelector.selectApplicablePrice(List.of(), applicationDate))
        .thenReturn(Optional.empty());

    assertThrows(
        PriceNotFoundException.class,
        () -> service.getApplicablePrice(brandId, productId, applicationDate));

    verify(priceSelector).selectApplicablePrice(List.of(), applicationDate);
  }

  private Price price(int priority, long priceListId) {
    return new Price(
        brandId,
        new DateRange(applicationDate.minusDays(1), applicationDate.plusDays(1)),
        new PriceListId(priceListId),
        productId,
        priority,
        new Money(new BigDecimal("35.50"), Currency.getInstance("EUR")));
  }
}
