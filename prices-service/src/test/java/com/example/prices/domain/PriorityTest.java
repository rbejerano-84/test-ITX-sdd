package com.example.prices.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PriorityTest {

  @Test
  void acceptsZeroAndPositivePriorities() {
    assertEquals(new Priority(0), new Priority(0));
    assertEquals(1, new Priority(1).value());
  }

  @Test
  void rejectsNegativePriorities() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new Priority(-1));

    assertEquals("Priority must not be negative", exception.getMessage());
  }
}
