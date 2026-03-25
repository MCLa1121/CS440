package src.pas.uno.agents;


// SYSTEM IMPORTS
import edu.bu.pas.uno.Card;
import edu.bu.pas.uno.Game.GameView;
import edu.bu.pas.uno.Hand.HandView;
import edu.bu.pas.uno.enums.Color;
import edu.bu.pas.uno.enums.Value;
import edu.bu.pas.uno.moves.Move;
import edu.bu.pas.uno.tree.Node;

import java.util.Random;
import java.util.Set;


// JAVA PROJECT IMPORTS
import edu.bu.pas.uno.Game;
import edu.bu.pas.uno.Hand;
import edu.bu.pas.uno.agents.*;
import java.util.ArrayList;
import java.util.List;

public class ExpectedOutcomeAgent
    extends MCTSAgent
{
    //add a new field to solve a drawn card case
    //Remember the drawn card index from this search, so can use it later.
    private Integer DrawnIDx = null;
    
    //testing perpose
    // how deep to expand the explicit tree before doing a rollout
    private static final int ARTIFICIAL_LEAF_DEPTH = 3;

    // how many rollouts to do
    //private static final int NUM_ITERATIONS = 200;
    private static final int ROLLOUT = 1;
    // deadline in milliseconds — search must finish before this time
    private long searchDeadlineMS;
    

    public static class MCTSNode
        extends Node
    {
        public MCTSNode(final GameView game,
                        final int logicalPlayerIdx,
                        final Node parent)
        {
            super(game, logicalPlayerIdx, parent);
        }

        @Override
        public Node getChild(final Move move)
        {
            //create a dummy agent for the nextGame
            Agent[] dummies = ExpectedOutcomeAgent.dummy(this.getGameView());

            //so that that the nextGame agent will not be null 
            //create a copy so we dont make change to the parent
            Game nextGame = new Game(this.getGameView());

            //now figure out whose turn it is 
            int curLogicalPlayerID = this.getLogicalPlayerIdx();

            //get the current player's hand 
            Hand hand = nextGame.getHand(curLogicalPlayerID);

            //we need to know what state we are curently at
            Node.NodeState state = this.getNodeState();

            //now condider the three different cases
            //the case which the player has a legal move
            if(state == NodeState.HAS_LEGAL_MOVES){
                //use the copied agent
                Agent correctAgent = dummies[curLogicalPlayerID];
                Move correctedMove;
                
                // rebuild the move with the correct agent
                if(move.getNewColorIfWild() != null){
                    correctedMove = Move.createMove(correctAgent, move.getCardToPlayIdx(), move.getNewColorIfWild());
                } else {
                    correctedMove = Move.createMove(correctAgent, move.getCardToPlayIdx());
                }
                nextGame.resolveMove(correctedMove);
            }

            //case where we do not have a legal move
            else if(state == NodeState.NO_LEGAL_MOVES_UNRESOLVED_CARDS_PRESENT){
                //we need to draw the entire unresolved pile
                int total = nextGame.getUnresolvedCards().total();
                nextGame.drawTotal(hand, total);
                //move on to the next player 
                nextGame.resolveMove(null);
            }

            //case where no legal move but draw a card
            else if(state == NodeState.NO_LEGAL_MOVES_MAY_PLAY_DRAWN_CARD){
                //drawn one card 
                int drawnIdx = nextGame.drawCard(hand);
                //get the drawn card 
                Card drawnCard = hand.getCard(drawnIdx);

                //if the move is null, we keep the drawn card 
                if(move == null || !hand.getLegalMoves(nextGame).contains(drawnIdx)){
                    nextGame.resolveMove(null);
                }else{//otherwise we play the drawn card
                    Agent correctAgent = dummies[curLogicalPlayerID];
                    //now we need to consider whether the card is a wild card or not
                    //if it is, we need to consider the color chosen
                    Move actualMove;
                    if(drawnCard.isWild()){
                        actualMove = Move.createMove(correctAgent, drawnIdx, move.getNewColorIfWild());
                    }else{
                        actualMove = Move.createMove(correctAgent,  drawnIdx);
                    }
                    nextGame.resolveMove(actualMove);
                }
            }
            // after the action is resolved, it is now the next player's turn
            int nextLogicalPlayerIdx = nextGame.getPlayerOrder().getCurrentLogicalPlayerIdx();
            // return the child node for the new state
            return new MCTSNode(nextGame.getOmniscientView(),nextLogicalPlayerIdx,this);
        }
    }

    public ExpectedOutcomeAgent(final int playerIdx,
                                final long maxThinkingTimeInMS)
    {
        super(playerIdx, maxThinkingTimeInMS);
    }

    /**
     * A method to perform the MCTS search on the game tree
     *
     * @param   game            The {@link GameView} that should be the root of the game tree
     * @param   drawnCardIdx    This will be non-null when this method is being called by the 
     *                          <code>maybePlayDrawnCard</code> method of {@link Agent} and will
     *                          be <code>null</code> when being called by <code>chooseCardToPlay</code>
     *                          method of {@link Agent}
     * @return  The {@link Node} of the root who'se q-values should now be populated and ready to argmax
     */

    @Override
    public Node search(final GameView game,
                       final Integer drawnCardIdx)
    {
        // TODO: implement me!
        /*general idea
        1.selection 
        2.if the node is the leaf node, we do node expansion
        3.expand the tree in the normal way and use rollouts at artificial leaves to update q-values
        4.if it is a leaf node and it has been visited, rollout 
        5.for node expansion, find the first new node, the find the value of it 
        6.keep doing it until we reach the terminal state, rollout
        7.after find the value, backpropogation 
        */
        //if we start form the root, save the drawn card index
        //used for the later helper method 
        this.DrawnIDx = drawnCardIdx; 
        //first set the root node, the node that do not have a parent
        MCTSNode root = new MCTSNode(game, game.getPlayerOrder().getCurrentLogicalPlayerIdx(), null);
        
        //get the state of the root 
        Node.NodeState state = root.getNodeState() ;

        // set the search deadline — leave 30ms buffer so we return in time
        long timelimit = this.getMaxThinkingTimeInMS() - 30;
        if(timelimit < 1){
            timelimit = 1;
        }
        this.searchDeadlineMS = System.currentTimeMillis() + timelimit;

    //     // maybePlayDrawnCard case
    //     if(drawnCardIdx != null){
    //         int keepIdx = Node.NoLegalMovesIdxDefaults.DrawSingleCardIdxs.KEEP_CARD_MOVE_IDX;
    //         int playIdx = Node.NoLegalMovesIdxDefaults.DrawSingleCardIdxs.PLAY_CARD_MOVE_IDX;

    //         root.setQValueTotal(keepIdx, 0.5f);
    //         root.setQCount(keepIdx, 1);

    //         root.setQValueTotal(playIdx, 0.6f);
    //         root.setQCount(playIdx, 1);

    //         return root;
    // }

    //     //if there are legal move to play
    //     if(state == Node.NodeState.HAS_LEGAL_MOVES){
    //         // just mark the first legal move as explored
    //         root.setQValueTotal(0, 1.0f);
    //         root.setQCount(0, 1);
    //         return root;
    //     }

        

    //     // unresolved draw-pile case
    //     if(state == Node.NodeState.NO_LEGAL_MOVES_UNRESOLVED_CARDS_PRESENT){
    //         int moveIdx = Node.NoLegalMovesIdxDefaults.DrawUnresolvedCardsIdxs.MOVE_IDX;
    //         root.setQValueTotal(moveIdx, 0.5f);
    //         root.setQCount(moveIdx, 1);
    //         return root;
    // }
    evaluate(root);

    return root;
    }
    
    private float evaluate(final Node node){
        // Time check FIRST — before any recursive work.
        // If we are out of time, return the heuristic immediately
        // so we do not go over the deadline.
        if(System.currentTimeMillis() >= this.searchDeadlineMS){
            return heuristic(node);
        }

        // if this is a terminal node, return the true win/loss value
        if(node.isTerminal()){
            return reachTerminal(node.getGameView());
        }

        // if we have reached the artificial leaf depth,
        // estimate the value with the heuristic instead of expanding further
        if(node.getDepth() >= ARTIFICIAL_LEAF_DEPTH){
            return heuristic(node);
        }

        // get the state of the current node
        Node.NodeState state = node.getNodeState();

        // --- Case 1: player has legal moves ---
        if(state == Node.NodeState.HAS_LEGAL_MOVES){
            // expand every legal move and evaluate each child
            for(int moveIdx = 0; moveIdx < node.getOrderedLegalMoves().size(); moveIdx++){
                // check time before expanding each child
                if(System.currentTimeMillis() >= this.searchDeadlineMS){
                    break;
                }

                // get the card index for this move
                int cardIdx = node.getOrderedLegalMoves().get(moveIdx);

                // build the move object
                Move move = makeMove(node, cardIdx);

                // get the child node after applying this move
                Node child = node.getChild(move);

                // recursively evaluate the child
                float childValue = evaluate(child);

                // store the q-value and count for this move
                node.setQValueTotal(moveIdx, childValue);
                node.setQCount(moveIdx, 1);
            }

            // return the weighted average utility of all evaluated moves
            return node.getUtilityValues();
        }

        // --- Case 2: no legal moves, must draw unresolved pile ---
        if(state == Node.NodeState.NO_LEGAL_MOVES_UNRESOLVED_CARDS_PRESENT){
            // only one action available: draw the whole unresolved pile
            int moveIdx = Node.NoLegalMovesIdxDefaults.DrawUnresolvedCardsIdxs.MOVE_IDX;

            // get the child and evaluate it
            Node child = node.getChild(null);
            float childValue = evaluate(child);

            // store the q-value and count
            node.setQValueTotal(moveIdx, childValue);
            node.setQCount(moveIdx, 1);

            return node.getUtilityValues();
        }

        // --- Case 3: no legal moves, draw one card, then play or keep ---
        int playIdx = Node.NoLegalMovesIdxDefaults.DrawSingleCardIdxs.PLAY_CARD_MOVE_IDX;
        int keepIdx = Node.NoLegalMovesIdxDefaults.DrawSingleCardIdxs.KEEP_CARD_MOVE_IDX;

        // evaluate the "keep" branch if we still have time
        if(System.currentTimeMillis() < this.searchDeadlineMS){
            // null move means keep the drawn card
            Node keep = node.getChild(null);
            float keepValue = evaluate(keep);
            node.setQValueTotal(keepIdx, keepValue);
            node.setQCount(keepIdx, 1);
        }

        // evaluate the "play" branch if we still have time
        if(System.currentTimeMillis() < this.searchDeadlineMS){
            // build the move for playing the drawn card
            Move playDrawn = drawnMove(node);
            if(playDrawn != null){
                Node play = node.getChild(playDrawn);
                float value = evaluate(play);
                node.setQValueTotal(playIdx, value);
                node.setQCount(playIdx, 1);
            }
        }

        return node.getUtilityValues();
    }

    private float reachTerminal(final GameView game){
        // find our logical index
        int myIdx = game.getPlayerOrder().getLogicalIdx(this.getPlayerIdx());

        // we win if our hand is empty
        if(game.getHandView(myIdx).size() == 0){
            return 1.0f;
        } else {
            return 0.0f;
        }
    }

    private float heuristic(final Node node) {
        int myIdx = node.getGameView().getPlayerOrder().getLogicalIdx(this.getPlayerIdx());
        int myCards = node.getGameView().getHandView(myIdx).size();

        int bestOther = Integer.MAX_VALUE;
        for (int i = 0; i < node.getGameView().getNumPlayers(); i++) {
            if (i == myIdx) continue;
            int otherCards = node.getGameView().getHandView(i).size();
            if (otherCards < bestOther) {
                bestOther = otherCards;
            }
        }

    if (myCards < bestOther) return 1.0f;
    else if (myCards == bestOther) return 0.5f;
    else return 0.0f;
}
    

    //a helper method that make a move
    private Move makeMove (final Node node, final int cardIdx){
        //check what the current player have in hand reaching a node
        HandView hand = node.getGameView().getHandView(node.getLogicalPlayerIdx());
        Card card = hand.getCard(cardIdx);

        //built a tempAgent for this player 
        Agent tempAgent = tempAgent(node);
        //if have a wild card, choose a color
        if(card.isWild()){
            Color chosenColor = chooseBestWildColor(hand);
            return Move.createMove(tempAgent, cardIdx, chosenColor);
        }
        //not wild card 
        return Move.createMove(tempAgent, cardIdx);
    }

    //a helper method for a player want to play the drawn card
    private Move DrawnMove(final Node node){
        //make a temp agent for the player who is playing this turn 
        Agent tempAgent = tempAgent(node);
        //if the node is the root node, we just use that value 
        if(node.getDepth() == 0 && DrawnIDx != null){
            HandView hand = node.getGameView().getHandView(node.getLogicalPlayerIdx());
            if(this.DrawnIDx != null && this.DrawnIDx >= 0){
                Card drawnCard = hand.getCard(this.DrawnIDx);
                if(drawnCard.isWild()){
                    Color choseColor = chooseBestWildColor( hand);
                    return Move.createMove(tempAgent, this.DrawnIDx, choseColor);
                }
                return Move.createMove(tempAgent, this.DrawnIDx);
            }
        }
        return null;
    }
    
    //reimplement dummy agent
    static Agent[] dummy(final GameView view){
        Agent[] agents = new Agent[view.getNumPlayers()];
        for(int logicalIdx = 0; logicalIdx < view.getNumPlayers(); logicalIdx++){
            final int playerIdx = view.getPlayerOrder().getAgentIdx(logicalIdx);
            agents[logicalIdx] = new Agent(playerIdx, 0){
                @Override
                public Move chooseCardToPlay(final GameView game){
                    return null;
                }
                @Override
                public Move maybePlayDrawnCard(final GameView game, final int drawnCardIdx){
                    return null;
                }
            };
            agents[logicalIdx].setLogicalPlayerIdx(logicalIdx);
        }
        return agents;
    }
    //make a temp agent for a node
    private Agent tempAgent(final Node node){
        //find which player is acting now
        final int cuerrentIdx = node.getLogicalPlayerIdx();
        //change the index to a real inded
        final int playerIdx = node.getGameView().getPlayerOrder().getAgentIdx(cuerrentIdx);
        Agent temp = new Agent(playerIdx, 0){
            public Move chooseCardToPlay(final GameView game){
                return null;
            }
            public Move maybePlayDrawnCard(final GameView game, final int drawnCardIdx){
                return null;
            }
        };
        temp.setLogicalPlayerIdx(cuerrentIdx);
        return temp;
    }

    //adding a helper to find how many wild card we have in hand
    private Color chooseBestWildColor(final HandView hand){
        int red = 0;
        int blue = 0;
        int green = 0;
        int yellow = 0;

    for(int i = 0; i < hand.size(); i++){
        Card c = hand.getCard(i);
        if(c == null){
            continue;
        } 
        //check what card we have 
        switch(c.color()){
            case RED:
                red++;
                break;
            case BLUE:
                blue++;
                break;
            case GREEN:
                green++;
                break;
            case YELLOW:
                yellow++;
                break;
            default:
                break;
        }
    }
    //set the current best color to be the red color 
    Color best = Color.RED;
    int bestCount = red;
    //now check which one is better and update
    if(blue > bestCount){
        best = Color.BLUE;
        bestCount = blue;
    }
    if(green > bestCount){
        best = Color.GREEN;
        bestCount = green;
    }
    if(yellow > bestCount){
        best = Color.YELLOW;
    }

    return best;
}
//a helper method to make move for the argMax 
    private Move makeMoveFromCardIdx(final GameView game, final int cardIdx){
    // Use the current logical player from this game state
        int curLogicalIdx = game.getPlayerOrder().getCurrentLogicalPlayerIdx();

        HandView hand = game.getHandView(curLogicalIdx);
        Card card = hand.getCard(cardIdx);

        if(card == null){
        return null;
        }
        //if the card is a wild card 
        if(card.isWild()){
        //choose a best color 
        Color chosenColor = chooseBestWildColor(hand);
        //use the current agent object as the player making the move
        return Move.createMove(this, cardIdx, chosenColor);
        }

        return Move.createMove(this, cardIdx);
    }
    
    /**
     * A method to argmax the Q values inside a {@link Node}
     *
     * @param   node            The {@link Node} who has populated q-values
     * @return  The {@link Move} corresponding to whichever {@link Move} has the largest q-value. Note
     *          that this can be <code>null</code> if you choose to not play the drawn card (you will
     *          have to detect whether or not you are in that scenario by examining the @{link Node}'s state).
     */
    @Override
    public Move argmaxQValues(final Node node)
    {
        // TODO: implement me!
        //three cases, legal and non-legal move, a may drown card move 
        //find the largest q value if it is a legal move

        //get the current state
        Node.NodeState state = node.getNodeState();
        //now check which case the current case belong to
        //if the current state is a non-legal move 
        if(state == Node.NodeState.NO_LEGAL_MOVES_UNRESOLVED_CARDS_PRESENT){
            return null;
        }
        //if in the current state the player need to draw one card.
        if(state == Node.NodeState.NO_LEGAL_MOVES_MAY_PLAY_DRAWN_CARD){
            //keep information of the cards
            //if the index = 0, we play the card 
            //if the index = 1, we keep the card
            int playIdx = Node.NoLegalMovesIdxDefaults.DrawSingleCardIdxs.PLAY_CARD_MOVE_IDX;
            int keepIdx = Node.NoLegalMovesIdxDefaults.DrawSingleCardIdxs.KEEP_CARD_MOVE_IDX;
            //a variable that check how good is the playedcard  
            float playQ; 
            //check how many time this move has been sampled
            if(node.getQCount(playIdx) == 0){
                //if it equal to 0, we set the current move to negative infinity 
                //since we dont want to comput Q value by dividing 0
                playQ = Float.NEGATIVE_INFINITY;//avoid this move it 0
            }else{
                playQ = node.getQValue(playIdx);
            }
            //same logic for keepQ
            float keepQ;
            if(node.getQCount(keepIdx) == 0){
                //if it equal to 0, we set the current move to negative infinity 
                //since we dont want to comput Q value by dividing 0
                keepQ = Float.NEGATIVE_INFINITY;//avoid this move it 0
            }else{
                keepQ = node.getQValue(keepIdx);
            }

            //now check which value is bigger
            //if we want to keep the card, return null
            if(keepQ > playQ){
                return null;
            }
            //otherwise we play the drawn card
            if(this.DrawnIDx == null){
                return null;
            }
            return makeMoveFromCardIdx(node.getGameView(), this.DrawnIDx);
        }
        
        //if we have the legal move
        int bestMoveIdx = -1;
        float beatQ = Float.NEGATIVE_INFINITY;

        for(int moveIdx = 0; moveIdx < node.getOrderedLegalMoves().size(); moveIdx++){
            //since it is not divided by 0 
            if(node.getQCount(moveIdx) == 0){
                continue;
            }
            float q = node.getQValue(moveIdx);
            if(bestMoveIdx == -1 || q > beatQ){
                bestMoveIdx = moveIdx;
                beatQ = q;
            }
        }
        //if we reach the move for the first time
        if(bestMoveIdx == -1){
            bestMoveIdx = 0;
        }
        //get the real card index from the q value
        int cardIdx = node.getOrderedLegalMoves().get(bestMoveIdx);
        return makeMoveFromCardIdx(node.getGameView(), cardIdx);
    }
    //Orgin 
    //javac -cp "./lib/*;." @uno.srcs
    //java -cp ".\lib\*;." edu.bu.pas.uno.SingleGameMain edu.bu.pas.uno.agents.RandomAgent src.pas.uno.agents.ExpectedOutcomeAgent
    //java -cp "./lib/*;." edu.bu.pas.uno.SingleGameMain src.pas.uno.agents.ExpectedOutcomeAgent edu.bu.pas.uno.agents.RandomAgent
}
