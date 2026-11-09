package com.example.prices.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class DomainValueObjectsTest {

    @Test
    void identifiersMustBePositive() {
        assertDoesNotThrow(() -> new BrandId(1));
        assertDoesNotThrow(() -> new ProductId(1));
        assertDoesNotThrow(() -> new PriceListId(1));

        assertThrows(IllegalArgumentException.class, () -> new BrandId(0));
        assertThrows(IllegalArgumentException.class, () -> new ProductId(-1));
        assertThrows(IllegalArgumentException.class, () -> new PriceListId(0));
    }

    @Test
    void moneyRequiresTwoDecimalPlacesAndACurrency() {
        assertDoesNotThrow(
                () -> new Money(new BigDecimal("35.50"), Currency.getInstance("EUR")));

        assertThrows(
                IllegalArgumentException.class,
                () -> new Money(new BigDecimal("35.5"), Currency.getInstance("EUR")));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Money(new BigDecimal("35.500"), Currency.getInstance("EUR")));
        assertThrows(
                NullPointerException.class, () -> new Money(new BigDecimal("35.50"), null));
    }

    @Test
    void dateRangeRequiresStartNotAfterEnd() {
        LocalDateTime start = LocalDateTime.parse("2020-06-14T00:00:00");
        LocalDateTime end = LocalDateTime.parse("2020-06-15T00:00:00");

        assertDoesNotThrow(() -> new DateRange(start, end));
        assertDoesNotThrow(() -> new DateRange(start, start));
        assertThrows(IllegalArgumentException.class, () -> new DateRange(end, start));
    }

    @Test
    void dateRangeContainsBothEndpoints() {
        LocalDateTime start = LocalDateTime.parse("2020-06-14T00:00:00");
        LocalDateTime end = LocalDateTime.parse("2020-06-15T00:00:00");
        DateRange range = new DateRange(start, end);

        assertTrue(range.contains(start));
        assertTrue(range.contains(end));
        assertTrue(range.contains(start.plusHours(12)));
        assertFalse(range.contains(start.minusNanos(1)));
        assertFalse(range.contains(end.plusNanos(1)));
    }
}
