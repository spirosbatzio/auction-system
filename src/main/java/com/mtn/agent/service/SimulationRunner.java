package com.mtn.agent.service;

import com.mtn.agent.domain.AuctionItem;
import com.mtn.agent.domain.AuctionState;
import com.mtn.agent.domain.Bid;
import com.mtn.agent.domain.BidRecord;
import com.mtn.agent.domain.RoundStat;
import com.mtn.agent.domain.entity.AgentConfig;
import com.mtn.agent.domain.entity.ScenarioConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.*;

@ApplicationScoped
public class SimulationRunner {

  private static final Logger LOG = Logger.getLogger(SimulationRunner.class);

  @Inject
  AuctioneerService auctioneer;

  @Inject
  Instance<AgentService> agentFactory;

  @Inject
  ValuationGenerator valGenerator;

  private final List<RoundStat> statsHistory = Collections.synchronizedList(new ArrayList<>());
  private final List<BidRecord> bidHistory = Collections.synchronizedList(new ArrayList<>());

  public List<RoundStat> getStatsHistory() {
    return new ArrayList<>(statsHistory);
  }

  public List<BidRecord> getBidHistory() {
    return new ArrayList<>(bidHistory);
  }

  public List<AuctionItem> getFinalItems() {
    return auctioneer.getState().items();
  }

  @Transactional
  public void runDatabaseScenario(Long scenarioId) {

    ScenarioConfig scenario = ScenarioConfig.findById(scenarioId);

    if (scenario == null) {
      LOG.error("Scenario not found with ID: " + scenarioId);
      return;
    }

    LOG.infov("=== LOADING DB SCENARIO: {0} ===", scenario.name);

    auctioneer.init(scenario.numberOfSlots);

    List<AgentService> agents = new ArrayList<>();

    for (AgentConfig config : scenario.agents) {
      AgentService agent = agentFactory.get();

      Map<String, Double> vals = valGenerator.generate(
              config.valuationType,
              scenario.numberOfSlots,
              config.targetSlot
      );
      double budget = (config.budgetLimit == 0) ? -1.0 : config.budgetLimit;
      agent.init(config.agentName, vals, config.strategyType, budget);
      agents.add(agent);

      LOG.infov("Loaded Agent: {0} [Strategy: {1} Valuation: {2} Budget: {3}",
              config.agentName, config.strategyType, config.valuationType, budget);
    }

    runLoop(agents, scenario.maxRounds);
  }

  public void runSimulation(int numberOfAgents) {
    LOG.info("=== STARTING RANDOM SIMULATION ===");

    auctioneer.init(5);

    List<AgentService> agents = new ArrayList<>();
    Random rand = new Random();

    for (int i = 0; i < numberOfAgents; i++) {
      AgentService agent = agentFactory.get();

      Map<String, Double> vals = new HashMap<>();
      vals.put("SLOT_1", 10.0 + rand.nextInt(15));
      vals.put("SLOT_2", 5.0 + rand.nextInt(10));
      vals.put("SLOT_3", (double) rand.nextInt(20));

      agent.init("SimAgent_" + i, vals, "MYOPIC", -1);
      agents.add(agent);
    }

    runLoop(agents, 100);
  }

  private void runLoop(List<AgentService> agents, int maxRounds) {
    statsHistory.clear();
    bidHistory.clear();

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
          bidHistory.add(new BidRecord(currentRound, agent.getAgentId(),bid.itemId(), bid.amount()));
        }
      }

      auctioneer.resolveRound();

      double revenue = state.items().stream()
              .mapToDouble(item -> item.price())
              .sum();

      statsHistory.add(new RoundStat(currentRound, bidsInThisRound, revenue));

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
}