package src.pas.uno.agents;


// SYSTEM IMPORTS
import edu.bu.pas.uno.Card;
import edu.bu.pas.uno.Game;
import edu.bu.pas.uno.Game.GameView;
import edu.bu.pas.uno.Hand.HandView;
import edu.bu.pas.uno.agents.MCTSAgent;
import edu.bu.pas.uno.enums.Color;
import edu.bu.pas.uno.enums.Value;
import edu.bu.pas.uno.moves.Move;
import edu.bu.pas.uno.tree.Node;

import java.util.ArrayList;
import java.util.List;
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
            Game simulation = new Game(this.getGameView());
            while (!simulation.isOver()) {
                simulation.resolveMove(move);
            }
            int next_state = simulation.getPlayerOrder().getCurrentLogicalPlayerIdx();
            MCTSNode mctsnode = new MCTSNode(simulation.getView(next_state), next_state, this);
            return mctsnode;
        }
    }

    public UCTAgent(final int playerIdx,
                    final long maxThinkingTimeInMS)
    {
        super(playerIdx, maxThinkingTimeInMS);
    }

    // ---------------------------- PRIVATE HELPER ---------------------------------------------
    // private helper -------------- help to make the choice to move
    private Move choiceToMove(Node node, int index) {
        // if can legally move
        if (node.getNodeState() == Node.NodeState.HAS_LEGAL_MOVES) {
            return wildMove(node.getGameView(),node.getLogicalPlayerIdx(), node.getOrderedLegalMoves().get(index));
        
        // if only aloowed to draw single card , play or keep 
        } else if (node.getNodeState() == Node.NodeState.NO_LEGAL_MOVES_MAY_PLAY_DRAWN_CARD) {
            // if we can play the card
            if (index == Node.NoLegalMovesIdxDefaults.DrawSingleCardIdxs.PLAY_CARD_MOVE_IDX) {
                // -1 because we are in a index form
                int draw_card_index = node.getGameView().getHandView(node.getLogicalPlayerIdx()).size() - 1;
                return wildMove(node.getGameView(), node.getLogicalPlayerIdx(), draw_card_index);
            }

            return null; // keep the card if non of the move can be make

        }

        return null; // return null if we make the draw and do nothing
    }

    // private helper ------------- help to make the choic to move if we have wild card
    private Move wildMove(GameView game, int player_index, int card_index) {
        Card card_in_hand = game.getHandView(player_index).getCard(card_index);
        if (card_in_hand.isWild()) {
            return Move.createMove(this, card_index,Color.getRandomColor(getRandom()));
        }
        return Move.createMove(this, card_index);
    }

    // private helper -------------help to rollout

    // private helper -------------help to backpropagate

    // private helper getNumberOfchoices ------------ get the number of choice we can have

    // private helper getBestUCB ----------- get the best ucb we can find


    // -------------------------------------------------------------------------------------------

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
        MCTSNode root_node = new MCTSNode(game, getLogicalPlayerIdx(), null);
        long Start_of_thinking_time = System.currentTimeMillis();

        //---------- while we still have budget to think keep loop running ----------
        while (System.currentTimeMillis() - Start_of_thinking_time < this.getMaxThinkingTimeInMS()) {
            Node current = root_node; 

            // -- for backpropageation --
            ArrayList<Node> node_path = new ArrayList<>();
            ArrayList<Integer> action_path = new ArrayList<>();
            node_path.add(current);

            // ------------ selection ------------
            // if not a leaf node keep running
            while (!current.isTerminal()) {
                int choices = ggt
 }

            // ------------expansion -------------

        }

        // ---------- simulation -------------
        

        // ---------- backpropagate ---------


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
        float max_ucb = Float.NEGATIVE_INFINITY;
        ArrayList<Integer> best_move_index = new ArrayList<>();
        long sum_of_visits = node.getStateCount(); // the N in formlar 

        // ----------- find how many choice do we have ---------
        int num_of_choice = 0;
        // if we have legal move
        if (node.getNodeState() == Node.NodeState.HAS_LEGAL_MOVES) {
            num_of_choice = node.getOrderedLegalMoves().size();
        
        // if we do not have legal move and play drawn move
        }else if (node.getNodeState() == Node.NodeState.NO_LEGAL_MOVES_MAY_PLAY_DRAWN_CARD) {
            num_of_choice = 2; // 1 for play 1 for keep in total there are 2 choice

        }else{
            num_of_choice = 1; // if cannot play it , the reamin choice is to draw it, and that become the only one choice
        }

        //-----------calculate ucb for each state(move) index
        for (int i = 0; i < num_of_choice; i++) {
            long move_visit_counter = node.getQCount(i); // in each node we have a counter for visit time (each node has it unique and the root node will have the sum)
            float value;

            // if zero mean we have not visit it before and it haa a value that is +infinity 
            if (move_visit_counter == 0) {
                value = Float.POSITIVE_INFINITY;
            }else{
                float Q_value =node.getQValue(i);

                // use the ucb formular
                double explore = Math.sqrt(2.0) * Math.sqrt(Math.log(sum_of_visits) / (double)move_visit_counter );
                value = Q_value + (float)explore;
            }

            if (value > max_ucb) {
                max_ucb = value; // the bigger the better update max ucb
                best_move_index.clear(); // empty the element in the list
                best_move_index.add(i); // add the index of max ucb
            }
        }

        int card_index_chose = best_move_index.get(this.getRandom().nextInt(best_move_index.size()));
        Card card_in_hand = node.getGameView().getHandView(node.getLogicalPlayerIdx()).getCard(card_index_chose);
        
        // ----------if can legally make a move
        if (node.getNodeState() == Node.NodeState.HAS_LEGAL_MOVES) {
            // whether has wild card in hand
            if (card_in_hand.isWild()) {
                return Move.createMove(this,card_index_chose,Color.getRandomColor(getRandom())); // pick a randome color
            }
            return Move.createMove(this, card_index_chose); // if not wild make the move ues card index chose

        // ----------if node cannot make legal move but may play draw card
        }else if (node.getNodeState() == Node.NodeState.NO_LEGAL_MOVES_MAY_PLAY_DRAWN_CARD) {
            if (card_index_chose == Node.NoLegalMovesIdxDefaults.DrawSingleCardIdxs.PLAY_CARD_MOVE_IDX) {
                int draw_card_index = node.getOrderedLegalMoves().get(card_index_chose); // get draw card index
                // if has card wild
                if (card_in_hand.isWild()) {
                    return Move.createMove(this, draw_card_index,Color.getRandomColor(getRandom()));
                }
                return Move.createMove(this, draw_card_index);
            }
        }
        return null; // we keep the card that we draw
    }
}