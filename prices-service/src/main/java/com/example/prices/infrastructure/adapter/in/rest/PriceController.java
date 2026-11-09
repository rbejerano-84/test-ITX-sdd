package com.example.prices.infrastructure.adapter.in.rest;

import com.example.prices.application.GetApplicablePriceUseCase;
import com.example.prices.domain.BrandId;
import com.example.prices.domain.PriceQuery;
import com.example.prices.domain.ProductId;
import com.example.prices.infrastructure.adapter.in.rest.generated.api.PricesApi;
import com.example.prices.infrastructure.adapter.in.rest.generated.model.PriceResponse;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PriceController implements PricesApi {

  private final GetApplicablePriceUseCase getApplicablePriceUseCase;
  private final PriceRestMapper priceRestMapper;

  public PriceController(
      GetApplicablePriceUseCase getApplicablePriceUseCase, PriceRestMapper priceRestMapper) {
    this.getApplicablePriceUseCase =
        Objects.requireNonNull(getApplicablePriceUseCase, "Price use case is required");
    this.priceRestMapper = Objects.requireNonNull(priceRestMapper, "Price REST mapper is required");
  }

  @Override
  public ResponseEntity<PriceResponse> getApplicablePrice(
      LocalDateTime applicationDate, Long productId, Long brandId) {
    return ResponseEntity.ok(
        priceRestMapper.toResponse(
            getApplicablePriceUseCase.getApplicablePrice(
                new PriceQuery(new BrandId(brandId), new ProductId(productId), applicationDate))));
  }
}
