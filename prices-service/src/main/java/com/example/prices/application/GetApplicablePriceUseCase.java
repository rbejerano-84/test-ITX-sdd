package com.example.prices.application;

import com.example.prices.domain.Price;
import com.example.prices.domain.PriceQuery;

public interface GetApplicablePriceUseCase {

  Price getApplicablePrice(PriceQuery query);
}
