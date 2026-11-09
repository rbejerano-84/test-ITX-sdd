package com.example.prices.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PriceSelectorTest {

  private final PriceSelector selector = new PriceSelector();
  private final LocalDateTime queryDate = LocalDateTime.parse("2020-06-14T10:00:00");

  @Test
  void selectsThePriceWithTheHighestPriority() {
    Price lowerPriority = price(1, 9, queryDate.minusDays(1), queryDate.plusDays(1));
    Price higherPriority = price(2, 2, queryDate.minusDays(1), queryDate.plusDays(1));

    assertSame(
        higherPriority,
        selector
            .selectApplicablePrice(List.of(higherPriority, lowerPriority), queryDate)
            .orElseThrow());
  }

  @Test
  void ignoresHigherPriorityPricesThatAreNotApplicableAtTheDate() {
    Price applicablePrice = price(1, 9, queryDate.minusDays(1), queryDate.plusDays(1));
    Price notApplicablePrice = price(5, 2, queryDate.minusDays(3), queryDate.minusDays(2));

    assertSame(
        applicablePrice,
        selector
            .selectApplicablePrice(List.of(notApplicablePrice, applicablePrice), queryDate)
            .orElseThrow());
  }

  @Test
  void selectsTheOnlyCandidate() {
    Price onlyPrice = price(3, 1, queryDate.minusDays(1), queryDate.plusDays(1));

    assertSame(
        onlyPrice, selector.selectApplicablePrice(List.of(onlyPrice), queryDate).orElseThrow());
  }

  @Test
  void returnsEmptyWhenThereAreNoCandidates() {
    Optional<Price> selected = selector.selectApplicablePrice(List.of(), queryDate);

    assertFalse(selected.isPresent());
  }

  @Test
  void considersStartAndEndDatesInclusive() {
    LocalDateTime start = queryDate.minusDays(1);
    LocalDateTime end = queryDate.plusDays(1);
    Price boundedPrice = price(1, 1, start, end);

    assertSame(
        boundedPrice, selector.selectApplicablePrice(List.of(boundedPrice), start).orElseThrow());
    assertSame(
        boundedPrice, selector.selectApplicablePrice(List.of(boundedPrice), end).orElseThrow());
    assertFalse(
        selector.selectApplicablePrice(List.of(boundedPrice), start.minusNanos(1)).isPresent());
    assertFalse(
        selector.selectApplicablePrice(List.of(boundedPrice), end.plusNanos(1)).isPresent());
  }

  @Test
  void breaksPriorityTiesByTheGreaterPriceListId() {
    Price lowerPriceList = price(4, 10, queryDate.minusDays(1), queryDate.plusDays(1));
    Price higherPriceList = price(4, 11, queryDate.minusDays(1), queryDate.plusDays(1));

    assertSame(
        higherPriceList,
        selector
            .selectApplicablePrice(List.of(lowerPriceList, higherPriceList), queryDate)
            .orElseThrow());
  }

  private Price price(int priority, long priceListId, LocalDateTime start, LocalDateTime end) {
    return new Price(
        new BrandId(1),
        new DateRange(start, end),
        new PriceListId(priceListId),
        new ProductId(35455),
        new Priority(priority),
        new Money(new BigDecimal("35.50"), Currency.getInstance("EUR")));
  }
}
