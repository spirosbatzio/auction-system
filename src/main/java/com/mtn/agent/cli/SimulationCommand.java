package com.mtn.agent.cli;

import com.mtn.agent.service.SimulationRunner;
import io.quarkus.runtime.Quarkus;
import jakarta.inject.Inject;
import picocli.CommandLine;

@CommandLine.Command(name = "sim", description = "In memory simulation for experiment results")
public class SimulationCommand implements Runnable {

  @Inject
  SimulationRunner runner;

  @CommandLine.Option(names = {"-n", "--agents"}, description = "Number of agents", defaultValue = "5")
  int numberOfAgents;

  @CommandLine.Option(names = {"-id", "--scenario-id"}, description = "Run a specific DB Scenario ID", defaultValue = "0")
  long scenarioId;

  @Override
  public void run() {
    if (scenarioId > 0) {
      System.out.println("Loading scenario " + scenarioId);
      runner.runDatabaseScenario(scenarioId);
    } else {
      System.out.println("Executing Random Simulation with " + numberOfAgents + " agents");
      runner.runSimulation(numberOfAgents);
    }

    Quarkus.waitForExit();

  }
}
