package com.mtn.agent.api;

import com.mtn.agent.domain.AuctionState;
import com.mtn.agent.domain.Bid;
import com.mtn.agent.service.AuctioneerService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("auction")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuctionResource {

  @Inject
  AuctioneerService auctioneer;

  @GET
  public AuctionState getState() {
    return auctioneer.getState();
  }

  @POST
  @Path("/bid")
  public void submitBid(Bid bid) {
    auctioneer.receiveBid(bid);
  }

  @POST
  @Path("init")
  public void init() {
    auctioneer.init();
  }

  @POST
  @Path("/resolve")
  public void resolve() {
    auctioneer.resolveRound();
  }
}
