package com.mtn.agent.service;

import com.mtn.agent.domain.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SimulationRunner {

  private static final Logger LOG = Logger.getLogger(SimulationRunner.class);

  @Inject
  AuctioneerService auctioneer;

  @Inject
  Instance<AgentService> agentFactory;

  @Inject
  ValuationGenerator valGenerator;

  @Inject
  ScenarioService scenarioService;

  @Inject
  EquilibriumAnalysisService equilibriumAnalysisService;

  private final List<RoundStat> statsHistory = Collections.synchronizedList(new ArrayList<>());
  private final List<BidRecord> bidHistory = Collections.synchronizedList(new ArrayList<>());
  private final Map<String, Map<String, Double>> agentValuations = new ConcurrentHashMap<>();
  private final List<EquilibriumRoundStat> equilibriumHistory = Collections.synchronizedList(new ArrayList<>());

  private List<AgentService> currentAgents = List.of();

  public List<RoundStat> getStatsHistory() {
    return new ArrayList<>(statsHistory);
  }

  public List<BidRecord> getBidHistory() {
    return new ArrayList<>(bidHistory);
  }

  public List<AuctionItem> getFinalItems() {
    return auctioneer.getState().items();
  }

  public List<AgentService> getCurrentAgents() {
    return currentAgents;
  }

  public List<EquilibriumRoundStat> getEquilibriumHistory() {
    return new ArrayList<>(equilibriumHistory);
  }

  public void runInMemoryScenario(Long scenarioId) {
    Optional<ScenarioData> scenarioOpt = scenarioService.getScenario(scenarioId);

    if (scenarioOpt.isEmpty()) {
      LOG.error("Scenario not found with ID: " + scenarioId);
      return;
    }

    ScenarioData scenario = scenarioOpt.get();
    LOG.infov("=== LOADING IN-MEMORY SCENARIO: {0} ===", scenario.name());

    auctioneer.init(scenario.numberOfSlots(), scenario.epsilon());
    agentValuations.clear();
    List<AgentService> agents = new ArrayList<>();

    for (AgentData config : scenario.agents()) {
      AgentService agent = agentFactory.get();

      Map<String, Double> vals = valGenerator.generate(
              config.valuationType(),
              scenario.numberOfSlots(),
              config.targetSlot()
      );
      double budget = (config.budgetLimit() == 0) ? -1.0 : config.budgetLimit();
      agent.init(config.agentName(), vals, config.strategyType(), budget);
      agentValuations.put(agent.getAgentId(), vals);
      agents.add(agent);

      LOG.infov("Loaded Agent: {0} [Strategy: {1} Valuation: {2} Budget: {3}",
              config.agentName(), config.strategyType(), config.valuationType(), budget);
    }

    currentAgents = agents;

    runLoop(agents, scenario.maxRounds());
  }

  private void runLoop(List<AgentService> agents, int maxRounds) {
    statsHistory.clear();
    bidHistory.clear();
    equilibriumHistory.clear();

    int currentRound = 0;
    System.out.println("DATA_CSV:Round,TotalBids,Revenue");

    while (currentRound < maxRounds) {
      AuctionState state = auctioneer.getState();

      if (!state.isActive()) {
        LOG.info("Simulation finished naturally (Equilibrium reached).");
        break;
      }

      currentRound++;

      int bidsInThisRound = 0;
      for (AgentService agent : agents) {
        Bid bid = agent.decideBid(state);
        if (bid != null) {
          auctioneer.receiveBid(bid);
          bidsInThisRound++;
          bidHistory.add(new BidRecord(currentRound, agent.getAgentId(), bid.itemId(), bid.amount()));
        }
      }

      auctioneer.resolveRound();

      double revenue = state.items().stream()
              .mapToDouble(item -> item.price())
              .sum();

      statsHistory.add(new RoundStat(currentRound, bidsInThisRound, revenue));

      EquilibriumAnalysisService.NashEquilibriumResult nashResult = equilibriumAnalysisService.checkNashEquilibrium(
              state, agentValuations, agents);
      EquilibriumAnalysisService.ParetoEfficiencyResult paretoResult = equilibriumAnalysisService.calculateParetoEfficiency(
              state, agentValuations);

      equilibriumHistory.add(new EquilibriumRoundStat(
              currentRound,
              nashResult.isNashEquilibrium(),
              nashResult.agentsWhoCanImprove().size(),
              paretoResult.efficiencyRatio(),
              paretoResult.currentSocialWelfare()
      ));

      System.out.println("DATA_CSV:" + currentRound + "," + bidsInThisRound + "," + revenue);

      try { Thread.sleep(50); } catch (InterruptedException e) {}
    }

    printFinalResults();
  }

  private void printFinalResults() {
    System.out.println("\n====== FINAL ALLOCATION ======");
    var items = auctioneer.getState().items();
    double totalWelfare = 0;

    for (var item : items) {
      String owner = item.currentWinner() == null ? "NOBODY" : item.currentWinner();
      System.out.printf("Item %s -> Owned by %s at Price %.2f%n", item.id(), owner, item.price());
      totalWelfare += item.price();
    }
    System.out.println("Total Revenue (Social Welfare Proxy): " + totalWelfare);
    System.out.println("==============================\n");
  }

  public EquilibriumAnalysisService.NashEquilibriumResult getNashEquilibriumResult() {
    AuctionState state = auctioneer.getState();
    List<AgentService> agents = getCurrentAgents(); // You'll need to track this
    return equilibriumAnalysisService.checkNashEquilibrium(state, agentValuations, agents);
  }

  public EquilibriumAnalysisService.ParetoEfficiencyResult getParetoEfficiencyResult() {
    AuctionState state = auctioneer.getState();
    return equilibriumAnalysisService.calculateParetoEfficiency(state, agentValuations);
  }
}