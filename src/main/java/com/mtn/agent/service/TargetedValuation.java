package com.mtn.agent.service;

import java.util.Map;

public class TargetedValuation implements ValuationStrategy {

  private final Map<String, Double> privateValues;

  public TargetedValuation(Map<String, Double> privateValues) {
    this.privateValues = privateValues;
  }

  @Override
  public double getValue(String itemId) {
    return privateValues.getOrDefault(itemId, 0.0);
  }

  @Override
  public String getName() {
    return "TargetedValuation";
  }
}
