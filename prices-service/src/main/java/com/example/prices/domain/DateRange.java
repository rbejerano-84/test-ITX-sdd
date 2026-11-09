package com.example.prices.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public record DateRange(LocalDateTime start, LocalDateTime end) {

    public DateRange {
        Objects.requireNonNull(start, "Start date is required");
        Objects.requireNonNull(end, "End date is required");
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }
    }

    public boolean contains(LocalDateTime date) {
        Objects.requireNonNull(date, "Date is required");
        return !date.isBefore(start) && !date.isAfter(end);
    }
}
