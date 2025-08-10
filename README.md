# Summary
● Integrated the popular tabletop game "Ticket to Ride" into TAG, a Java based framework for AI
research.

● Analysed metric data from thousands of simulated games using AI players, which resulted in
valuable insight into advantageous strategies

# Ticket to Ride GUI:

<img width="635" height="452" alt="image" src="https://github.com/user-attachments/assets/13fdc197-aa41-4f25-8ffc-d4757293e3d8" />


# Key Findings

## AI Agent Performance
 - MCTS won 95% of five player games and 90% of four player games.
 - This win rate for MCTS fell to 36% when in 2 player games.
 - Long-term planning gave MCTS the edge over OSLA, though both used the same heuristic.


## Player Count Impact
- Double routes shift outcomes. MCTS wins 57% of three player games, 90% of four player games once double tracks open.
- Greedy agents such as my rule based agents MLRA and SRA played better in games with less players
- In five player games, all agents proportionally drew more train cards than two player games.


## Strategic Insights
- Random player lost 138 - 142 points on unfinished destination cards, showing Ticket to Ride punishes hoarding destination ticket cards
- MLRA (Mid Length Route agent) beat SRA (Short Route agent) consistently - long routes are more valuable than short ones.
- MCTS played destination cards better than the rest - the gap in final scores shows this bump in score when destination points are calculated.


## Computational Performance
- MCTS demands heavy computation. Agents need up to 3.4 s for a move in five player games. Time rises in direct proportion to player count.
-  The state space keeps 146 components at any count - game states climb from 74.6 with two players to 185.7 with five.

