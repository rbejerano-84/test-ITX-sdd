package com.example.prices.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.example.prices.domain.PriceNotFoundException;
import com.example.prices.domain.PriceQuery;
import com.example.prices.domain.PriceRepository;
import com.example.prices.domain.PriceSelector;
import com.example.prices.domain.Priority;
import com.example.prices.domain.ProductId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetApplicablePriceServiceTest {

  private final PriceRepository priceRepository = mock(PriceRepository.class);
  private final PriceSelector priceSelector = new PriceSelector();
  private final GetApplicablePriceService service =
      new GetApplicablePriceService(priceRepository, priceSelector);

  private final BrandId brandId = new BrandId(1);
  private final ProductId productId = new ProductId(35455);
  private final LocalDateTime applicationDate = LocalDateTime.parse("2020-06-14T10:00:00");
  private final PriceQuery query = new PriceQuery(brandId, productId, applicationDate);

  @Test
  void returnsThePriceSelectedByTheDomainSelector() {
    Price winningPrice = price(2, 2);
    Price candidate = price(1, 9);
    List<Price> candidates = List.of(winningPrice, candidate);
    when(priceRepository.findApplicablePrices(query)).thenReturn(candidates);

    Price result = service.getApplicablePrice(query);

    assertSame(winningPrice, result);
    verify(priceRepository).findApplicablePrices(query);
  }

  @Test
  void throwsPriceNotFoundWhenThereAreNoCandidates() {
    when(priceRepository.findApplicablePrices(query)).thenReturn(List.of());

    PriceNotFoundException exception =
        assertThrows(PriceNotFoundException.class, () -> service.getApplicablePrice(query));

    assertEquals(
        "No applicable price for brand 1, product 35455 at 2020-06-14T10:00",
        exception.getMessage());
    verify(priceRepository).findApplicablePrices(query);
  }

  private Price price(int priority, long priceListId) {
    return new Price(
        brandId,
        new DateRange(applicationDate.minusDays(1), applicationDate.plusDays(1)),
        new PriceListId(priceListId),
        productId,
        new Priority(priority),
        new Money(new BigDecimal("35.50"), Currency.getInstance("EUR")));
  }
}
