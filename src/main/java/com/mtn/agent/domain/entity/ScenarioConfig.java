package com.mtn.agent.domain.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.HashSet;
import java.util.Set;

@Entity
public class ScenarioConfig extends PanacheEntity {

  public String name;
  public int numberOfSlots;
  public int maxRounds;

  @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL)
  public Set<AgentConfig> agents = new HashSet<>();

  public static ScenarioConfig findByName(String name) {
    return find("name", name).firstResult();
  }
}
