package com.mtn.agent.service;

import com.mtn.agent.domain.AgentPayoff;
import com.mtn.agent.domain.AuctionItem;
import com.mtn.agent.domain.AuctionState;
import com.mtn.agent.service.AgentService;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class EquilibriumAnalysisService {

  private static final Logger LOG = Logger.getLogger(EquilibriumAnalysisService.class);

  public Map<String, AgentPayoff> calculatePayoffs(
          AuctionState state,
          Map<String, Map<String, Double>> agentValuations
  ) {
    Map<String, AgentPayoff> payoffs = new HashMap<>();

    for (String agentId : agentValuations.keySet()) {
      Map<String, Double> valuations = agentValuations.get(agentId);
      double totalValuation = 0.0;
      double totalPricePaid = 0.0;
      int itemsWon = 0;

      for (AuctionItem item : state.items()) {
        if (agentId.equals(item.currentWinner())) {
          double valuation = valuations.getOrDefault(item.id(), 0.0);
          if (valuation == 0.0 && item.price() > 0) {
            LOG.warnv("Agent {0} won {1} but has no valuation for it!", agentId, item.id());
          }
          totalValuation += valuation;
          totalPricePaid += item.price();
          itemsWon++;

        }
      }

      double utility = totalValuation - totalPricePaid;
      payoffs.put(agentId, new AgentPayoff(
              agentId,
              totalValuation,
              totalPricePaid,
              utility,
              itemsWon
      ));
    }
    return payoffs;
  }

  public NashEquilibriumResult checkNashEquilibrium(
          AuctionState state,
          Map<String, Map<String, Double>> agentValuations,
          List<AgentService> agents
  ) {

    Map<String, AgentPayoff> currentPayoffs = calculatePayoffs(state, agentValuations);

    List<String> agentsWhoCanImprove = new ArrayList<>();

    for (AgentService agent : agents) {
      String agentId = agent.getAgentId();
      AgentPayoff currentPayoff = currentPayoffs.get(agentId);

      if (currentPayoff == null) continue;

      Map<String, Double> valuations = agentValuations.get(agentId);
      if (valuations == null) continue;

      // Unilateral deviation check: can this agent improve by switching to any unowned item?
      for (AuctionItem item : state.items()) {
        if (agentId.equals(item.currentWinner())) continue;

        double val = valuations.getOrDefault(item.id(), 0.0);
        double askPrice = item.price() + 1.0;
        double gainIfWon = val - askPrice;

        if (gainIfWon > 1e-6) {
          agentsWhoCanImprove.add(agentId);
          break;
        }
      }
    }

    boolean isNashEquilibrium = agentsWhoCanImprove.isEmpty();

    return new NashEquilibriumResult(
            isNashEquilibrium,
            agentsWhoCanImprove,
            currentPayoffs
    );
  }

  public ParetoEfficiencyResult calculateParetoEfficiency(
          AuctionState state,
          Map<String, Map<String, Double>> agentValuations) {

    Map<String, AgentPayoff> currentPayoffs = calculatePayoffs(state, agentValuations);

    // Calculate total social welfare (sum of valuations for won items, price-independent)
    double currentSocialWelfare = currentPayoffs.values().stream()
            .mapToDouble(AgentPayoff::totalValuation)
            .sum();

    // Find Pareto-optimal allocation (greedy: assign each item to agent with highest valuation)
    double paretoOptimalWelfare = calculateParetoOptimalWelfare(state, agentValuations);

    double efficiencyRatio = paretoOptimalWelfare > 0
            ? currentSocialWelfare / paretoOptimalWelfare
            : 0.0;

    boolean isParetoOptimal = Math.abs(currentSocialWelfare - paretoOptimalWelfare) < 0.01;

    return new ParetoEfficiencyResult(
            isParetoOptimal,
            currentSocialWelfare,
            paretoOptimalWelfare,
            efficiencyRatio,
            currentPayoffs
    );
  }

  private double calculateParetoOptimalWelfare(
          AuctionState state,
          Map<String, Map<String, Double>> agentValuations) {

    // Greedy allocation: assign each item to agent with highest valuation
    double totalWelfare = 0.0;

    for (AuctionItem item : state.items()) {
      double maxValuation = 0.0;
      String bestAgent = null;

      for (Map.Entry<String, Map<String, Double>> entry : agentValuations.entrySet()) {
        double valuation = entry.getValue().getOrDefault(item.id(), 0.0);
        if (valuation > maxValuation) {
          maxValuation = valuation;
          bestAgent = entry.getKey();
        }
      }

      if (bestAgent != null) {
        totalWelfare += maxValuation; // Assuming price = 0 in optimal allocation
      }
    }

    return totalWelfare;
  }


  // Result classes
  public record NashEquilibriumResult(
          boolean isNashEquilibrium,
          List<String> agentsWhoCanImprove,
          Map<String, AgentPayoff> payoffs
  ) {
  }

  public record ParetoEfficiencyResult(
          boolean isParetoOptimal,
          double currentSocialWelfare,
          double paretoOptimalWelfare,
          double efficiencyRatio,  // 0.0 to 1.0
          Map<String, AgentPayoff> payoffs
  ) {
  }
}
