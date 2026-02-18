package com.mtn.agent.domain;

import java.util.ArrayList;
import java.util.List;

public record ScenarioData(
        Long id,
        String name,
        int numberOfSlots,
        int maxRounds,
        double epsilon,
        List<AgentData> agents
) {

  public ScenarioData {
    agents = agents != null ? agents : new ArrayList<AgentData>();
  }

  public ScenarioData withAgent(AgentData agent) {
    List<AgentData> newAgents = new ArrayList<>(this.agents);
    newAgents.add(agent);
    return new ScenarioData(id, name, numberOfSlots, maxRounds, epsilon, newAgents);
  }

  public ScenarioData withoutAgent(Long agentId) {
    List<AgentData> newAgents = new ArrayList<>(this.agents);
    newAgents.removeIf(a -> a.id().equals(agentId));
    return new ScenarioData(id, name, numberOfSlots, maxRounds, epsilon, newAgents);
  }
}
