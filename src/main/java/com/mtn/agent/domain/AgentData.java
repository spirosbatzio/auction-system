package com.mtn.agent.domain;

public record AgentData(
        Long id,
        String agentName,
        String strategyType, // MYOPIC, SNIPER, BUDGET, BUNDLE, FLEXIBLE
        String valuationType,
        int targetSlot, // -1 no specific target
        double budgetLimit // -1 for unlimited
) {
}
