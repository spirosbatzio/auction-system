package com.mtn.agent.client;

import com.mtn.agent.domain.AuctionState;
import com.mtn.agent.domain.Bid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("auction")
@RegisterRestClient(configKey = "auction-api")
public interface AuctionClient {

  @GET
  AuctionState getState();

  @POST
  @Path("bid")
  void submitBid(Bid bid);
}
