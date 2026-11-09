package com.example.prices.infrastructure.adapter.out.persistence;

import com.example.prices.application.PriceRepository;
import com.example.prices.domain.BrandId;
import com.example.prices.domain.Price;
import com.example.prices.domain.ProductId;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class PricePersistenceAdapter implements PriceRepository {

  private final PriceJpaRepository priceJpaRepository;
  private final PriceEntityMapper priceEntityMapper;

  public PricePersistenceAdapter(
      PriceJpaRepository priceJpaRepository, PriceEntityMapper priceEntityMapper) {
    this.priceJpaRepository = priceJpaRepository;
    this.priceEntityMapper = priceEntityMapper;
  }

  @Override
  public List<Price> findApplicablePrices(
      BrandId brandId, ProductId productId, LocalDateTime applicationDate) {
    return priceJpaRepository
        .findApplicablePrices(brandId.value(), productId.value(), applicationDate)
        .stream()
        .map(priceEntityMapper::toDomain)
        .toList();
  }
}
