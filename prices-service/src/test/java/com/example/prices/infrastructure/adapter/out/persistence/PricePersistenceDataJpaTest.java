package com.example.prices.infrastructure.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.prices.application.PriceRepository;
import com.example.prices.domain.BrandId;
import com.example.prices.domain.DateRange;
import com.example.prices.domain.Money;
import com.example.prices.domain.Price;
import com.example.prices.domain.PriceListId;
import com.example.prices.domain.ProductId;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@Import({PricePersistenceAdapter.class, PriceEntityMapper.class})
class PricePersistenceDataJpaTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private javax.sql.DataSource dataSource;

  @Autowired private PriceRepository priceRepository;

  @Test
  void loadsReferencePricesAndCreatesTheRequiredSchema() throws SQLException {
    assertEquals(4, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM prices", Integer.class));

    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metadata = connection.getMetaData();
      Map<String, ColumnDefinition> columns = readColumns(metadata);

      assertEquals(
          Set.of(
              "ID",
              "BRAND_ID",
              "START_DATE",
              "END_DATE",
              "PRICE_LIST",
              "PRODUCT_ID",
              "PRIORITY",
              "PRICE",
              "CURRENCY"),
          columns.keySet());
      assertEquals("DECIMAL", columns.get("PRICE").typeName());
      assertEquals(2, columns.get("PRICE").decimalDigits());
      assertEquals("CHARACTER", columns.get("CURRENCY").typeName());
      assertEquals(3, columns.get("CURRENCY").size());
      assertEquals("TIMESTAMP", columns.get("START_DATE").typeName());
      assertEquals("TIMESTAMP", columns.get("END_DATE").typeName());
      assertEquals(Set.of("ID"), readPrimaryKeys(metadata));
      assertTrue(
          readIndexes(metadata)
              .contains(List.of("BRAND_ID", "PRODUCT_ID", "START_DATE", "END_DATE")));
    }
  }

  @Test
  void returnsOnlyApplicableCandidatesForTheFiveReferenceScenarios() {
    assertCandidates("2020-06-14T10:00:00", 1L);
    assertCandidates("2020-06-14T16:00:00", 1L, 2L);
    assertCandidates("2020-06-14T21:00:00", 1L);
    assertCandidates("2020-06-15T10:00:00", 1L, 3L);
    assertCandidates("2020-06-16T21:00:00", 1L, 4L);
  }

  @Test
  void mapsEverySeededPriceToTheDomainModel() {
    assertDomainPrice(
        "2020-06-14T10:00:00", 1L, "35.50", 0, "2020-06-14T00:00:00", "2020-12-31T23:59:59");
    assertDomainPrice(
        "2020-06-14T16:00:00", 2L, "25.45", 1, "2020-06-14T15:00:00", "2020-06-14T18:30:00");
    assertDomainPrice(
        "2020-06-15T10:00:00", 3L, "30.50", 1, "2020-06-15T00:00:00", "2020-06-15T11:00:00");
    assertDomainPrice(
        "2020-06-16T21:00:00", 4L, "38.95", 1, "2020-06-15T16:00:00", "2020-12-31T23:59:59");
  }

  @Test
  void includesCandidatesAtEveryExactValidityBoundary() {
    assertCandidates("2020-06-14T00:00:00", 1L);
    assertCandidates("2020-12-31T23:59:59", 1L, 4L);
    assertCandidates("2020-06-14T15:00:00", 1L, 2L);
    assertCandidates("2020-06-14T18:30:00", 1L, 2L);
    assertCandidates("2020-06-15T00:00:00", 1L, 3L);
    assertCandidates("2020-06-15T11:00:00", 1L, 3L);
    assertCandidates("2020-06-15T16:00:00", 1L, 4L);
  }

  @Test
  void filtersCandidatesByBrandAndProduct() {
    assertEquals(
        List.of(),
        priceRepository.findApplicablePrices(
            new BrandId(2), new ProductId(35455), LocalDateTime.parse("2020-06-14T10:00:00")));
    assertEquals(
        List.of(),
        priceRepository.findApplicablePrices(
            new BrandId(1), new ProductId(99999), LocalDateTime.parse("2020-06-14T10:00:00")));
  }

  private void assertCandidates(String applicationDate, Long... expectedPriceLists) {
    List<Long> actualPriceLists =
        priceRepository
            .findApplicablePrices(
                new BrandId(1), new ProductId(35455), LocalDateTime.parse(applicationDate))
            .stream()
            .map(Price::priceList)
            .map(priceList -> priceList.value())
            .sorted()
            .toList();
    assertEquals(List.of(expectedPriceLists), actualPriceLists);
  }

  private void assertDomainPrice(
      String applicationDate,
      long expectedPriceList,
      String expectedAmount,
      int expectedPriority,
      String expectedStart,
      String expectedEnd) {
    Price price =
        priceRepository
            .findApplicablePrices(
                new BrandId(1), new ProductId(35455), LocalDateTime.parse(applicationDate))
            .stream()
            .filter(candidate -> candidate.priceList().value() == expectedPriceList)
            .findFirst()
            .orElseThrow();

    assertEquals(new BrandId(1), price.brandId());
    assertEquals(
        new DateRange(LocalDateTime.parse(expectedStart), LocalDateTime.parse(expectedEnd)),
        price.validityPeriod());
    assertEquals(new PriceListId(expectedPriceList), price.priceList());
    assertEquals(new ProductId(35455), price.productId());
    assertEquals(expectedPriority, price.priority());
    assertEquals(
        new Money(new BigDecimal(expectedAmount), Currency.getInstance("EUR")), price.money());
  }

  private Map<String, ColumnDefinition> readColumns(DatabaseMetaData metadata) throws SQLException {
    Map<String, ColumnDefinition> columns = new HashMap<>();
    try (ResultSet resultSet = metadata.getColumns(null, null, "PRICES", null)) {
      while (resultSet.next()) {
        columns.put(
            resultSet.getString("COLUMN_NAME"),
            new ColumnDefinition(
                resultSet.getString("TYPE_NAME"),
                resultSet.getInt("COLUMN_SIZE"),
                resultSet.getInt("DECIMAL_DIGITS")));
      }
    }
    return columns;
  }

  private Set<String> readPrimaryKeys(DatabaseMetaData metadata) throws SQLException {
    Set<String> primaryKeys = new HashSet<>();
    try (ResultSet resultSet = metadata.getPrimaryKeys(null, null, "PRICES")) {
      while (resultSet.next()) {
        primaryKeys.add(resultSet.getString("COLUMN_NAME"));
      }
    }
    return primaryKeys;
  }

  private Set<List<String>> readIndexes(DatabaseMetaData metadata) throws SQLException {
    Map<String, Map<Short, String>> indexes = new HashMap<>();
    try (ResultSet resultSet = metadata.getIndexInfo(null, null, "PRICES", false, false)) {
      while (resultSet.next()) {
        String indexName = resultSet.getString("INDEX_NAME");
        String columnName = resultSet.getString("COLUMN_NAME");
        if (indexName != null && columnName != null) {
          indexes
              .computeIfAbsent(indexName, ignored -> new TreeMap<>())
              .put(resultSet.getShort("ORDINAL_POSITION"), columnName);
        }
      }
    }
    Set<List<String>> columnsByIndex = new HashSet<>();
    for (Map<Short, String> columns : indexes.values()) {
      columnsByIndex.add(new ArrayList<>(columns.values()));
    }
    return columnsByIndex;
  }

  private record ColumnDefinition(String typeName, int size, int decimalDigits) {}
}
