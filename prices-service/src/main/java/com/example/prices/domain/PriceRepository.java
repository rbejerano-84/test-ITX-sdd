package com.example.prices.domain;

import java.util.List;

public interface PriceRepository {

  List<Price> findApplicablePrices(PriceQuery query);
}
