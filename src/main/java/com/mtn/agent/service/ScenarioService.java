package com.mtn.agent.service;

import com.mtn.agent.domain.AgentData;
import com.mtn.agent.domain.ScenarioData;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class ScenarioService {
  private static final Logger LOG = Logger.getLogger(ScenarioService.class.getName());

  private final Map<Long, ScenarioData> scenarios = new ConcurrentHashMap<>();
  private final AtomicLong scenarioIdGenerator = new AtomicLong(100); // Start after pre-loaded ones
  private final AtomicLong agentIdGenerator = new AtomicLong(1000);


  @PostConstruct
  public void init() {
    loadPreLoadScenarios();
  }

  private void loadPreLoadScenarios() {
    ScenarioData scenario1 = new ScenarioData(
            1L,
            "General Purpose Cloud",
            5,
            50,
            1.0,
            new ArrayList<>()
    );

    scenario1 = scenario1.withAgent(new AgentData(10L, "Production_DB_Master", "MYOPIC", "RICH", -1, -1));
    scenario1 = scenario1.withAgent(new AgentData(11L, "Internal_Wiki_App", "MYOPIC", "POOR", -1, -1));
    scenario1 = scenario1.withAgent(new AgentData(12L, "Spot_Instance_Bot", "SNIPER", "RANDOM", -1, -1));
    scenario1 = scenario1.withAgent(new AgentData(13L, "License_Server_Lock", "MYOPIC", "FOCUSED", 1, -1));

    scenarios.put(1L, scenario1);

    ScenarioData scenario2 = new ScenarioData(
            2L,
            "HPC Cluster Congestion",
            10,
            100,
            1.0,
            new ArrayList<>()
    );

    scenario2 = scenario2.withAgent(new AgentData(20L, "Weather_Simulation_A", "MYOPIC", "RICH", -1, -1));
    scenario2 = scenario2.withAgent(new AgentData(21L, "Crypto_Miner_Farm", "MYOPIC", "RICH", -1, -1));
    scenario2 = scenario2.withAgent(new AgentData(22L, "Priority_Scheduler_X", "SNIPER", "RICH", -1, -1));
    scenario2 = scenario2.withAgent(new AgentData(23L, "Financial_Model_Risk", "MYOPIC", "RICH", -1, -1));

    scenarios.put(2L, scenario2);

    ScenarioData scenario3 = new ScenarioData(
            3L,
            "Mixed Workload Optimization",
            5,
            100,
            1.0,
            new ArrayList<>()
    );

    scenario3 = scenario3.withAgent(new AgentData(30L, "Web_Server_HA", "FLEXIBLE", "FLEXIBLE_PAIR", -1, -1));
    scenario3 = scenario3.withAgent(new AgentData(31L, "Distributed_ML_Job", "BUNDLE", "BUNDLE_PAIR", -1, -1));
    scenario3 = scenario3.withAgent(new AgentData(32L, "Dev_Test_Env", "BUDGET", "RICH", -1, 15.0));

    scenarios.put(3L, scenario3);

    LOG.infov("Loaded {0} pre-configured scenarios", scenarios.size());

  }

  public List<ScenarioData> getAllScenarios() {
    return new ArrayList<>(scenarios.values());
  }

  public Optional<ScenarioData> getScenario(Long id) {
    return Optional.ofNullable(scenarios.get(id));
  }

  public ScenarioData createScenario(String name, int numberOfSlots, int maxRounds) {
    return createScenario(name, numberOfSlots, maxRounds, 1.0);  // Default epsilon
  }

  public ScenarioData createScenario(String name, int numberOfSlots, int maxRounds, double epsilon) {
    Long id = scenarioIdGenerator.getAndIncrement();
    ScenarioData scenario = new ScenarioData(id, name, numberOfSlots, maxRounds,epsilon, new ArrayList<>());
    scenarios.put(id, scenario);
    LOG.infov("Created scenario: {0} (ID: {1})", name, id);
    return scenario;
  }

  public Optional<ScenarioData> updateScenario(Long id, String name, int numberOfSlots, int maxRounds, double epsilon) {
    ScenarioData existing = scenarios.get(id);
    if (existing == null) {
      return Optional.empty();
    }
    ScenarioData updated = new ScenarioData(id, name, numberOfSlots, maxRounds, epsilon, existing.agents());
    scenarios.put(id, updated);
    LOG.infov("Updated scenario: {0} (ID: {1}, Epsilon: {2})", name, id, epsilon);
    return Optional.of(updated);
  }

  public boolean deleteScenario(Long id) {
    // Prevent deletion of pre-loaded scenarios
    if (id <= 3) {
      LOG.warnv("Cannot delete pre-loaded scenario with ID: {0}", id);
      return false;
    }
    ScenarioData removed = scenarios.remove(id);
    if (removed != null) {
      LOG.infov("Deleted scenario: {0} (ID: {1})", removed.name(), id);
      return true;
    }
    return false;
  }

  // Agent operations
  public Optional<AgentData> addAgent(Long scenarioId, String agentName, String strategyType,
                                      String valuationType, int targetSlot, double budgetLimit) {
    ScenarioData scenario = scenarios.get(scenarioId);
    if (scenario == null) {
      return Optional.empty();
    }

    Long agentId = agentIdGenerator.getAndIncrement();
    AgentData agent = new AgentData(agentId, agentName, strategyType, valuationType, targetSlot, budgetLimit);
    ScenarioData updated = scenario.withAgent(agent);
    scenarios.put(scenarioId, updated);
    LOG.infov("Added agent {0} to scenario {1}", agentName, scenario.name());
    return Optional.of(agent);
  }

  public boolean deleteAgent(Long scenarioId, Long agentId) {
    ScenarioData scenario = scenarios.get(scenarioId);
    if (scenario == null) {
      return false;
    }

    ScenarioData updated = scenario.withoutAgent(agentId);
    scenarios.put(scenarioId, updated);
    LOG.infov("Deleted agent {0} from scenario {1}", agentId, scenario.name());
    return true;
  }

  public List<AgentData> getAgents(Long scenarioId) {
    ScenarioData scenario = scenarios.get(scenarioId);
    if (scenario == null) {
      return Collections.emptyList();
    }
    return new ArrayList<>(scenario.agents());
  }
}
