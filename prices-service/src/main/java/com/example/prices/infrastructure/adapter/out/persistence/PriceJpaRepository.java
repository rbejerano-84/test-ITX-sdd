package com.example.prices.infrastructure.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceJpaRepository extends JpaRepository<PriceEntity, Long> {

  @Query(
      """
      SELECT price
      FROM PriceEntity price
      WHERE price.brandId = :brandId
        AND price.productId = :productId
        AND price.startDate <= :applicationDate
        AND price.endDate >= :applicationDate
      """)
  List<PriceEntity> findApplicablePrices(
      @Param("brandId") long brandId,
      @Param("productId") long productId,
      @Param("applicationDate") LocalDateTime applicationDate);
}
