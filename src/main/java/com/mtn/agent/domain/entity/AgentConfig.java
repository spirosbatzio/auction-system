package com.mtn.agent.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class AgentConfig extends PanacheEntity {

  public String agentName;
  public String strategyType;  // MYOPIC, SNIPER, BUDGET, BUNDLE, FLEXIBLE
  public String valuationType;  // RICH, POOR, FOCUSED
  public int targetSlot;

  public double budgetLimit;

  @ManyToOne
  public ScenarioConfig scenario;
}
