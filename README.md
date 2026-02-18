# Multi-Agent Auction Simulation System

## Overview

A high-performance simulation engine for **Simultaneous Ascending Auctions (SAA)**. 
his project models autonomous agents competing for cloud computing resources (slots) using Game Theoretic strategies, with real-time analysis of Nash equilibrium convergence and Pareto efficiency.

This project was developed as an assignment (but mostly for fun) for the **Intelligent Agents and Multi agent Systems** course of the [MSc in Artificial Intelligence](https://msc-ai.iit.demokritos.gr/), offered jointly by the University of Piraeus and NCSR "Demokritos".

**Authors:** Kostis Matzorakis, George Manthos, Spiros Batziopoulos

The system simulates realistic market dynamics such as price inflation, bidding wars, and resource allocation efficiency, providing comprehensive game-theoretic analysis through an interactive dashboard and REST API.

## Theoretical Foundation

### Simultaneous Ascending Auctions (SAA)

Simultaneous Ascending Auctions are a class of auction mechanisms where multiple items are auctioned simultaneously over multiple rounds. In each round:

1. **Bidding Phase**: Agents submit bids on items they value
2. **Resolution Phase**: The auctioneer processes bids:
   - **Single Bid**: Item assigned to the bidder at their bid price
   - **Multiple Bids**: Price increases (over-demand), no winner assigned yet
   - **No Bids**: Item remains with current winner (if any)

3. **Termination**: Auction ends when no agent submits bids (equilibrium reached) or maximum rounds exceeded

### Game Theory Concepts

#### Nash Equilibrium

A **Nash Equilibrium** is a state where no agent can improve their utility by unilaterally changing their strategy, given the strategies of other agents. In our auction:

- **Nash Equilibrium**: No agent can improve by changing their bid
- **Not Nash Equilibrium**: At least one agent can improve their utility by bidding differently

The system tracks Nash equilibrium convergence over rounds, showing:
- Binary state: Equilibrium reached (1) or not (0)
- Distance metric: Number of agents who can improve (converges to 0)

#### Pareto Efficiency

**Pareto Efficiency** (or Pareto Optimality) is a state where no reallocation can make one agent better off without making another worse off. The system calculates:

- **Current Social Welfare**: Sum of all agent utilities in current allocation
- **Pareto-Optimal Welfare**: Maximum possible social welfare (theoretical upper bound)
- **Efficiency Ratio**: Current welfare / Optimal welfare (0.0 to 1.0)

A Pareto efficiency of 1.0 means the allocation is optimal; lower values indicate potential improvements.

#### Agent Utility

Each agent's utility is calculated as:
```
Utility = Total Valuation of Won Items - Total Price Paid
```

Agents bid to maximize their utility, leading to strategic interactions and convergence to equilibrium.

## Tech Stack

* **Core:** Java 21, Quarkus Framework
* **Storage:** In-Memory (no database required)
* **Data Analysis:** Tablesaw (Dataframes)
* **Visualization:** Plotly JS (Charts & Dashboards)
* **API:** REST (JAX-RS)
* **Build Tool:** Maven

## Architecture

### System Components

```
┌─────────────────────────────────────────┐
│         REST API Layer                  │
│  /api/scenarios, /api/simulation, etc.  │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│      Simulation Runner                  │
│  - Manages auction rounds               │
│  - Tracks equilibrium metrics           │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│      Auctioneer Service                 │
│  - Receives bids                        │
│  - Resolves rounds                      │
│  - Manages item prices                  │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│      Agent Services                     │
│  - Implement bidding strategies         │
│  - Calculate utility                    │
│  - Make bidding decisions               │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│   Equilibrium Analysis Service          │
│  - Nash equilibrium detection           │
│  - Pareto efficiency calculation        │
│  - Agent payoff computation             │
└─────────────────────────────────────────┘
```

### How It Works

1. **Scenario Initialization**: Load scenario configuration (number of slots, agents, strategies)
2. **Agent Creation**: Each agent is initialized with:
   - Valuation function (how much they value each slot)
   - Bidding strategy (how they decide to bid)
   - Budget constraints (if applicable)

3. **Auction Execution**:
   - **Round Loop**: For each round until termination:
     - Agents evaluate current state and decide bids
     - Auctioneer collects bids
     - Auctioneer resolves round (updates prices/winners)
     - Equilibrium metrics calculated and stored
   - **Termination**: When no bids submitted (Nash equilibrium) or max rounds reached

4. **Analysis**: Post-auction analysis of:
   - Final allocations
   - Nash equilibrium status
   - Pareto efficiency
   - Agent payoffs and utilities

## Key Features

### Agent Strategies

The system implements five distinct bidding strategies:

1. **Myopic (Greedy)**: Bids aggressively on any item with positive utility ($Valuation - Price > 0$), ignoring budget constraints. Maximizes immediate utility.

2. **Budget Constrained**: Behaves like Myopic but strictly respects a wallet limit. Stops bidding when budget exhausted.

3. **Bundle (All-or-Nothing)**: Bids only if it can secure a specific set of items together. Requires all items in bundle to be affordable.

4. **Flexible (Load Balancer)**: Targets *any* one item from a preferred set. Satisfied if it wins any item it values.

5. **Sniper**: Waits until later rounds to enter the auction, attempting to secure items at lower prices. Strategic delay tactic.

### Valuation Types

Agents can have different valuation functions:

- **RICH**: High valuations (30-50) for multiple slots
- **POOR**: Low valuations (5-15) for multiple slots  
- **FOCUSED**: Very high valuation (100) for one specific slot
- **RANDOM**: Random valuations (10-30) for ~50% of slots
- **BUNDLE_PAIR**: Values two specific slots together (e.g., SLOT_1 + SLOT_2)
- **FLEXIBLE_PAIR**: Values either of two slots (e.g., SLOT_1 OR SLOT_2)

### Pre-Loaded Scenarios

1. **Scenario 1: General Purpose Cloud**
   - 5 slots, 50 max rounds
   - Mix of RICH, POOR, SNIPER, and FOCUSED agents
   - Demonstrates balanced market dynamics

2. **Scenario 2: HPC Cluster Congestion**
   - 10 slots, 100 max rounds
   - Multiple RICH agents causing bidding wars
   - High contention scenario

3. **Scenario 3: Mixed Workload Optimization**
   - 5 slots, 100 max rounds
   - FLEXIBLE, BUNDLE, and BUDGET strategies
   - Complex strategic interactions

## Getting Started

### Prerequisites

* Java 21+
* Maven (or use the included `./mvnw` wrapper)
* Docker (optional, for containerized deployment)

### Quick Start

1. **Start the Application**:
   ```bash
   ./mvnw quarkus:dev
   ```
   The application will start at `http://localhost:8080`

2. **Run a Simulation via API**:
   ```bash
   # Run Scenario 1
   curl -X POST http://localhost:8080/api/simulation/run/1
   
   # Wait a few seconds, then check results
   curl http://localhost:8080/api/simulation/results
   ```

3. **View Dashboard**:
   Open `http://localhost:8080/plot` in your browser to see:
   - Final allocation results
   - Revenue evolution charts
   - Market intensity analysis
   - **Nash equilibrium convergence**
   - **Pareto efficiency tracking**
   - Agent payoffs and utilities

## API Documentation

### Scenarios

- `GET /api/scenarios` - List all scenarios
- `GET /api/scenarios/{id}` - Get scenario details
- `POST /api/scenarios` - Create new scenario
- `PUT /api/scenarios/{id}` - Update scenario
- `DELETE /api/scenarios/{id}` - Delete scenario

### Agents

- `GET /api/scenarios/{id}/agents` - List agents in scenario
- `POST /api/scenarios/{id}/agents` - Add agent to scenario
- `DELETE /api/scenarios/{scenarioId}/agents/{agentId}` - Remove agent

### Simulation

- `POST /api/simulation/run/{scenarioId}` - Start simulation
- `GET /api/simulation/status` - Check simulation status
- `GET /api/simulation/results` - Get simulation results

### Equilibrium Analysis

- `GET /api/equilibrium/nash` - Get Nash equilibrium analysis
- `GET /api/equilibrium/pareto` - Get Pareto efficiency analysis
- `GET /api/equilibrium/analysis` - Get complete analysis

### Dashboard

- `GET /plot` - Interactive dashboard with charts and metrics

## Dashboard Features

The dashboard provides comprehensive analysis:

### 1. Final Allocation Results
Table showing winners and final prices for each item.

### 2. Revenue Evolution
Line chart tracking total revenue (social welfare proxy) over rounds.

### 3. Market Intensity
Bar chart showing number of bids per round, indicating auction activity.

### 4. Agent Bidding Activity
Bar chart comparing total bids per agent, showing bidding aggressiveness.

### 5. Nash Equilibrium Convergence
Line chart showing:
- Binary indicator: 0 (not Nash) → 1 (Nash equilibrium)
- Convergence point when equilibrium is reached

### 6. Agents Who Can Improve
Line chart tracking the number of agents who can improve by changing strategy:
- Starts high (many agents can improve)
- Gradually decreases as auction progresses
- Reaches 0 when Nash equilibrium is achieved

### 7. Pareto Efficiency Convergence
Line chart showing efficiency ratio over rounds:
- Ratio of current social welfare to Pareto-optimal welfare
- Values between 0.0 and 1.0
- Higher values indicate more efficient allocations

### 8. Agent Payoffs & Utilities
Table showing for each agent:
- Items won
- Total valuation
- Total price paid
- Utility (valuation - price paid)
- Color-coded: Green (positive utility), Red (negative utility)

### 9. Equilibrium Status Cards
- **Nash Equilibrium**: Shows if final state is Nash equilibrium
- **Pareto Efficiency**: Shows efficiency ratio and optimal welfare comparison

## Example Usage

### Create and Run Custom Scenario

```bash
# 1. Create a scenario
curl -X POST http://localhost:8080/api/scenarios \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Test Scenario",
    "numberOfSlots": 5,
    "maxRounds": 50
  }'

# Response: {"id": 100, "name": "My Test Scenario", ...}

# 2. Add agents
curl -X POST http://localhost:8080/api/scenarios/100/agents \
  -H "Content-Type: application/json" \
  -d '{
    "agentName": "TestAgent1",
    "strategyType": "MYOPIC",
    "valuationType": "RICH",
    "targetSlot": -1,
    "budgetLimit": -1.0
  }'

# 3. Run simulation
curl -X POST http://localhost:8080/api/simulation/run/100

# 4. Check equilibrium analysis
curl http://localhost:8080/api/equilibrium/analysis
```

## Docker Deployment

### Build Docker Image

```bash
# Build the application
./mvnw clean package

# Build Docker image
docker build -f src/main/docker/Dockerfile.jvm -t auction-system:latest .
```

### Run Container

```bash
docker run -p 8080:8080 auction-system:latest
```

The application uses in-memory storage, so no database container is required!

## Key Insights from Convergence Analysis

### Nash Equilibrium Convergence

- **Binary Nature**: Nash equilibrium is a discrete state (0 or 1), not continuous
- **Convergence Pattern**: The number of agents who can improve decreases gradually
- **Termination**: Auction naturally terminates when Nash equilibrium is reached (no bids = equilibrium)

### Pareto Efficiency

- **Fluctuation**: Efficiency ratio fluctuates during auction as allocations change
- **Realistic Behavior**: Perfect efficiency (1.0) is rarely achieved in practice
- **Trade-offs**: Nash equilibrium and Pareto optimality may not coincide

### Strategic Behavior

- **Myopic Agents**: Tend to bid aggressively, driving prices up
- **Sniper Agents**: Wait for opportunities, may enter late
- **Budget Constraints**: Limit bidding power, affecting final allocations
- **Bundle Strategies**: Require coordination, may fail if items too expensive

## Research Applications

This system can be used to study:

- **Mechanism Design**: How auction rules affect outcomes
- **Strategy Analysis**: Which strategies lead to better outcomes
- **Efficiency Trade-offs**: Relationship between Nash equilibrium and Pareto efficiency
- **Market Dynamics**: Price formation and convergence behavior
- **Multi-Agent Systems**: Coordination and competition in resource allocation

## Future Enhancements

Potential extensions:

- Learning agents (reinforcement learning strategies)
- Different auction mechanisms (Vickrey, Dutch, etc.)
- Network effects (valuations depend on other agents' allocations)
- Dynamic valuations (valuations change over time)
- Multi-unit auctions (agents can win multiple units)

---

