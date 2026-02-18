package com.mtn.agent.domain;

public record EquilibriumRoundStat(
        int round,
        boolean isNashEquilibrium,
        int agentsWhoCanImprove,
        double paretoEfficiencyRatio,
        double socialWelfare
) {
}
