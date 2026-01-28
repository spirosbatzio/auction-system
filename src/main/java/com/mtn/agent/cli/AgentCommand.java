package com.mtn.agent.cli;

import com.mtn.agent.client.AuctionClient;
import com.mtn.agent.domain.AuctionState;
import com.mtn.agent.domain.Bid;
import com.mtn.agent.service.AgentService;
import com.mtn.agent.service.ValuationGenerator;
import io.quarkus.runtime.Quarkus;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

import java.util.Map;

@CommandLine.Command(name = "agent", description = "Starts a Distributed Agent Client")
public class AgentCommand implements Runnable {

  @RestClient
  AuctionClient client;

  @Inject
  AgentService agentService;

  @Inject
  ValuationGenerator valGenerator;

  @CommandLine.Option(names = {"-u", "--url"}, defaultValue = "http://localhost:8080")
  String serverUrl;

  @CommandLine.Option(names = {"-t", "--type"}, description = "Valuation Type: RICH, POOR, FOCUSED", defaultValue = "RANDOM")
  String valType;

  @CommandLine.Option(names = {"-s", "--strategy"}, description = "Bidding Strategy: MYOPIC, SNIPER", defaultValue = "MYOPIC")
  String strategy;

  @CommandLine.Option(names = {"-n", "--slots"}, description = "Total slots in auction", defaultValue = "5")
  int totalSlots;

  @CommandLine.Option(names = {"-b", "--budget"}, description = "Budget limit (-1 for unlimited)", defaultValue = "-1.0")
  double budget;

  @Override
  public void run() {
    System.out.println("--- STARTING AGENT ---");
    System.out.println("Connecting to: " + serverUrl);

    Map<String, Double> vals = valGenerator.generate(valType, totalSlots, -1);

    agentService.init("NetAgent_" + valType, vals, strategy, budget);

    System.out.println("Agent ID: " + agentService.getAgentId());
    System.out.println("Strategy: " + strategy);

    while (true) {
      try {
        AuctionState state = client.getState();
        if (!state.isActive()) {
          System.out.println("Auction finished.");
          break;
        }

        Bid bid = agentService.decideBid(state);
        if (bid != null) {
          client.submitBid(bid);
          System.out.println("Bid submitted: " + bid);
        }
        Thread.sleep(1000);
      } catch (Exception e) {
        System.err.println("Connection error: " + e.getMessage());
        try { Thread.sleep(2000); } catch (InterruptedException ex) {}
      }
    }
    Quarkus.waitForExit();
  }
}