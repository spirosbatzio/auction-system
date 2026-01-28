package com.mtn.agent.service;

import com.mtn.agent.domain.AuctionItem;
import com.mtn.agent.domain.AuctionState;
import com.mtn.agent.domain.Bid;
import jakarta.enterprise.context.Dependent;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.UUID;

@Dependent
public class AgentService {

  private static final Logger LOG = Logger.getLogger(AgentService.class);

  private String agentId;
  private Map<String, Double> valuations;
  private BiddingStrategy strategy;
  private double budgetLimit;

  public void init(String idPrefix, Map<String, Double> valuations, String strategyType, double budgetLimit) {

    if (idPrefix.contains("_")) {
      this.agentId = idPrefix;
    } else {
      this.agentId = idPrefix + "_" + UUID.randomUUID().toString().substring(0, 4);
    }

    this.valuations = valuations;
    this.budgetLimit = budgetLimit;
    this.strategy = resolveStrategy(strategyType);

    LOG.debugv("Agent {0} initialized. Strategy: {1}, Budget: {2}", agentId, strategy.getName(), budgetLimit);
  }

  public String getAgentId() {
    return agentId;
  }

  public Bid decideBid(AuctionState state) {
    if (!state.isActive()) return null;

    valuations.put("BUDGET_LIMIT", this.budgetLimit);

    return strategy.decide(state, valuations, agentId);
  }

  private BiddingStrategy resolveStrategy(String type) {
    if (type == null) return new MyopicStrategy();

    return switch (type.toUpperCase()) {
      case "SNIPER" -> new SniperStrategy();
      case "MYOPIC" -> new MyopicStrategy();
      case "BUDGET" -> new BudgetConstrainedStrategy();
      case "BUNDLE" -> new BundleStrategy();
      case "FLEXIBLE" -> new FlexibleStrategy();
      default -> null;
    };
  }

  public static class MyopicStrategy implements BiddingStrategy {
    @Override
    public String getName() {
      return "MYOPIC";
    }

    @Override
    public Bid decide(AuctionState state, Map<String, Double> valuations, String agentId) {
      Bid bestBid = null;
      double maxUtility = -1.0;

      for (AuctionItem item : state.items()) {
        if (agentId.equals(item.currentWinner())) continue;

        double myValue = valuations.getOrDefault(item.id(), 0.0);
        double askPrice = item.price() + 1.0;
        double utility = myValue - askPrice;

        if (utility > 0 && utility > maxUtility) {
          maxUtility = utility;
          bestBid = new Bid(agentId, item.id(), askPrice);
        }
      }
      return bestBid;
    }
  }

  public static class BudgetConstrainedStrategy implements BiddingStrategy {
    @Override
    public String getName() {
      return "BUDGET";
    }

    @Override
    public Bid decide(AuctionState state, Map<String, Double> valuations, String agentId) {
      double budget = valuations.getOrDefault("BUDGET_LIMIT", Double.MAX_VALUE);

      if (budget < 0) budget = Double.MAX_VALUE;

      double currentExposure = 0;
      for (AuctionItem item : state.items()) {
        if (agentId.equals(item.currentWinner())) {
          currentExposure += item.price();
        }
      }

      Bid bestBid = null;
      double maxUtility = -1.0;

      for (AuctionItem item : state.items()) {
        if (agentId.equals(item.currentWinner())) continue;

        double myValue = valuations.getOrDefault(item.id(), 0.0);
        double askPrice = item.price() + 1.0;


        if (currentExposure + askPrice > budget) continue;

        double utility = myValue - askPrice;
        if (utility > 0 && utility > maxUtility) {
          maxUtility = utility;
          bestBid = new Bid(agentId, item.id(), askPrice);
        }
      }
      return bestBid;
    }
  }

  public static class SniperStrategy implements BiddingStrategy {
    @Override
    public String getName() {
      return "SNIPER";
    }

    @Override
    public Bid decide(AuctionState state, Map<String, Double> valuations, String agentId) {
      if (state.round() < 3) return null;


      return new MyopicStrategy().decide(state, valuations, agentId);
    }
  }

  public static class BundleStrategy implements BiddingStrategy {
    @Override
    public String getName() {
      return "BUNDLE";
    }

    @Override
    public Bid decide(AuctionState state, Map<String, Double> valuations, String agentId) {
      double totalBundleValue = 0;
      double currentBundleCost = 0;
      boolean isLosingAny = false;
      String itemToBid = null;

      for (AuctionItem item : state.items()) {
        double val = valuations.getOrDefault(item.id(), 0.0);
        if (val > 0) {
          totalBundleValue += val;
          if (agentId.equals(item.currentWinner())) {
            currentBundleCost += item.price();
          } else {
            isLosingAny = true;
            currentBundleCost += (item.price() + 1.0);
            if (itemToBid == null) itemToBid = item.id();
          }
        }
      }

      if (currentBundleCost > totalBundleValue) return null;

      if (isLosingAny && itemToBid != null) {
        String finalItemToBid = itemToBid;
        double askPrice = state.items().stream()
                .filter(i -> i.id().equals(finalItemToBid)).findFirst().get().price() + 1.0;
        return new Bid(agentId, finalItemToBid, askPrice);
      }
      return null;
    }
  }

  public static class FlexibleStrategy implements BiddingStrategy {
    @Override
    public String getName() {
      return "FLEXIBLE";
    }

    @Override
    public Bid decide(AuctionState state, Map<String, Double> valuations, String agentId) {

      for (AuctionItem item : state.items()) {
        if (valuations.containsKey(item.id()) && valuations.get(item.id()) > 0) {
          if (agentId.equals(item.currentWinner())) return null;
        }
      }

      Bid bestBid = null;
      double minPrice = Double.MAX_VALUE;

      for (AuctionItem item : state.items()) {
        double myValue = valuations.getOrDefault(item.id(), 0.0);
        if (myValue <= 0) continue;

        double askPrice = item.price() + 1.0;
        double utility = myValue - askPrice;

        if (utility > 0 && askPrice < minPrice) {
          minPrice = askPrice;
          bestBid = new Bid(agentId, item.id(), askPrice);
        }
      }
      return bestBid;
    }
  }
}