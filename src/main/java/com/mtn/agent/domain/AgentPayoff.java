package com.mtn.agent.domain;

public record AgentPayoff(
        String agentId,
        double totalValuation,
        double totalPricePaid,
        double utility,
        int itemsWon
) {
  public double getUtility() {
    return utility;
  }
}
