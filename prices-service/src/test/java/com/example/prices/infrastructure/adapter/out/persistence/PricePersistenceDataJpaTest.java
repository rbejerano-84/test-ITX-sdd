package com.example.prices.infrastructure.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class PricePersistenceDataJpaTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private javax.sql.DataSource dataSource;

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
      assertEquals("CHAR", columns.get("CURRENCY").typeName());
      assertEquals(3, columns.get("CURRENCY").size());
      assertEquals("TIMESTAMP", columns.get("START_DATE").typeName());
      assertEquals("TIMESTAMP", columns.get("END_DATE").typeName());
      assertEquals(Set.of("ID"), readPrimaryKeys(metadata));
      assertTrue(
          readIndexes(metadata)
              .contains(Set.of("BRAND_ID", "PRODUCT_ID", "START_DATE", "END_DATE")));
    }
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

  private Set<Set<String>> readIndexes(DatabaseMetaData metadata) throws SQLException {
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
    Set<Set<String>> columnsByIndex = new HashSet<>();
    for (Map<Short, String> columns : indexes.values()) {
      columnsByIndex.add(new HashSet<>(columns.values()));
    }
    return columnsByIndex;
  }

  private record ColumnDefinition(String typeName, int size, int decimalDigits) {}
}
