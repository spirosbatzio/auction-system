package com.mtn.agent.service;

public interface ValuationStrategy {

  double getValue(String itemId);

  String getName();
}
