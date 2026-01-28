package com.mtn.agent.service;

import com.mtn.agent.domain.AuctionItem;
import com.mtn.agent.domain.BidRecord;
import com.mtn.agent.domain.RoundStat;
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

  public String generateDashboard(List<RoundStat> stats, List<BidRecord> bids, List<AuctionItem> finalItems) {

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

    StringBuilder htmlTable = new StringBuilder();
    htmlTable.append("<table class='styled-table'>");
    htmlTable.append("<thead><tr><th>Item</th><th>Winner</th><th>Final Price</th></tr></thead><tbody>");

    double totalRevenue = 0;
    for (AuctionItem item : finalItems) {
      String winner = (item.currentWinner() == null) ? "<span style='color:red'>UNSOLD</span>" : item.currentWinner();
      htmlTable.append(String.format("<tr><td>%s</td><td>%s</td><td>%.2f €</td></tr>", item.id(), winner, item.price()));
      totalRevenue += item.price();
    }
    htmlTable.append(String.format("<tr style='font-weight:bold; background:#f3f3f3'><td>TOTAL</td><td>-</td><td>%.2f €</td></tr>", totalRevenue));
    htmlTable.append("</tbody></table>");


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
                    \s
                     /* Table Styles */
                     .styled-table { width: 100%%; border-collapse: collapse; margin: 0 auto; font-size: 0.9em; font-family: sans-serif; box-shadow: 0 0 20px rgba(0, 0, 0, 0.15); }
                     .styled-table thead tr { background-color: #009879; color: #ffffff; text-align: left; }
                     .styled-table th, .styled-table td { padding: 12px 15px; border-bottom: 1px solid #dddddd; }
                     .styled-table tbody tr:nth-of-type(even) { background-color: #f3f3f3; }
                     .styled-table tbody tr:last-of-type { border-bottom: 2px solid #009879; }
                 </style>
             </head>
             <body>
                 <h1>Auction Simulation Dashboard</h1>
            
                 <div class="grid">
                     <div class="card full-width">
                         <h3 style="text-align:center">Final Allocation Results</h3>
                         %s
                     </div>
            
                     <div class="card"><div id="%s"></div></div>
                     <div class="card"><div id="%s"></div></div>
            
                     <div class="card full-width">
                         <div id="%s"></div>
                     </div>
                 </div>
            
                 %s %s %s
             </body>
             </html>
            \s""".formatted(
            htmlTable.toString(),
            id1, id2, id3,
            revenueFig.asJavascript(id1).replace("<script>", "<script>"),
            bidsFig.asJavascript(id2),
            agentFig.asJavascript(id3)
    ).replace("<script><script>", "<script>").replace("</script></script>", "</script>");
  }

  private String cleanUUID() {
    return "chart_" + UUID.randomUUID().toString().replace("-", "");
  }
}