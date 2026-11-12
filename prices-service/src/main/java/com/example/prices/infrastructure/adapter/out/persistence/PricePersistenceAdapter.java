package com.example.prices.infrastructure.adapter.out.persistence;

import com.example.prices.domain.Price;
import com.example.prices.domain.PriceQuery;
import com.example.prices.domain.PriceRepository;
import java.util.List;
import java.util.Objects;
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
  public List<Price> findApplicablePrices(PriceQuery query) {
    Objects.requireNonNull(query, "Price query is required");
    return priceJpaRepository
        .findApplicablePrices(
            query.brandId().value(), query.productId().value(), query.applicationDate())
        .stream()
        .map(priceEntityMapper::toDomain)
        .toList();
  }
}
