package com.example.prices.infrastructure;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.OpenApiMatchers.OpenApiValidationException;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
class PriceApiContractTest {

  private static final String OPENAPI_SPEC = "openapi/prices-api.yaml";
  private static final String PRICES_PATH = "/api/v1/prices";
  private static OpenApiInteractionValidator contractValidator;

  @Autowired private MockMvc mockMvc;

  @BeforeAll
  static void createContractValidator() {
    contractValidator =
        OpenApiInteractionValidator.createForSpecificationUrl(OPENAPI_SPEC)
            .withLevelResolver(
                LevelResolver.create()
                    // The API intentionally uses local date-times without an RFC 3339 offset.
                    .withLevel(
                        "validation.request.parameter.schema.format.date-time",
                        ValidationReport.Level.IGNORE)
                    .withLevel(
                        "validation.response.body.schema.format.date-time",
                        ValidationReport.Level.IGNORE)
                    .build())
            .build();
  }

  @Test
  void successfulResponseMatchesOpenApiContract() throws Exception {
    mockMvc
        .perform(
            get(PRICES_PATH)
                .param("applicationDate", "2020-06-14T10:00:00")
                .param("productId", "35455")
                .param("brandId", "1"))
        .andExpect(status().isOk())
        .andExpect(openApi().isValid(contractValidator));
  }

  @Test
  void notFoundResponseMatchesOpenApiContract() throws Exception {
    mockMvc
        .perform(
            get(PRICES_PATH)
                .param("applicationDate", "2020-06-14T10:00:00")
                .param("productId", "99999")
                .param("brandId", "1"))
        .andExpect(status().isNotFound())
        .andExpect(openApi().isValid(contractValidator));
  }

  @Test
  void badRequestResponseMatchesOpenApiContract() throws Exception {
    mockMvc
        .perform(
            get(PRICES_PATH)
                .param("applicationDate", "not-a-date")
                .param("productId", "35455")
                .param("brandId", "1"))
        .andExpect(status().isBadRequest())
        .andExpect(openApi().isValid(contractValidator));
  }

  @Test
  void validatorRejectsAResponseThatViolatesTheContract() {
    MockMvc invalidEndpoint =
        MockMvcBuilders.standaloneSetup(new InvalidPriceResponseController()).build();

    assertThatThrownBy(
            () ->
                invalidEndpoint
                    .perform(
                        get(PRICES_PATH)
                            .param("applicationDate", "2020-06-14T10:00:00")
                            .param("productId", "35455")
                            .param("brandId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(openApi().isValid(contractValidator)))
        .isInstanceOf(OpenApiValidationException.class);
  }

  @RestController
  static class InvalidPriceResponseController {

    @GetMapping(value = PRICES_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Integer> getInvalidPriceResponse() {
      return Map.of("brandId", 1);
    }
  }
}
