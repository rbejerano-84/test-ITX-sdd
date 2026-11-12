package com.example.prices.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class PriceEndToEndTest {

  private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

  @Value("${local.server.port}")
  private int port;

  @Test
  void returnsExpectedPricesForAllFiveReferenceScenarios() throws Exception {
    List<PriceScenario> scenarios =
        List.of(
            new PriceScenario("2020-06-14T10:00:00", 1, "35.50"),
            new PriceScenario("2020-06-14T16:00:00", 2, "25.45"),
            new PriceScenario("2020-06-14T21:00:00", 1, "35.50"),
            new PriceScenario("2020-06-15T10:00:00", 3, "30.50"),
            new PriceScenario("2020-06-16T21:00:00", 4, "38.95"));

    for (PriceScenario scenario : scenarios) {
      HttpResponse<String> response = getPrice(scenario.applicationDate(), "35455", "1");

      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(response.headers().firstValue("Content-Type").orElse(""))
          .startsWith("application/json");
      assertThat(JsonPath.<Integer>read(response.body(), "$.productId")).isEqualTo(35455);
      assertThat(JsonPath.<Integer>read(response.body(), "$.brandId")).isEqualTo(1);
      assertThat(JsonPath.<Integer>read(response.body(), "$.priceList"))
          .isEqualTo(scenario.priceList());
      assertThat(new BigDecimal(JsonPath.read(response.body(), "$.price").toString()))
          .isEqualByComparingTo(scenario.price());
      assertThat(JsonPath.<String>read(response.body(), "$.currency")).isEqualTo("EUR");
    }
  }

  @Test
  void returnsProblemDetailWhenNoPriceIsApplicable() throws Exception {
    HttpResponse<String> response = getPrice("2019-01-01T10:00:00", "35455", "1");

    assertThat(response.statusCode()).isEqualTo(404);
    assertThat(response.headers().firstValue("Content-Type").orElse(""))
        .startsWith("application/problem+json");
    assertThat(JsonPath.<Integer>read(response.body(), "$.status")).isEqualTo(404);
    assertThat(JsonPath.<String>read(response.body(), "$.instance")).isEqualTo("/api/v1/prices");
    assertThat(JsonPath.<String>read(response.body(), "$.detail")).isNotBlank();
  }

  @Test
  void returnsProblemDetailForInvalidRequestParameters() throws Exception {
    HttpResponse<String> response = getPrice("not-a-date", "35455", "1");

    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(response.headers().firstValue("Content-Type").orElse(""))
        .startsWith("application/problem+json");
    assertThat(JsonPath.<Integer>read(response.body(), "$.status")).isEqualTo(400);
    assertThat(JsonPath.<String>read(response.body(), "$.errors[0].field"))
        .isEqualTo("applicationDate");
    assertThat(JsonPath.<String>read(response.body(), "$.errors[0].message")).isNotBlank();
  }

  private HttpResponse<String> getPrice(String date, String productId, String brandId)
      throws Exception {
    URI uri =
        UriComponentsBuilder.fromUriString("http://localhost")
            .port(port)
            .path("/api/v1/prices")
            .queryParam("applicationDate", date)
            .queryParam("productId", productId)
            .queryParam("brandId", brandId)
            .build()
            .encode()
            .toUri();

    HttpRequest request =
        HttpRequest.newBuilder(uri).header("Accept", "application/json").GET().build();

    return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private record PriceScenario(String applicationDate, int priceList, String price) {}
}
