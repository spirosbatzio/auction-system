package com.mtn.agent.domain;

public record AuctionItem(String id, double price, String currentWinner) {

  public AuctionItem withNewPrice(double newPrice, String newWinner) {
    return new AuctionItem(this.id, newPrice, newWinner);
  }
}
