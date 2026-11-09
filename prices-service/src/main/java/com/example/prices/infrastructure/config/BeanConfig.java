package com.example.prices.infrastructure.config;

import com.example.prices.application.GetApplicablePriceService;
import com.example.prices.application.PriceRepository;
import com.example.prices.domain.PriceSelector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class BeanConfig {

  @Bean
  PriceSelector priceSelector() {
    return new PriceSelector();
  }

  @Bean
  GetApplicablePriceService getApplicablePriceService(
      PriceRepository priceRepository, PriceSelector priceSelector) {
    return new GetApplicablePriceService(priceRepository, priceSelector);
  }
}
