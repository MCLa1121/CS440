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
            // set up a simulation 
            Game simulation = new Game(this.getGameView());
            // resolved move apply one time (if use a while loop here the agent will do noting)
            simulation.resolveMove(move);

            // get the next state of the simulatoin (the current player index)
            int next_state = simulation.getPlayerOrder().getCurrentLogicalPlayerIdx();

            // return a mctsnode after we get the next state 
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
        }

        // if no leagal move and no unresloved card persent reutnr null which here we need to draw card
        if (node.getNodeState() == Node.NodeState.NO_LEGAL_MOVES_UNRESOLVED_CARDS_PRESENT) {
            return null;
        }

        // if no legal move , and may play the drawn card 
        if (node.getNodeState() == Node.NodeState.NO_LEGAL_MOVES_MAY_PLAY_DRAWN_CARD) {
            // if we can play the card
            if (index == Node.NoLegalMovesIdxDefaults.DrawSingleCardIdxs.PLAY_CARD_MOVE_IDX) {
                // -1 because we are in a index form
                int draw_card_index = node.getGameView().getHandView(node.getLogicalPlayerIdx()).size() - 1;
                return wildMove(node.getGameView(), node.getLogicalPlayerIdx(), draw_card_index);
            }else{
                return null; // keep the card if non of the move can be make
            }
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
                // pick a random card base on the legal move we can make 
                int Pick_random_card = legal_moves.get(this.getRandom().nextInt(legal_moves.size()));
                // call wild move to return a move if we have wild card
                move = wildMove(simulation_game.getView(current_player_index), current_player_index, Pick_random_card);
            
            
            } else if (simulation_game.getUnresolvedCards().isEmpty()) {
                // we draw one card an and need to deside whether to keep it or play it
                // we draw a card and get the index of the draw card
                int draw_card_index = simulation_game.drawCard(current_card_hand);
                // get the draw card by using the draw card index (the exact card)
                Card draw_card = current_card_hand.getCard(draw_card_index);

                // if the drawn card are legal to pley
                if (draw_card.canBePlayedAsDrawCard(simulation_game)) {
                    move = wildMove(simulation_game.getView(current_player_index), current_player_index, draw_card_index);
                } else {
                    move = null; // we keep the card taht we have drawn
                }
            
            // otherwise we need to draw the card and add the total draw card to the with the unreaoved card that have in total
            } else {
                simulation_game.drawTotal(current_card_hand, simulation_game.getUnresolvedCards().total());
                move = null; // if not set move to null the simulation will keep running forever
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
        } else {
            // return one becaue only choice is to keep the card and do noting.
            return 1; 
        }
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
        // Bug: getlogicalPlayerIdx(); change it to game.getplayerOrder().getCurrentLogicalPlayerIdx()
        MCTSNode root_node = new MCTSNode(game, game.getPlayerOrder().getCurrentLogicalPlayerIdx(), null);
        long Start_of_thinking_time = System.currentTimeMillis();
        long budget = Math.max(1, this.getMaxThinkingTimeInMS() /2); 

        //---------- while we still have budget to think keep loop running ----------
        while (System.currentTimeMillis() - Start_of_thinking_time < budget) {

            Node current = root_node; 

            // -- for backpropageation --
            ArrayList<Node> node_path = new ArrayList<>();
            ArrayList<Integer> move_path = new ArrayList<>();
            node_path.add(current);

            // ------------ selection ------------
            // if not a leaf node keep running
            while (!current.isTerminal()) {

                // set not visited as -1
                int  not_visited = -1;

                // choices as the number fo choice from current root node
                int choices = getNumberOfChoices(current);

                // use the for loop to determine the move index that we have not visit
                for (int i = 0; i< choices; i++) {
                    if (current.getQCount(i) == 0) {
                        not_visited = i;
                        break;          
                    }
                }

                // --------- Expansion ------------
                // if not visited is not equal to -1 we want to try a new move that not been visited before
                if (not_visited != -1) {
                    // create expand move , and based on the current node and not visted move index
                    Move expand_move = choiceToMove(current, not_visited);
                    // update current to its child base on the move epaand move
                    current = current.getChild(expand_move);
                    
                    // update move path and node path
                    move_path.add(not_visited);
                    node_path.add(current);

                    // break the loop to uwe are going to simulate this new move
                    break; 
                
                //-----------Selection ---------------
                // otherwise (when not visted index in -1, mean we try all move at lealst 1 time)
                } else {
                    // Bug: i should not assume my oppoent also pick the action that maximize my ucb score
                    // use getbestUcb to get the best ucb value we can have
                    int best_ucb;
                    // if the current turn is my turn i call get best ucb to get the best ucb as my perspective
                    if (current.getLogicalPlayerIdx() == this.getLogicalPlayerIdx()) {
                        best_ucb = getBestUCB(current);
                    
                    // otherwise it is not my turn; which shoudl random choose
                    } else {
                        best_ucb = this.getRandom().nextInt()getNumberOfChoices(current);
                    }
                    // and get the best move based on the best ucb value we just get
                    Move best_move = choiceToMove(current, best_ucb);
                    // update current
                    current = current.getChild(best_move);

                    // update node path and move path
                    move_path.add(best_ucb);
                    node_path.add(current);
                }  
            }

            // ----------- Rollout --------------
            float rollout_value = rollout(current.getGameView());
            // ----------- Backpropagation ---------
            backpropagate(node_path, move_path, rollout_value);
        }

        // return root_node after we done
        return root_node;

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

        // get the number of legalmove we can do
        int num_of_legal_move = getNumberOfChoices(node);
        // set the best q value to negative infinity the bigger the better
        float best_q_vlaue = Float.NEGATIVE_INFINITY;

        // set an array list to store the best move index
        ArrayList<Integer> best_move_index = new ArrayList<>();

        // use a for loop to itrate 
        for (int i = 0; i < num_of_legal_move; i++) {
            // if getqcount is zero , mean we have not visit ths move, so skip this move
            if (node.getQCount(i) == 0) {
                continue;
            }

            // if the move is visited get the q value of this move
            float q = node.getQValue(i);

            // if q is bigger the best_q_value we have found
            if (q > best_q_vlaue) {
                // update the best q value
                best_q_vlaue = q;
                // empty the best move index list
                best_move_index.clear();
                // add the best move index that we have right now into the list
                best_move_index.add(i);
                
            // else if the q value is as good as the best we found before, we just add to the list (add the move index)
            } else if (q == best_q_vlaue) {
                best_move_index.add(i);
            }
        }

        //------------ if no move has been visit choose randomly ----
        // if the best move index list is empty, mean no move has been  visit
        if (best_move_index.isEmpty()) {
            // set a random choice index by randomly pick baase on the number of legal move we have
            int random_choice_index = this.getRandom().nextInt(num_of_legal_move);

            // reuturn choice to mvoe to make a move base on the current choice we have make
            return choiceToMove(node, random_choice_index);
        }

        // we need to choice one of the best move in the list e.g we have 3 card that is eqaully good
        int best_move_choice = best_move_index.get(this.getRandom().nextInt(best_move_index.size()));

        // return choice to move based on the best move choice we have get
        return choiceToMove(node, best_move_choice);
    }
}


// javac -cp "./lib/*;." @uno.srcs
// java -cp "./lib/*;." edu.bu.pas.uno.SingleGameMain src.pas.uno.agents.UCTAgent src.pas.uno.agents.UCTAgent 
// java -cp "./lib/*;." edu.bu.pas.uno.SingleGameMain edu.bu.pas.uno.agents.RandomAgent src.pas.uno.agents.UCTAgent 