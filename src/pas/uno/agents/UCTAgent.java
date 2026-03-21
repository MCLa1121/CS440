package src.pas.uno.agents;


// SYSTEM IMPORTS
import edu.bu.pas.uno.Card;
import edu.bu.pas.uno.Game;
import edu.bu.pas.uno.Game.GameView;
import edu.bu.pas.uno.Hand;
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

        // getChild method: return a mctsnode
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

    // ---------------------------- PRIVATE HELPER -------------------------------------------------------------------------------------------------------------------------
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
    private float rollout(GameView gameview) {
        // set a simulation game
        Game simulation_game = new Game(gameview);

        // while the game is not over keep playing
        while (!simulation_game.isOver()) {
            // get the current player index, and get the current card in hand, and set move to null
            int current_player_index = simulation_game.getPlayerOrder().getCurrentLogicalPlayerIdx();
            Hand current_card_hand = simulation_game.getHand(current_player_index);
            Move move = null;


            // if the current card in hand can move legally
            if (current_card_hand.hasLegalMoves(simulation_game)) {
                // store all legal move in the array list
                ArrayList<Integer> legal_moves = new ArrayList<>(current_card_hand.getLegalMoves(simulation_game));
                // pick a radom card base on the legal move we can make 
                int Pick_random_card = legal_moves.get(this.getRandom().nextInt(legal_moves.size()));
                // call wild move to return a move if we have wild card
                move = wildMove(gameview, current_player_index, Pick_random_card);
            
            
            } else if (simulation_game.getUnresolvedCards().isEmpty()) {
                // we draw one card an and need to deside whether to keep it or play it
                // we draw a card and get the index of the draw card
                int draw_card_index = simulation_game.drawCard(current_card_hand);
                // get the draw card by using the draw card index (the exact card)
                Card draw_card = current_card_hand.getCard(draw_card_index);

                // if the drawn card are legal to pley
                if (draw_card.canBePlayedAsDrawCard(simulation_game)) {
                    move = wildMove(gameview, current_player_index, draw_card_index);
                }
            
            // otherwise we need to draw the card and add the total draw card to the with the unreaoved card that have in total
            } else {
                simulation_game.drawTotal(current_card_hand, simulation_game.getUnresolvedCards().total());;
            }

            // if the move is null then call resovlved move to move to the next turn
            simulation_game.resolveMove(move);
        }

        // ------------ Win OR Lose --------
        // if we have play all of the card in our hand (0 card in hand)
        if (simulation_game.getHand(this.getLogicalPlayerIdx()).size() == 0) {
            // win + 1
            return 1.0f;

        } else {
            // lose -1
            return -1.0f;
        }
    }

    // private helper -------------help to backpropagate
    private void backpropagate(ArrayList<Node> node_path, ArrayList<Integer> move_path, float q_value) {
        for (int i = 0; i < move_path.size(); i++) {
            // get the parent of this node path
            Node parent = node_path.get(i);
            
            // get the move that we choose
            int move_that_choose = move_path.get(i);

            // return the current visit time
            long current_count = parent.getQCount(move_that_choose);
            
            // return the current total of q value overall nodes (based on the current move that we choose)
            float Total_q_value = parent.getQValueTotal(move_that_choose);

            // update N(s,a) +=1 (base on current move)
            parent.setQCount(move_that_choose, current_count + 1);

            // update q value in total (base on current move)
            parent.setQValueTotal(move_that_choose, Total_q_value + q_value);
        }
    }

    // private helper getNumberOfchoices ------------ get the number of choice we can have
    private int getNumberOfChoices(Node node) {
        // if it has legal move, return the size of the orderedlegal move which is our choices
        if (node.getNodeState() == Node.NodeState.HAS_LEGAL_MOVES) {
            return node.getOrderedLegalMoves().size();

        // if we only have a move that we may be able to play a card or keep a card (in total 2 choices)
        } else if (node.getNodeState() == Node.NodeState.NO_LEGAL_MOVES_MAY_PLAY_DRAWN_CARD) {
            return 2; 
        }

        // return one becaue only choice is to keep the card and do noting.
        return 1; 
    }

    // private helper getBestUCB ----------- get the best ucb we can find (note its retuning the index)
    private int getBestUCB(Node node) {
        // set the best ucb value to neagive infinity (we are going to have the maximum ucb, the bigger the better)
        float best_ucb = Float.NEGATIVE_INFINITY;

        // create an array list to store the best ucb index in there
        ArrayList<Integer> best_ucb_index = new ArrayList<>();

        // totoal count of all visit time with all nodes's vists times
        long total_count = node.getStateCount();

        // get the number of choice that we have
        int num_of_choice = getNumberOfChoices(node);
        
        for (int i = 0; i < num_of_choice; i++) {
            // set a counter to get Q count number (eaiser explantation: counter for visit time for the current node)
            long counter = node.getQCount(i);
            // initiazlie the ubc value as float
            float UCB_Value; 

            // if the counter is 0, meaning never visit before set the ucb value to +infinty
            if (counter == 0) {
                UCB_Value = Float.POSITIVE_INFINITY;
            } else {
                // get the q value of the node
                float q_value = node.getQValue(i);

                // there we are using the ucb rule; implement the formular
                double explore = Math.sqrt(2) * Math.sqrt(Math.log(total_count) / (double) counter);

                // implementing ucb
                UCB_Value =  q_value + (float)explore;
            }

            // if the ucb value just calculate is better than the max ucb value we update the best ucb
            if (UCB_Value > best_ucb) {
                // update the best ucb value
                best_ucb = UCB_Value;

                // empty the current list and add the current i to the list
                best_ucb_index.clear();
                best_ucb_index.add(i);

            // if ucb value is equal to the best ucb value then just add this current in to the list
            } else if (UCB_Value == best_ucb) {
                best_ucb_index.add(i);
            }
        }

        // reuturn the best ucb_index that we randomly pick in the array list
        return best_ucb_index.get(this.getRandom().nextInt(best_ucb_index.size()));
    }

    // ----------------------------------------------------------------------------------------------------------------------------------------------------------------------

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