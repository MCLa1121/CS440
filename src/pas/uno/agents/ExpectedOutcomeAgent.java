package src.pas.uno.agents;


// SYSTEM IMPORTS
import edu.bu.pas.uno.Card;
import edu.bu.pas.uno.Game.GameView;
import edu.bu.pas.uno.Hand.HandView;
import edu.bu.pas.uno.agents.MCTSAgent;
import edu.bu.pas.uno.enums.Color;
import edu.bu.pas.uno.enums.Value;
import edu.bu.pas.uno.moves.Move;
import edu.bu.pas.uno.tree.Node;

import java.util.Random;
import java.util.Set;


// JAVA PROJECT IMPORTS
import edu.bu.pas.uno.Game;

public class ExpectedOutcomeAgent
    extends MCTSAgent
{
    //add a new field for argmax to solve a drawn card case
    private Integer DrawnIDx = null;
    
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
            //create a copy so we dont make change to the parent
            Game nextGame = new Game(this.getGameView());
            return null;
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
        return null;
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
    HandView hand = game.getHandView(this.getLogicalPlayerIdx());
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
                keepQ = node.getQValue(playIdx);
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
        return null;
    }
}
