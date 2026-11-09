package com.example.prices.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PriceAcceptanceTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void returnsTheApplicablePriceForEachReferenceScenario() throws Exception {
    assertApplicablePrice(
        "2020-06-14T10:00:00",
        1,
        "2020-06-14T00:00:00",
        "2020-12-31T23:59:59",
        "35.50");
    assertApplicablePrice(
        "2020-06-14T16:00:00",
        2,
        "2020-06-14T15:00:00",
        "2020-06-14T18:30:00",
        "25.45");
    assertApplicablePrice(
        "2020-06-14T21:00:00",
        1,
        "2020-06-14T00:00:00",
        "2020-12-31T23:59:59",
        "35.50");
    assertApplicablePrice(
        "2020-06-15T10:00:00",
        3,
        "2020-06-15T00:00:00",
        "2020-06-15T11:00:00",
        "30.50");
    assertApplicablePrice(
        "2020-06-16T21:00:00",
        4,
        "2020-06-15T16:00:00",
        "2020-12-31T23:59:59",
        "38.95");
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
}
