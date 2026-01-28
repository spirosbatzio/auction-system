package com.mtn.agent.service;

import com.mtn.agent.domain.AuctionState;
import com.mtn.agent.domain.Bid;

import java.util.Map;

public interface BiddingStrategy {
  Bid decide(AuctionState state, Map<String, Double> valuations, String agentId);

  String getName();
}
