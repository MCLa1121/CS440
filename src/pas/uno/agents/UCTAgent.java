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

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;


// JAVA PROJECT IMPORTS


public class UCTAgent
    extends MCTSAgent
{

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
            return null;
        }
    }

    public UCTAgent(final int playerIdx,
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
        Node current = ;

        // if is current a leaf node
        if (current.isTerminal()) {

            if( the ni value is for current O) {
                rollout;
            }else{
                for (Move act: current.getOrderedLegalMoves()) {
                    add a new state to a tree;
                    current = first new child node;
                    if (si.isTerminal()) {
                        
                    }
                }
            }

        }
        current = current.getChild(null); // current = child node of current that maximize ucs1(si)


        return current;
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
        float max_Q_Value = Float.NEGATIVE_INFINITY;
        

        ArrayList<Integer> best_move_index = new ArrayList<>(); // because the node.get...legalmove is returning the list integer type so we need to dedfind an arraylist to store the index of move

        // --------- state where the player is legal move card to play -----------
        if (node.getNodeState() == Node.NodeState.HAS_LEGAL_MOVES) {
            int numOfMove= node.getOrderedLegalMoves().size();

            for (int i = 0; i < numOfMove; i++) {
                float q_value = node.getQValue(i);

                if (q_value > max_Q_Value) {
                    max_Q_Value = q_value;
                    best_move_index.clear(); // remove all element in the list
                    best_move_index.add(i); // add the i to the list
                }else if (q_value == max_Q_Value || (Float.isNaN(q_value) && Float.isNaN(max_Q_Value))){
                    best_move_index.add(i);
                }
            }

            if (best_move_index.isEmpty()) return null;

            int Random_Qindex = best_move_index.get(this.getRandom().nextInt(best_move_index.size()));
            Integer Index_card_in_hand = node.getOrderedLegalMoves().get(Random_Qindex);

            Card Play_Card = node.getGameView().getHandView(node.getLogicalPlayerIdx()).getCard(Index_card_in_hand);
            
            // if is wild we play wild move else other move
            if (Play_Card.isWild()) {
                return Move.createMove(this, Index_card_in_hand, Color.getRandomColor(getRandom()));
            } else {
                return Move.createMove(this, Index_card_in_hand);
            }

        // ---------state where we as a player need to draw 1 card and must choose to play or not play it
        }else if(node.getNodeState() == Node.NodeState.NO_LEGAL_MOVES_MAY_PLAY_DRAWN_CARD) {
            float q_value_play = node.getQValue(Node.NoLegalMovesIdxDefaults.DrawSingleCardIdxs.PLAY_CARD_MOVE_IDX);
            float q_value_keep = node.getQValue(Node.NoLegalMovesIdxDefaults.DrawSingleCardIdxs.KEEP_CARD_MOVE_IDX);

            int Index_draw_card = node.getGameView().getHandView(node.getLogicalPlayerIdx()).size();

            if (q_value_play > q_value_keep) {
                return Move.createMove(this, Index_draw_card);
            }else if(q_value_keep > q_value_play){
                return null; // null mean we are keeping the card
            }else{
                // if tie, than choose it randomly
                if (this.getRandom().nextBoolean()) {
                    return Move.createMove(this, Index_draw_card);
                }else{
                    return null; // return null we keep the card
                }
            }

        // ----------state where we as a player get a card that can not be resolved, e.g draw more than 2 cards
        }else if(node.getNodeState() == Node.NodeState.NO_LEGAL_MOVES_UNRESOLVED_CARDS_PRESENT) {
            return null; // we can not doing anyting about it but keep it
        }
        
        return null; 
        
    }
}