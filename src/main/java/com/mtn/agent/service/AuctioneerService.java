package com.mtn.agent.service;

import com.mtn.agent.domain.AuctionItem;
import com.mtn.agent.domain.AuctionState;
import com.mtn.agent.domain.Bid;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class AuctioneerService {

  private static final Logger LOG = Logger.getLogger(AuctioneerService.class);

  private final Map<String, AuctionItem> items = new ConcurrentHashMap<>();
  private final List<Bid> currentRoundBids = Collections.synchronizedList(new ArrayList<>());

  private int round = 0;
  private boolean isActive = true;
  private double epsilon = 1.0;

  public void init() {
    init(5, 1.0);
  }

  public void init(int numberOfSlots) {
    init(numberOfSlots, 1.0);
  }

  public void init(int numberOfSlots, double epsilon) {
    items.clear();
    currentRoundBids.clear();

    this.round = 0;
    this.isActive = true;
    this.epsilon = epsilon;

    for (int i = 1; i <= numberOfSlots; i++) {
      String slotId = "SLOT_" + i;
      items.put(slotId, new AuctionItem(slotId, 0.0, null));
    }

    LOG.infov("--- AUCTION INITIALIZED with {0} Slots, Epsilon: {1} ---", numberOfSlots, epsilon);
  }


  public AuctionState getState() {
    return new AuctionState(new ArrayList<>(items.values()), isActive, round);
  }

  public void receiveBid(Bid bid) {
    if (!isActive) {
      LOG.warnv("Auction is over, rejected bid from agent: {0}", bid.agentId());
      return;
    }
    currentRoundBids.add(bid);
  }

  public synchronized void resolveRound() {
    if (!isActive) return;

    round++;
    LOG.infov("--- RESOLVING ROUND {0} ---", round);
    LOG.infov("Bids received: {0}", currentRoundBids.size());

    Map<String, List<Bid>> bidsPerItem = new HashMap<>();
    for (Bid b : currentRoundBids) {
      bidsPerItem.computeIfAbsent(b.itemId(), k -> new ArrayList<>()).add(b);
    }

    boolean somethingChanged = false;

    for (String itemId : items.keySet()) {
      List<Bid> bids = bidsPerItem.getOrDefault(itemId, Collections.emptyList());
      AuctionItem item = items.get(itemId);

      if (bids.isEmpty()) {
        continue;

      } else if (bids.size() == 1) {

        Bid winnerBid = bids.getFirst();
        double newPrice = Math.max(item.price(), winnerBid.amount());


        if (!winnerBid.agentId().equals(item.currentWinner()) || newPrice > item.price()) {
          items.put(itemId, item.withNewPrice(newPrice, winnerBid.agentId()));
          somethingChanged = true;
          LOG.infov("-> Item {0} won by {1} at price {2}", itemId, winnerBid.agentId(), newPrice);
        }

      } else {

        double newPrice = item.price() + epsilon;

        items.put(itemId, item.withNewPrice(newPrice, null));

        somethingChanged = true;
        LOG.infov("-> Item {0} OVER-DEMAND! Price increased to {1}", itemId, newPrice);
      }
    }

    currentRoundBids.clear();

    if (!somethingChanged) {
      isActive = false;
      LOG.info("AUCTION TERMINATED");
    }
  }
}