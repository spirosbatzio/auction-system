package com.mtn.agent.api;

import com.mtn.agent.service.EquilibriumAnalysisService;
import com.mtn.agent.service.SimulationRunner;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/api/equilibrium")
@Produces(MediaType.APPLICATION_JSON)
public class EquilibriumResource {

  @Inject
  SimulationRunner simulationRunner;

  @Inject
  EquilibriumAnalysisService equilibriumAnalysisService;

  @GET
  @Path("/nash")
  public Response getNashEquilibrium() {
    try {
      var result = simulationRunner.getNashEquilibriumResult();
      return Response.ok(result).build();
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST)
              .entity(Map.of("error", e.getMessage()))
              .build();
    }
  }

  @GET
  @Path("/pareto")
  public Response getParetoEfficiency() {
    try {
      var result = simulationRunner.getParetoEfficiencyResult();
      return Response.ok(result).build();
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST)
              .entity(Map.of("error", e.getMessage()))
              .build();
    }
  }

  @GET
  @Path("/analysis")
  public Response getFullAnalysis() {
    try {
      var nashResult = simulationRunner.getNashEquilibriumResult();
      var paretoResult = simulationRunner.getParetoEfficiencyResult();

      return Response.ok(Map.of(
              "nash", nashResult,
              "pareto", paretoResult
      )).build();
    } catch (Exception e) {
      return Response.status(Response.Status.BAD_REQUEST)
              .entity(Map.of("error", e.getMessage()))
              .build();
    }
  }
}
