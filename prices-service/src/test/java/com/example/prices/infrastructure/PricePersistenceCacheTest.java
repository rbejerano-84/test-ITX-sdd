package com.example.prices.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.prices.domain.BrandId;
import com.example.prices.domain.Price;
import com.example.prices.domain.PriceQuery;
import com.example.prices.domain.PriceRepository;
import com.example.prices.domain.ProductId;
import com.example.prices.infrastructure.config.CacheConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@SpringBootTest
class PricePersistenceCacheTest {

  private static final PriceQuery QUERY =
      new PriceQuery(
          new BrandId(1), new ProductId(35455), LocalDateTime.parse("2020-06-14T10:00:00"));

  @Autowired private PriceRepository priceRepository;

  @Autowired private CacheManager cacheManager;

  @AfterEach
  void clearPriceQueryCache() {
    priceCache().clear();
  }

  @Test
  void reusesCachedCandidatesForTheSameQuery() {
    List<Price> firstResult = priceRepository.findApplicablePrices(QUERY);
    List<Price> secondResult = priceRepository.findApplicablePrices(QUERY);

    assertThat(firstResult).hasSize(1);
    assertThat(secondResult).isSameAs(firstResult);
    assertThat(priceCache().get(QUERY).get()).isSameAs(firstResult);
  }

  @Test
  void usesTheCompleteQueryAsCacheKey() {
    PriceQuery differentDate =
        new PriceQuery(QUERY.brandId(), QUERY.productId(), QUERY.applicationDate().plusHours(1));

    List<Price> originalResult = priceRepository.findApplicablePrices(QUERY);
    List<Price> differentDateResult = priceRepository.findApplicablePrices(differentDate);

    assertThat(differentDateResult).isNotSameAs(originalResult);
    assertThat(priceCache().get(QUERY).get()).isSameAs(originalResult);
    assertThat(priceCache().get(differentDate).get()).isSameAs(differentDateResult);
  }

  private Cache priceCache() {
    return cacheManager.getCache(CacheConfig.PRICE_QUERY_CACHE);
  }
}
