package com.example.prices.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.prices.application.GetApplicablePriceUseCase;
import com.example.prices.application.PriceNotFoundException;
import com.example.prices.domain.BrandId;
import com.example.prices.domain.DateRange;
import com.example.prices.domain.Money;
import com.example.prices.domain.Price;
import com.example.prices.domain.PriceListId;
import com.example.prices.domain.ProductId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import({PriceControllerWebMvcTest.UseCaseConfiguration.class, PriceRestMapper.class})
@ExtendWith(OutputCaptureExtension.class)
class PriceControllerWebMvcTest {

  private static final LocalDateTime APPLICATION_DATE = LocalDateTime.parse("2020-06-14T10:00:00");

  @Autowired private MockMvc mockMvc;

  @Autowired private GetApplicablePriceUseCase getApplicablePriceUseCase;

  @BeforeEach
  void resetUseCase() {
    reset(getApplicablePriceUseCase);
  }

  @Test
  void returnsContractPriceResponseForSuccessfulQuery() throws Exception {
    when(getApplicablePriceUseCase.getApplicablePrice(
            any(BrandId.class), any(ProductId.class), any(LocalDateTime.class)))
        .thenReturn(applicablePrice());

    mockMvc
        .perform(
            get("/api/v1/prices")
                .param("applicationDate", "2020-06-14T10:00:00")
                .param("productId", "35455")
                .param("brandId", "1"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.productId").value(35455))
        .andExpect(jsonPath("$.brandId").value(1))
        .andExpect(jsonPath("$.priceList").value(1))
        .andExpect(jsonPath("$.startDate").value("2020-06-14T00:00:00"))
        .andExpect(jsonPath("$.endDate").value("2020-12-31T23:59:59"))
        .andExpect(jsonPath("$.price").value(35.50))
        .andExpect(jsonPath("$.currency").value("EUR"));
  }

  @Test
  void returnsProblemWithFieldErrorWhenApplicationDateIsMissing() throws Exception {
    mockMvc
        .perform(get("/api/v1/prices").param("productId", "35455").param("brandId", "1"))
        .andExpect(status().isBadRequest())
        .andExpect(problemContentType())
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").isNotEmpty())
        .andExpect(jsonPath("$.instance").value("/api/v1/prices"))
        .andExpect(jsonPath("$.errors[0].field").value("applicationDate"));
  }

  @Test
  void returnsProblemWithFieldErrorWhenApplicationDateIsInvalid() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/prices")
                .param("applicationDate", "not-a-date")
                .param("productId", "35455")
                .param("brandId", "1"))
        .andExpect(status().isBadRequest())
        .andExpect(problemContentType())
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").isNotEmpty())
        .andExpect(jsonPath("$.instance").value("/api/v1/prices"))
        .andExpect(jsonPath("$.errors[0].field").value("applicationDate"));
  }

  @Test
  void returnsProblemWithFieldErrorWhenProductIdIsNotNumeric() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/prices")
                .param("applicationDate", "2020-06-14T10:00:00")
                .param("productId", "not-a-number")
                .param("brandId", "1"))
        .andExpect(status().isBadRequest())
        .andExpect(problemContentType())
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").isNotEmpty())
        .andExpect(jsonPath("$.instance").value("/api/v1/prices"))
        .andExpect(jsonPath("$.errors[0].field").value("productId"));
  }

  @Test
  void returnsProblemWithFieldErrorWhenProductIdIsLessThanOne() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/prices")
                .param("applicationDate", "2020-06-14T10:00:00")
                .param("productId", "0")
                .param("brandId", "1"))
        .andExpect(status().isBadRequest())
        .andExpect(problemContentType())
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").isNotEmpty())
        .andExpect(jsonPath("$.instance").value("/api/v1/prices"))
        .andExpect(jsonPath("$.errors[0].field").value("productId"));
  }

  @Test
  void returnsProblemWithRequestedParametersWhenNoPriceIsApplicable() throws Exception {
    when(getApplicablePriceUseCase.getApplicablePrice(
            new BrandId(1), new ProductId(35455), APPLICATION_DATE))
        .thenThrow(
            new PriceNotFoundException(new BrandId(1), new ProductId(35455), APPLICATION_DATE));

    mockMvc
        .perform(
            get("/api/v1/prices")
                .param("applicationDate", "2020-06-14T10:00:00")
                .param("productId", "35455")
                .param("brandId", "1"))
        .andExpect(status().isNotFound())
        .andExpect(problemContentType())
        .andExpect(jsonPath("$.title").value("Price Not Found"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(
            jsonPath("$.detail")
                .value("No applicable price for brand 1, product 35455 at 2020-06-14T10:00"))
        .andExpect(jsonPath("$.instance").value("/api/v1/prices"));
  }

  @Test
  void returnsProblemWhenPostIsNotSupported() throws Exception {
    mockMvc
        .perform(post("/api/v1/prices"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().string("Allow", "GET"))
        .andExpect(problemContentType())
        .andExpect(jsonPath("$.title").value("Method Not Allowed"))
        .andExpect(jsonPath("$.status").value(405))
        .andExpect(jsonPath("$.detail").isNotEmpty())
        .andExpect(jsonPath("$.instance").value("/api/v1/prices"));
  }

  @Test
  void returnsProblemWhenXmlIsRequested() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/prices")
                .accept(MediaType.APPLICATION_XML)
                .param("applicationDate", "2020-06-14T10:00:00")
                .param("productId", "35455")
                .param("brandId", "1"))
        .andExpect(status().isNotAcceptable())
        .andExpect(problemContentType())
        .andExpect(jsonPath("$.title").value("Not Acceptable"))
        .andExpect(jsonPath("$.status").value(406))
        .andExpect(jsonPath("$.detail").isNotEmpty())
        .andExpect(jsonPath("$.instance").value("/api/v1/prices"));
  }

  @Test
  void returnsGenericProblemForUnexpectedException(CapturedOutput output) throws Exception {
    when(getApplicablePriceUseCase.getApplicablePrice(
            new BrandId(1), new ProductId(35455), APPLICATION_DATE))
        .thenThrow(new IllegalStateException("internal database credential"));

    mockMvc
        .perform(
            get("/api/v1/prices")
                .param("applicationDate", "2020-06-14T10:00:00")
                .param("productId", "35455")
                .param("brandId", "1"))
        .andExpect(status().isInternalServerError())
        .andExpect(problemContentType())
        .andExpect(jsonPath("$.title").value("Internal Server Error"))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
        .andExpect(jsonPath("$.instance").value("/api/v1/prices"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("internal database credential"))));

    assertThat(output).contains("ERROR", "IllegalStateException", "internal database credential");
  }

  private static org.springframework.test.web.servlet.ResultMatcher problemContentType() {
    return content().contentTypeCompatibleWith(MediaType.valueOf("application/problem+json"));
  }

  private static Price applicablePrice() {
    return new Price(
        new BrandId(1),
        new DateRange(
            LocalDateTime.parse("2020-06-14T00:00:00"), LocalDateTime.parse("2020-12-31T23:59:59")),
        new PriceListId(1),
        new ProductId(35455),
        0,
        new Money(new BigDecimal("35.50"), Currency.getInstance("EUR")));
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class UseCaseConfiguration {

    @Bean
    GetApplicablePriceUseCase getApplicablePriceUseCase() {
      return org.mockito.Mockito.mock(GetApplicablePriceUseCase.class);
    }
  }
}
