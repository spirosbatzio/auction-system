package com.mtn.agent.domain;

import java.util.List;

public record AuctionState(List<AuctionItem> items, boolean isActive, int round) {
}
