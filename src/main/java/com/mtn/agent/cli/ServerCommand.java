package com.mtn.agent.cli;

import com.mtn.agent.service.AuctioneerService;
import io.quarkus.runtime.Quarkus;
import jakarta.inject.Inject;
import picocli.CommandLine;

@CommandLine.Command(name = "server", description = "Starts Auction Server")
public class ServerCommand implements Runnable {

  @Inject
  AuctioneerService auctioneer;

  @CommandLine.Option(names = {"-n", "--slots"}, defaultValue = "5")
  int slots;

  @Override
  public void run() {
    System.out.println("Starting Server with " + slots + " slots...");
    auctioneer.init(slots);
    Quarkus.waitForExit();
  }
}