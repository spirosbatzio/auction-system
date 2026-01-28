package com.mtn.agent.api;

import com.mtn.agent.service.PlotService;
import com.mtn.agent.service.SimulationRunner;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/plot")
public class PlotResource {

  @Inject
  SimulationRunner runner;

  @Inject
  PlotService plotService;

  @GET
  @Produces(MediaType.TEXT_HTML)
  public String getPlot() {

    var stats = runner.getStatsHistory();
    var bids = runner.getBidHistory();
    var items = runner.getFinalItems();
    if (stats == null || stats.isEmpty()) {
      return """
                   <html><body style='text-align:center; padding:50px; font-family:sans-serif;'>
                   <h1>No Data Available</h1>
                   <p>Please run a simulation first using the command line (e.g., <code>sim -id 3</code>).</p>
                   </body></html>
                   """;
    }

    return plotService.generateDashboard(stats, bids, items);
  }
}