# Summary
● Integrated the popular tabletop game "Ticket to Ride" into TAG, a Java based framework for AI
research.

● Analysed metric data from thousands of simulated games using AI players, which resulted in
valuable insight into advantageous strategies

# Ticket to Ride GUI:

<img width="635" height="452" alt="image" src="https://github.com/user-attachments/assets/13fdc197-aa41-4f25-8ffc-d4757293e3d8" />


# Key Findings

## AI Agent Performance
- **MCTS dominated high-player games** with **95% win rate** in 5-player games and **90%** in 4-player games
- **Performance declined with fewer players**: MCTS win rate dropped to **36%** in 2-player games
- **Long-term strategy matters**: MCTS consistently outperformed OSLA despite sharing the same heuristic function

## Player Count Impact
- **Double routes are game-changing**: Major performance shifts occurred between 3-player (**57%** MCTS win rate) and 4-player (**90%**) configurations when double routes become available
- **Greedy strategies work better with fewer players**: Rule-based agents (MLRA, SRA) showed improved performance as player count decreased
- **Resource competition increases**: All agents drew more train cards in 5-player games compared to 2-player games

## Strategic Insights
- **Destination cards are high-risk, high-reward**: Random player averaged **-138 to -142 points** from incomplete destination cards, demonstrating the penalty for poor destination card management
- **Route length optimization**: MLRA consistently outperformed SRA, supporting research that **longer routes are more valuable** than short routes
- **Delayed rewards crucial**: MCTS utilized destination cards more effectively than other agents, contributing to its superior performance

## Computational Performance
- **MCTS is computationally intensive**: Agent decision times reached up to **3.4 seconds** in 5-player games
- **Linear scaling**: Computation time increased proportionally with player count
- **State space remains constant**: **146 components** regardless of player count, though number of game states increased from **74.6** (2 players) to **185.7** (5 players)
