package com.mtn.agent.api;

import com.mtn.agent.domain.AgentData;
import com.mtn.agent.domain.ScenarioData;
import com.mtn.agent.service.ScenarioService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("api/scenarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScenarioResource {

  @Inject
  ScenarioService scenarioService;

  @GET
  public List<ScenarioData> getAllScenarios() {
    return scenarioService.getAllScenarios();
  }

  @GET
  @Path("/{id}")
  public Response getScenario(@PathParam("id") Long id) {
    Optional<ScenarioData> scenario = scenarioService.getScenario(id);
    return scenario.map(Response::ok)
            .orElse(Response.status(Response.Status.NOT_FOUND))
            .build();
  }

  @POST
  public Response createScenario(CreateScenarioRequest request) {
    if (request.name == null || request.name.isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
              .entity(Map.of("error", "Scenario name is required"))
              .build();
    }
    if (request.numberOfSlots <= 0 || request.maxRounds <= 0) {
      return Response.status(Response.Status.BAD_REQUEST)
              .entity(Map.of("error", "numberOfSlots and maxRounds must be positive"))
              .build();
    }

    double epsilon = request.epsilon != null && request.epsilon > 0
            ? request.epsilon
            : 1.0;

    ScenarioData scenario = scenarioService.createScenario(
            request.name,
            request.numberOfSlots,
            request.maxRounds,
            epsilon
    );
    return Response.status(Response.Status.CREATED).entity(scenario).build();
  }

  @PUT
  @Path("/{id}")
  public Response updateScenario(@PathParam("id") Long id, CreateScenarioRequest request) {
    double epsilon = request.epsilon != null && request.epsilon > 0
            ? request.epsilon
            : 1.0;

    Optional<ScenarioData> updated = scenarioService.updateScenario(
            id,
            request.name,
            request.numberOfSlots,
            request.maxRounds,
            epsilon
    );
    return updated.map(Response::ok)
            .orElse(Response.status(Response.Status.NOT_FOUND))
            .build();
  }

  @DELETE
  @Path("/{id}")
  public Response deleteScenario(@PathParam("id") Long id) {
    boolean deleted = scenarioService.deleteScenario(id);
    if (deleted) {
      return Response.noContent().build();
    }
    return Response.status(Response.Status.NOT_FOUND)
            .entity(Map.of("error", "Scenario not found or cannot be deleted"))
            .build();
  }

  @GET
  @Path("/{id}/agents")
  public Response getAgents(@PathParam("id") Long id) {
    List<AgentData> agents = scenarioService.getAgents(id);
    return Response.ok(agents).build();
  }

  @POST
  @Path("/{id}/agents")
  public Response addAgent(@PathParam("id") Long scenarioId, CreateAgentRequest request) {
    if (request.agentName == null || request.agentName.isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
              .entity(Map.of("error", "Agent name is required"))
              .build();
    }

    Optional<AgentData> agent = scenarioService.addAgent(
            scenarioId,
            request.agentName,
            request.strategyType,
            request.valuationType,
            request.targetSlot != null ? request.targetSlot : -1,
            request.budgetLimit != null ? request.budgetLimit : -1.0
    );

    return agent.map(a -> Response.status(Response.Status.CREATED).entity(a).build())
            .orElse(Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Scenario not found"))
                    .build());
  }

  @DELETE
  @Path("/{scenarioId}/agents/{agentId}")
  public Response deleteAgent(@PathParam("scenarioId") Long scenarioId,
                              @PathParam("agentId") Long agentId) {
    boolean deleted = scenarioService.deleteAgent(scenarioId, agentId);
    if (deleted) {
      return Response.noContent().build();
    }
    return Response.status(Response.Status.NOT_FOUND).build();
  }



  public static class CreateScenarioRequest {
    public String name;
    public int numberOfSlots;
    public int maxRounds;
    Double epsilon;
  }

  public static class CreateAgentRequest {
    public String agentName;
    public String strategyType;
    public String valuationType;
    public Integer targetSlot;
    public Double budgetLimit;
  }
}
