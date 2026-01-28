# Multi-Agent Auction Simulation System

## Overview
A high-performance simulation engine for **Simultaneous Ascending Auctions (SAA)** built with **Quarkus** and **Java 21**. This project models autonomous agents competing for cloud computing resources (slots) using Game Theoretic strategies.

The system simulates realistic market dynamics such as inflation, bidding wars, and resource allocation efficiency, visualizing the results via an interactive real-time dashboard.

## Tech Stack
* **Core:** Java 21, Quarkus Framework
* **Database:** PostgreSQL (Docker)
* **Data Analysis:** Tablesaw (Dataframes)
* **Visualization:** Plotly JS (Charts & Dashboards)
* **Build Tool:** Maven

## Key Features

### Agent Strategies
The system implements five distinct bidding strategies:
1.  **Myopic (Greedy):** Bids aggressively on any item with positive utility ($Valuation - Price > 0$), ignoring budget constraints.
2.  **Budget Constrained:** Behaves like Myopic but strictly respects a wallet limit (e.g., `Dev_Test_Env` stops bidding when budget is exhausted).
3.  **Bundle (All-or-Nothing):** Bids only if it can secure a specific set of items together (e.g., `Distributed_ML_Job` needs Slot 1 & 2 simultaneously).
4.  **Flexible (Load Balancer):** Targets *any* one item from a preferred set (e.g., `Web_Server_HA` needs Slot 1 OR 2).
5.  **Sniper:** Waits until the final rounds to enter the auction, attempting to secure items at lower prices.

### Scenarios
* **Scenario 1 (General Cloud):** Balanced mix of enterprise (High budget) and startup (Low budget) agents.
* **Scenario 2 (High Contention):** "Bidding War" scenario with wealthy agents causing massive price inflation and over-demand.
* **Scenario 3 (Advanced Strategy):** Complex interaction between Bundle, Flexible, and Budget-constrained agents demonstrating non-trivial market dynamics.

## Getting Started

### Prerequisites
* Java 21+
* Docker & Docker Compose
* Maven (or use the included `./mvnw` wrapper)

### 1. Start Infrastructure
Run the database container using Docker Compose:
```bash
docker-compose up -d postgres-db
```
### 2. Application Start
Start the Quarkus development mode:
```bash
./mvnw clean package quarkus:dev
```
### 3. Simulation execution
The application runs an interactive CLI. Inside the running terminal, use the sim command:
```bash
sim -id 3
```
### 4. Visualization dashboard
The dashboard provides deep insights into the auction dynamics:

1. Final Allocation Table: Shows the winners and the final clearing price for each item.

2. Revenue Convergence: A line chart showing the total Social Welfare evolution over time.

3. Market Intensity: A bar chart displaying the number of bids per round.

4. Agent Activity: Analysis of bidders aggressiveness.