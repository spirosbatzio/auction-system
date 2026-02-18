package com.mtn.agent.api;

import com.mtn.agent.service.ScenarioService;
import com.mtn.agent.service.SimulationRunner;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Path("/api/simulation")
@Produces(MediaType.APPLICATION_JSON)
public class SimulationResource {

  private static final Logger LOG = Logger.getLogger(SimulationResource.class);

  @Inject
  SimulationRunner simulationRunner;

  @Inject
  ScenarioService scenarioService;

  private volatile boolean isRunning = false;

  @POST
  @Path("/run/{scenarioId}")
  public Response runSimulation(@PathParam("scenarioId") Long scenarioId) {
    if (isRunning) {
      return Response.status(Response.Status.CONFLICT)
              .entity(Map.of("error", "Simulation is already running"))
              .build();
    }

    if (scenarioService.getScenario(scenarioId).isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
              .entity(Map.of("error", "Scenario not found"))
              .build();
    }

    isRunning = true;

    // Run simulation asynchronously
    CompletableFuture.runAsync(() -> {
      try {
        simulationRunner.runInMemoryScenario(scenarioId);
      } finally {
        isRunning = false;
      }
    });

    return Response.accepted()
            .entity(Map.of("message", "Simulation started", "scenarioId", scenarioId))
            .build();
  }

  @GET
  @Path("/status")
  public Response getStatus() {
    return Response.ok(Map.of(
            "isRunning", isRunning,
            "hasResults", !simulationRunner.getStatsHistory().isEmpty()
    )).build();
  }

  @GET
  @Path("/results")
  public Response getResults() {
    if (simulationRunner.getStatsHistory().isEmpty()) {
      return Response.status(Response.Status.NO_CONTENT)
              .entity(Map.of("message", "No simulation results available"))
              .build();
    }

    return Response.ok(Map.of(
            "stats", simulationRunner.getStatsHistory(),
            "bids", simulationRunner.getBidHistory(),
            "finalItems", simulationRunner.getFinalItems()
    )).build();
  }
}
