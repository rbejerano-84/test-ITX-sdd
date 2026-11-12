package com.example.prices.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class PriceAcceptanceTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void test1_requestAt10OnDay14() throws Exception {
    assertApplicablePrice(
        "2020-06-14T10:00:00", 1, "2020-06-14T00:00:00", "2020-12-31T23:59:59", "35.50");
  }

  @Test
  void test2_requestAt16OnDay14() throws Exception {
    assertApplicablePrice(
        "2020-06-14T16:00:00", 2, "2020-06-14T15:00:00", "2020-06-14T18:30:00", "25.45");
  }

  @Test
  void test3_requestAt21OnDay14() throws Exception {
    assertApplicablePrice(
        "2020-06-14T21:00:00", 1, "2020-06-14T00:00:00", "2020-12-31T23:59:59", "35.50");
  }

  @Test
  void test4_requestAt10OnDay15() throws Exception {
    assertApplicablePrice(
        "2020-06-15T10:00:00", 3, "2020-06-15T00:00:00", "2020-06-15T11:00:00", "30.50");
  }

  @Test
  void test5_requestAt21OnDay16() throws Exception {
    assertApplicablePrice(
        "2020-06-16T21:00:00", 4, "2020-06-15T16:00:00", "2020-12-31T23:59:59", "38.95");
  }

  @Test
  void returnsNotFoundForUnknownProduct() throws Exception {
    assertNotFound("2020-06-14T10:00:00", "99999");
  }

  @Test
  void returnsNotFoundBeforeAnyPriceIsApplicable() throws Exception {
    assertNotFound("2020-06-13T23:59:59", "35455");
  }

  @Test
  void returnsNotFoundAfterAllPricesExpire() throws Exception {
    assertNotFound("2021-01-01T00:00:00", "35455");
  }

  @Test
  void includesPriceAtItsStartDate() throws Exception {
    assertApplicablePrice(
        "2020-06-14T00:00:00", 1, "2020-06-14T00:00:00", "2020-12-31T23:59:59", "35.50");
  }

  @Test
  void includesPriceAtItsEndDate() throws Exception {
    assertApplicablePrice(
        "2020-06-14T18:30:00", 2, "2020-06-14T15:00:00", "2020-06-14T18:30:00", "25.45");
  }

  @Test
  void choosesTheHigherPriceListWhenApplicablePricesTieOnPriority() throws Exception {
    LocalDateTime startDate = LocalDateTime.parse("2020-06-14T00:00:00");
    LocalDateTime endDate = LocalDateTime.parse("2020-06-14T23:59:59");
    insertPrice(9, startDate, endDate, 10, 99100, 1, "10.00");
    insertPrice(9, startDate, endDate, 11, 99100, 1, "11.00");

    mockMvc
        .perform(
            get("/api/v1/prices")
                .param("applicationDate", "2020-06-14T12:00:00")
                .param("productId", "99100")
                .param("brandId", "9"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productId").value(99100))
        .andExpect(jsonPath("$.brandId").value(9))
        .andExpect(jsonPath("$.priceList").value(11))
        .andExpect(jsonPath("$.price").value(11.00));
  }

  @Test
  void returnsBadRequestWhenApplicationDateIsMissing() throws Exception {
    assertBadRequest(
        get("/api/v1/prices").param("productId", "35455").param("brandId", "1"), "applicationDate");
  }

  @Test
  void returnsBadRequestWhenProductIdIsMissing() throws Exception {
    assertBadRequest(
        get("/api/v1/prices").param("applicationDate", "2020-06-14T10:00:00").param("brandId", "1"),
        "productId");
  }

  @Test
  void returnsBadRequestWhenBrandIdIsMissing() throws Exception {
    assertBadRequest(
        get("/api/v1/prices")
            .param("applicationDate", "2020-06-14T10:00:00")
            .param("productId", "35455"),
        "brandId");
  }

  @Test
  void returnsBadRequestWhenApplicationDateIsInvalid() throws Exception {
    assertBadRequest(
        get("/api/v1/prices")
            .param("applicationDate", "not-a-date")
            .param("productId", "35455")
            .param("brandId", "1"),
        "applicationDate");
  }

  @Test
  void returnsBadRequestWhenProductIdIsNotNumeric() throws Exception {
    assertBadRequest(
        get("/api/v1/prices")
            .param("applicationDate", "2020-06-14T10:00:00")
            .param("productId", "not-a-number")
            .param("brandId", "1"),
        "productId");
  }

  @Test
  void returnsBadRequestWhenProductIdIsOutsideTheAllowedRange() throws Exception {
    assertBadRequest(
        get("/api/v1/prices")
            .param("applicationDate", "2020-06-14T10:00:00")
            .param("productId", "0")
            .param("brandId", "1"),
        "productId");
  }

  @Test
  void returnsBadRequestWhenBrandIdIsNotNumeric() throws Exception {
    assertBadRequest(
        get("/api/v1/prices")
            .param("applicationDate", "2020-06-14T10:00:00")
            .param("productId", "35455")
            .param("brandId", "not-a-number"),
        "brandId");
  }

  @Test
  void returnsBadRequestWhenBrandIdIsOutsideTheAllowedRange() throws Exception {
    assertBadRequest(
        get("/api/v1/prices")
            .param("applicationDate", "2020-06-14T10:00:00")
            .param("productId", "35455")
            .param("brandId", "0"),
        "brandId");
  }

  @Test
  void returnsMethodNotAllowedForPost() throws Exception {
    mockMvc
        .perform(post("/api/v1/prices"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(
            content().contentTypeCompatibleWith(MediaType.valueOf("application/problem+json")))
        .andExpect(jsonPath("$.status").value(405));
  }

  @Test
  void returnsNotAcceptableWhenXmlIsRequested() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/prices")
                .accept(MediaType.APPLICATION_XML)
                .param("applicationDate", "2020-06-14T10:00:00")
                .param("productId", "35455")
                .param("brandId", "1"))
        .andExpect(status().isNotAcceptable())
        .andExpect(
            content().contentTypeCompatibleWith(MediaType.valueOf("application/problem+json")))
        .andExpect(jsonPath("$.status").value(406));
  }

  private void assertApplicablePrice(
      String applicationDate,
      int expectedPriceList,
      String expectedStartDate,
      String expectedEndDate,
      String expectedPrice)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/prices")
                    .param("applicationDate", applicationDate)
                    .param("productId", "35455")
                    .param("brandId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productId").value(35455))
            .andExpect(jsonPath("$.brandId").value(1))
            .andExpect(jsonPath("$.priceList").value(expectedPriceList))
            .andExpect(jsonPath("$.startDate").value(expectedStartDate))
            .andExpect(jsonPath("$.endDate").value(expectedEndDate))
            .andExpect(jsonPath("$.currency").value("EUR"))
            .andReturn();

    Object actualPrice = JsonPath.read(result.getResponse().getContentAsString(), "$.price");
    assertThat(new BigDecimal(actualPrice.toString()))
        .isEqualByComparingTo(new BigDecimal(expectedPrice));
  }

  private void assertNotFound(String applicationDate, String productId) throws Exception {
    mockMvc
        .perform(
            get("/api/v1/prices")
                .param("applicationDate", applicationDate)
                .param("productId", productId)
                .param("brandId", "1"))
        .andExpect(status().isNotFound())
        .andExpect(
            content().contentTypeCompatibleWith(MediaType.valueOf("application/problem+json")))
        .andExpect(jsonPath("$.title").value("Price Not Found"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.instance").value("/api/v1/prices"));
  }

  private void assertBadRequest(MockHttpServletRequestBuilder request, String field)
      throws Exception {
    mockMvc
        .perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(
            content().contentTypeCompatibleWith(MediaType.valueOf("application/problem+json")))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errors[0].field").value(field));
  }

  private void insertPrice(
      long brandId,
      LocalDateTime startDate,
      LocalDateTime endDate,
      long priceList,
      long productId,
      int priority,
      String price) {
    jdbcTemplate.update(
        """
        INSERT INTO prices (
            brand_id, start_date, end_date, price_list, product_id, priority, price, currency
        ) VALUES (?, ?, ?, ?, ?, ?, ?, 'EUR')
        """,
        brandId,
        Timestamp.valueOf(startDate),
        Timestamp.valueOf(endDate),
        priceList,
        productId,
        priority,
        new BigDecimal(price));
  }
}
