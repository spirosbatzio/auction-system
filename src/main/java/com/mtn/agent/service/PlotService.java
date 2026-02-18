package com.mtn.agent.service;

import com.mtn.agent.domain.AgentPayoff;
import com.mtn.agent.domain.AuctionItem;
import com.mtn.agent.domain.BidRecord;
import com.mtn.agent.domain.EquilibriumRoundStat;
import com.mtn.agent.domain.RoundStat;
import com.mtn.agent.service.EquilibriumAnalysisService.NashEquilibriumResult;
import com.mtn.agent.service.EquilibriumAnalysisService.ParetoEfficiencyResult;
import jakarta.enterprise.context.ApplicationScoped;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.components.Line;
import tech.tablesaw.plotly.components.Marker;
import tech.tablesaw.plotly.traces.BarTrace;
import tech.tablesaw.plotly.traces.ScatterTrace;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class PlotService {

  public String generateDashboard(
          List<RoundStat> stats,
          List<BidRecord> bids,
          List<AuctionItem> finalItems,
          NashEquilibriumResult nashResult,
          ParetoEfficiencyResult paretoResult,
          List<EquilibriumRoundStat> equilibriumHistory) {

    Table statsTable = Table.create("Stats")
            .addColumns(
                    IntColumn.create("Round", stats.stream().mapToInt(RoundStat::round).toArray()),
                    DoubleColumn.create("Revenue", stats.stream().mapToDouble(RoundStat::revenue).toArray()),
                    IntColumn.create("Bids", stats.stream().mapToInt(RoundStat::totalBids).toArray())
            );

    Map<String, Long> bidsPerAgent = bids.stream()
            .collect(Collectors.groupingBy(BidRecord::agentName, Collectors.counting()));

    Table agentTable = Table.create("AgentActivity")
            .addColumns(
                    StringColumn.create("Agent", bidsPerAgent.keySet().stream().toList()),
                    DoubleColumn.create("Count", bidsPerAgent.values().stream().mapToDouble(Long::doubleValue).toArray())
            );


    ScatterTrace revenueTrace = ScatterTrace.builder(
                    statsTable.numberColumn("Round"), statsTable.numberColumn("Revenue"))
            .mode(ScatterTrace.Mode.LINE_AND_MARKERS)
            .name("Revenue")
            .line(Line.builder().color("teal").width(3).build())
            .build();

    Figure revenueFig = new Figure(
            Layout.builder().title("1. Revenue Evolution").xAxis(Axis.builder().title("Round").build()).height(400).build(),
            revenueTrace
    );

    BarTrace bidsTrace = BarTrace.builder(
                    statsTable.intColumn("Round").asStringColumn(), statsTable.numberColumn("Bids"))
            .marker(Marker.builder().color("salmon").build())
            .build();

    Figure bidsFig = new Figure(
            Layout.builder().title("2. Market Intensity").xAxis(Axis.builder().title("Round").build()).height(400).build(),
            bidsTrace
    );

    BarTrace agentTrace = BarTrace.builder(
                    agentTable.stringColumn("Agent"), agentTable.numberColumn("Count"))
            .marker(Marker.builder().color("#6610f2").build())
            .build();

    Figure agentFig = new Figure(
            Layout.builder().title("3. Agents bids").height(400).build(),
            agentTrace
    );

    StringBuilder allocationTable = new StringBuilder();
    allocationTable.append("<table class='styled-table'>");
    allocationTable.append("<thead><tr><th>Item</th><th>Winner</th><th>Final Price</th></tr></thead><tbody>");

    double totalRevenue = 0;
    for (AuctionItem item : finalItems) {
      String winner = (item.currentWinner() == null) ? "<span style='color:red'>UNSOLD</span>" : item.currentWinner();
      allocationTable.append(String.format("<tr><td>%s</td><td>%s</td><td>%.2f €</td></tr>", item.id(), winner, item.price()));
      totalRevenue += item.price();
    }
    allocationTable.append(String.format("<tr style='font-weight:bold; background:#f3f3f3'><td>TOTAL</td><td>-</td><td>%.2f €</td></tr>", totalRevenue));
    allocationTable.append("</tbody></table>");

    StringBuilder payoffTable = new StringBuilder();
    if (nashResult != null && nashResult.payoffs() != null) {
      payoffTable.append("<table class='styled-table'>");
      payoffTable.append("<thead><tr><th>Agent</th><th>Items Won</th><th>Total Valuation</th><th>Total Paid</th><th>Utility</th></tr></thead><tbody>");

      for (AgentPayoff payoff : nashResult.payoffs().values()) {
        String utilityColor = payoff.utility() >= 0 ? "green" : "red";
        payoffTable.append(String.format(
                "<tr><td>%s</td><td>%d</td><td>%.2f</td><td>%.2f</td><td style='color:%s; font-weight:bold'>%.2f</td></tr>",
                payoff.agentId(),
                payoff.itemsWon(),
                payoff.totalValuation(),
                payoff.totalPricePaid(),
                utilityColor,
                payoff.utility()
        ));
      }
      payoffTable.append("</tbody></table>");
    }

    String nashStatusHtml = "";
    if (nashResult != null) {
      String nashStatus = nashResult.isNashEquilibrium()
              ? "<span style='color:green; font-weight:bold; font-size:1.2em'>✓ NASH EQUILIBRIUM</span>"
              : "<span style='color:orange; font-weight:bold; font-size:1.2em'>⚠ NOT NASH EQUILIBRIUM</span>";

      String agentsWhoCanImprove = nashResult.agentsWhoCanImprove().isEmpty()
              ? "<p style='color:green'>No agent can improve by changing strategy.</p>"
              : "<p style='color:orange'>Agents who can improve: <strong>" +
              String.join(", ", nashResult.agentsWhoCanImprove()) + "</strong></p>";

      nashStatusHtml = String.format("""
          <div class="equilibrium-card" style="padding:20px; background:%s; border-radius:8px; margin:10px 0;">
              <h3 style="margin-top:0;">Nash Equilibrium Analysis</h3>
              <div style="font-size:1.1em; margin:15px 0;">%s</div>
              %s
          </div>
          """,
              nashResult.isNashEquilibrium() ? "#e8f5e9" : "#fff3e0",
              nashStatus,
              agentsWhoCanImprove
      );
    }

    String paretoStatusHtml = "";
    if (paretoResult != null) {
      String paretoStatus = paretoResult.isParetoOptimal()
              ? "<span style='color:green; font-weight:bold; font-size:1.2em'>✓ PARETO OPTIMAL</span>"
              : "<span style='color:blue; font-weight:bold; font-size:1.2em'>ℹ PARETO EFFICIENCY: %.1f%%</span>";

      paretoStatusHtml = String.format("""
          <div class="equilibrium-card" style="padding:20px; background:#e3f2fd; border-radius:8px; margin:10px 0;">
              <h3 style="margin-top:0;">Pareto Efficiency Analysis</h3>
              <div style="font-size:1.1em; margin:15px 0;">%s</div>
              <div style="margin-top:10px;">
                  <p><strong>Current Social Welfare:</strong> %.2f</p>
                  <p><strong>Pareto-Optimal Welfare:</strong> %.2f</p>
                  <p><strong>Efficiency Ratio:</strong> <span style="font-size:1.2em; color:%s">%.1f%%</span></p>
              </div>
          </div>
          """,
              paretoResult.isParetoOptimal()
                      ? "<span style='color:green; font-weight:bold; font-size:1.2em'>✓ PARETO OPTIMAL</span>"
                      : String.format("<span style='color:blue; font-weight:bold; font-size:1.2em'>ℹ PARETO EFFICIENCY: %.1f%%</span>",
                      paretoResult.efficiencyRatio() * 100),
              paretoResult.currentSocialWelfare(),
              paretoResult.paretoOptimalWelfare(),
              paretoResult.efficiencyRatio() >= 0.9 ? "green" : paretoResult.efficiencyRatio() >= 0.7 ? "orange" : "red",
              paretoResult.efficiencyRatio() * 100
      );
    }

    String convergenceCardsHtml = "";
    String convergenceScriptsHtml = "";

    if (equilibriumHistory != null && !equilibriumHistory.isEmpty()) {
      Table convTable = Table.create("EquilibriumConvergence")
              .addColumns(
                      IntColumn.create("Round", equilibriumHistory.stream().mapToInt(EquilibriumRoundStat::round).toArray()),
                      IntColumn.create("IsNash", equilibriumHistory.stream().mapToInt(e -> e.isNashEquilibrium() ? 1 : 0).toArray()),
                      IntColumn.create("AgentsImprove", equilibriumHistory.stream().mapToInt(EquilibriumRoundStat::agentsWhoCanImprove).toArray()),
                      DoubleColumn.create("ParetoEff", equilibriumHistory.stream().mapToDouble(EquilibriumRoundStat::paretoEfficiencyRatio).toArray())
              );

      ScatterTrace nashConvTrace = ScatterTrace.builder(
                      convTable.numberColumn("Round"), convTable.numberColumn("IsNash"))
              .mode(ScatterTrace.Mode.LINE_AND_MARKERS)
              .name("Nash (1=yes, 0=no)")
              .line(Line.builder().color("green").width(2).build())
              .marker(Marker.builder().color("green").size(7).build())
              .build();

      Figure nashConvFig = new Figure(
              Layout.builder()
                      .title("4. Nash Equilibrium Convergence")
                      .xAxis(Axis.builder().title("Round").build())
                      .yAxis(Axis.builder().title("Nash (1/0)").build())
                      .height(350)
                      .build(),
              nashConvTrace
      );

      ScatterTrace improveConvTrace = ScatterTrace.builder(
                      convTable.numberColumn("Round"), convTable.numberColumn("AgentsImprove"))
              .mode(ScatterTrace.Mode.LINE_AND_MARKERS)
              .name("Agents who can improve")
              .line(Line.builder().color("orange").width(2).build())
              .marker(Marker.builder().color("orange").size(6).build())
              .build();

      Figure improveConvFig = new Figure(
              Layout.builder()
                      .title("5. Nash Distance: Agents Who Can Improve")
                      .xAxis(Axis.builder().title("Round").build())
                      .yAxis(Axis.builder().title("Count").build())
                      .height(350)
                      .build(),
              improveConvTrace
      );

      ScatterTrace paretoConvTrace = ScatterTrace.builder(
                      convTable.numberColumn("Round"), convTable.numberColumn("ParetoEff"))
              .mode(ScatterTrace.Mode.LINE_AND_MARKERS)
              .name("Pareto efficiency ratio")
              .line(Line.builder().color("royalblue").width(2).build())
              .marker(Marker.builder().color("royalblue").size(6).build())
              .build();

      Figure paretoConvFig = new Figure(
              Layout.builder()
                      .title("6. Pareto Efficiency Convergence")
                      .xAxis(Axis.builder().title("Round").build())
                      .yAxis(Axis.builder().title("Efficiency ratio").build())
                      .height(350)
                      .build(),
              paretoConvTrace
      );

      String id4 = cleanUUID();
      String id5 = cleanUUID();
      String id6 = cleanUUID();

      convergenceCardsHtml = String.format("""
          <div class="card"><div id="%s"></div></div>
          <div class="card"><div id="%s"></div></div>
          <div class="card full-width"><div id="%s"></div></div>
          """, id4, id5, id6);

      convergenceScriptsHtml = String.format("%s %s %s",
              nashConvFig.asJavascript(id4).replace("<script>", "<script>"),
              improveConvFig.asJavascript(id5),
              paretoConvFig.asJavascript(id6)
      );
    }

    String id1 = cleanUUID();
    String id2 = cleanUUID();
    String id3 = cleanUUID();

    return """
        <html>
        <head>
            <title>Advanced Auction Dashboard</title>
            <script src="https://cdn.plot.ly/plotly-2.27.0.min.js"></script>
            <style>
                body { font-family: 'Segoe UI', sans-serif; background: #f8f9fa; padding: 20px; }
                h1 { text-align: center; color: #343a40; margin-bottom: 40px; }
                .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; max-width: 1400px; margin: 0 auto; }
                .card { background: white; padding: 15px; border-radius: 10px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); }
                .full-width { grid-column: span 2; }

                /* Table Styles */
                .styled-table { width: 100%%; border-collapse: collapse; margin: 0 auto; font-size: 0.9em; font-family: sans-serif; box-shadow: 0 0 20px rgba(0, 0, 0, 0.15); }
                .styled-table thead tr { background-color: #009879; color: #ffffff; text-align: left; }
                .styled-table th, .styled-table td { padding: 12px 15px; border-bottom: 1px solid #dddddd; }
                .styled-table tbody tr:nth-of-type(even) { background-color: #f3f3f3; }
                .styled-table tbody tr:last-of-type { border-bottom: 2px solid #009879; }

                .equilibrium-card { border-left: 4px solid #009879; }
            </style>
        </head>
        <body>
            <h1>Auction Simulation Dashboard</h1>

            <div class="grid">
                <div class="card full-width">
                    <h3 style="text-align:center">Final Allocation Results</h3>
                    %s
                </div>

                %s
                %s

                <div class="card full-width">
                    <h3 style="text-align:center">Agent Payoffs & Utilities</h3>
                    %s
                </div>

                <div class="card"><div id="%s"></div></div>
                <div class="card"><div id="%s"></div></div>

                <div class="card full-width">
                    <div id="%s"></div>
                </div>

                %s
            </div>

            %s %s %s
            %s
        </body>
        </html>
        """.formatted(
                    allocationTable.toString(),
                    nashStatusHtml,
                    paretoStatusHtml,
                    payoffTable.length() > 0 ? payoffTable.toString() : "<p>No payoff data available</p>",
                    id1, id2, id3,
                    convergenceCardsHtml,
                    revenueFig.asJavascript(id1).replace("<script>", "<script>"),
                    bidsFig.asJavascript(id2),
                    agentFig.asJavascript(id3),
                    convergenceScriptsHtml
            ).replace("<script><script>", "<script>")
            .replace("</script></script>", "</script>");
  }

  public String generateDashboard(List<RoundStat> stats, List<BidRecord> bids, List<AuctionItem> finalItems) {
    return generateDashboard(stats, bids, finalItems, null, null, null);
  }

  private String cleanUUID() {
    return "chart_" + UUID.randomUUID().toString().replace("-", "");
  }
}