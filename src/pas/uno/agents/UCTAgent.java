package src.pas.uno.agents;


// SYSTEM IMPORTS
import edu.bu.pas.uno.Card;
import edu.bu.pas.uno.Deck;
import edu.bu.pas.uno.Game;
import edu.bu.pas.uno.Game.GameView;
import edu.bu.pas.uno.Hand;
import edu.bu.pas.uno.Hand.HandView;
import edu.bu.pas.uno.enums.Color;
import edu.bu.pas.uno.enums.Value;
import edu.bu.pas.uno.moves.Move;
import edu.bu.pas.uno.tree.Node;
import edu.bu.pas.uno.agents.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;


// JAVA PROJECT IMPORTS


public class UCTAgent
    extends MCTSAgent
{
    //-------------------------------------------- Proxy Agent --------------------------------------------
    // create a proxyAgent static class where extends Agent and create the proxyAgent constructor so we can use in search get_child and etc: the idea based on the piazza post
    private static class ProxyAgent extends Agent {
        public ProxyAgent(int playerIdx, long maxThinkingTimeInMS) {
            super(playerIdx, maxThinkingTimeInMS);
        }
        // remember to define the class properly (Note: super) 
        @Override
        public Move chooseCardToPlay(Game.GameView game) {
            return null; 
        }

        // remember to define the class properly (Note: super) 
        @Override
        public Move maybePlayDrawnCard(Game.GameView game, int drawnCardIdx) {
            return null; 
        }
    }

    // Here we default a private field that use ProxyAgent we just define 
    private ProxyAgent[] proxyAgents;

    public static class MCTSNode
        extends Node
    {
        // Here we modify two things
        // 1. add Agent[] simulationAgents to private field and as extra argument Note: used in getchild; wildMove
        // 2. add Integer drawnCardIdx to private field and as extra argument     Note: used in choiceToMove; search

        private final Agent[] simulationAgents;
        private final Integer drawnCardIdx;

        public MCTSNode(final GameView game,
                        final int logicalPlayerIdx,
                        final Node parent,
                        final Agent[] simulationAgents,
                        final Integer drawnCardIdx)
        {
            super(game, logicalPlayerIdx, parent);

            // default the two private feilds
            this.simulationAgents = simulationAgents; 
            this.drawnCardIdx = drawnCardIdx;
        }
        
        // A gettter method to get this.drawncardIndex
        public Integer getDrawnCardIdx() {
            return this.drawnCardIdx;
        }

        // getChild method: return a mctsnode 
        @Override
        public Node getChild(final Move move)
        {
            // deck is unknown (important). so we nee provide a  full Deck and use Hand[] from the game view so no unknown cards can touch the game.
            // set view: getgameView
            GameView view = this.getGameView();

            // set numPlayers: store the number of players of current gameview
            int numPlayers = view.getNumPlayers();

            // Here we create hands that the size is the number of players
            Hand[] hands = new Hand[numPlayers];

            // use a for loop to initiate the hands we just create
            for (int i = 0; i < numPlayers; i++) {

                // remember each index correspond to one playe, and each position in the array we store the hand view of players
                hands[i] = new Hand(view.getHandView(i));
            }

            // set simulation where is the core of this getchild method: look at doc in game how we create is based on Game(Deck drawPile, Hand[] hands, GameView view, Agent[] agentsInPlayingOrder)
            // Note: we use the simulationAgnets we created
            Game simulation = new Game(new Deck(true), hands, view, this.simulationAgents);

            // get the current player index
            int current_player_index = this.getLogicalPlayerIdx();

            // get current hand from the current player
            Hand current_hand = simulation.getHand(current_player_index);

            // if the unrealoved draw is avalilble (it eeqaul to the node state)
            if (this.getNodeState() == Node.NodeState.NO_LEGAL_MOVES_UNRESOLVED_CARDS_PRESENT) {
                // -------- Tesing --------- 
                // System.out.println("getChild unresolved");

                // player need to draw the total cards, so use drawTotal to add the total number of cards that we need, and add to our current hand
                simulation.drawTotal(current_hand,simulation.getUnresolvedCards().total());
                
                // -------- Tesing --------- 
                // System.out.println("before resolveMove null");
                
                // Here we need to set to null, indicate we finished the move (and next turn can be simulate corretly)
                simulation.resolveMove(null);
   
                // --------Testing ---------
                // System.out.println("after resolveMove null");
            } else {

                // -------- Tesing --------- 
                // System.out.println("before resolveMove move");

                // resolved move apply one time here if it is not the case that unrealoved draw is avalible (Note: if use a while loop here the agent will do noting)
                simulation.resolveMove(move);

                // -------- Tesing --------- 
                // System.out.println("after resolveMove move");
            }

            // get the next state of the simulatoin (the current player index)
            int next_state = simulation.getPlayerOrder().getCurrentLogicalPlayerIdx();

            // Store the omniscient view so child nodes CAN also have no the cards that is unknown, and here we create the mctsnode that sotre the formation
            // of next state and the omiscient view ..etc.
            MCTSNode mctsnode = new MCTSNode(simulation.getOmniscientView(), next_state, this, this.simulationAgents, null);

            // -------- Tesing --------- 
            // System.out.println("getChild end");

            // Here we return the mctsnode
            return mctsnode;
        }
    }

    public UCTAgent(final int playerIdx,
                    final long maxThinkingTimeInMS)
    {
        super(playerIdx, maxThinkingTimeInMS);
    }


    // ------------------------------------------------ PRIVATE HELPER SECTION ----------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------------------------------------------------------------------------------
    // Private helper -------------- help to make the choice to move
    private Move choiceToMove(Node node, int index) {
        // set player_index: get the logical player index from the node that pass in
        int player_index = node.getLogicalPlayerIdx();

        // if can legally move (equal to the current node state)
        if (node.getNodeState() == Node.NodeState.HAS_LEGAL_MOVES) {
            // then call wilmove private method, to check whether it can play wild or not
            // Note: node.getOrderedLegalMoves().get(index) use to get the card index
            return wildMove(node.getGameView(),player_index, node.getOrderedLegalMoves().get(index));
        }

        // if no leagal move and no unresloved card persent reuturn null which here we need to draw card
        if (node.getNodeState() == Node.NodeState.NO_LEGAL_MOVES_UNRESOLVED_CARDS_PRESENT) {
            // so we return null
            return null;
        }

        // if no legal move , and may play the drawn card (need to check are we able to play the card)
        if (node.getNodeState() == Node.NodeState.NO_LEGAL_MOVES_MAY_PLAY_DRAWN_CARD) {
            // if no leagl move and draw single card and keep it (if the index is the eqaul to the if statment)
            if (index == Node.NoLegalMovesIdxDefaults.DrawSingleCardIdxs.KEEP_CARD_MOVE_IDX) {
                // return null if no legal move allowed
                return null;
            }

            // Here set drawncardidx where store drawncard idx
            // Note: a bit ugly but need to cast mctsnode to in order to use the getDrawncardisx method
            Integer drawnCardIdx = ((MCTSNode) node).getDrawnCardIdx();

            // if the drawnCardisx is null value we return null (just in case, we prevent the card null value to be used in our move)
            if (drawnCardIdx == null) {
                // return null value here if drawncard idx is null
                return null; 
            }

            // if the card is playable then call wile card 
            // Note: the index of the card is now using drawCardIdx
            return wildMove(node.getGameView(), player_index, drawnCardIdx);
        }
        return null; // return null if we make the draw and do nothing
    }

    // Private helper ------------- help to make the choic to move if we have wild card
    private Move wildMove(GameView game, int player_index, int card_index) {
        // store the player card in hand to card in hand (by the pass in player index and the card index)
        Card card_in_hand = game.getHandView(player_index).getCard(card_index);

        // Here we store the ageet info from proxyAgent[] to proxyAgent
        ProxyAgent proxyAgent = proxyAgents[player_index];

        // if the card in hand is wild
        if (card_in_hand.isWild()) {
            // we return: we create a move that use the corresponded proxyAgent ,m card index and pick the color randomly
            return Move.createMove(proxyAgent, card_index,Color.getRandomColor(getRandom()));
        }
        
        // if its not a wild card then just play the card normally
        return Move.createMove(proxyAgent, card_index);
    }

    // Private helper -------------help to rollout
    private float rollout(GameView gameview) {
        // here is the same we did in getchild
        // set numPlayers: store the number of players of current gameview
        int num_of_player = gameview.getNumPlayers();

        // get current hand from the current player
        Hand[] hands = new Hand[num_of_player];

        // use a for loop to initiate the hands we just create
        for (int i = 0; i < num_of_player; i++) {
            // remember each index correspond to one playe, and each position in the array we store the hand view of players
            hands[i] = new Hand(gameview.getHandView(i));
        }

        // set simulation where is one of the important part for roll out method: look at doc in game how we create is based on Game(Deck drawPile, Hand[] hands, GameView view, Agent[] agentsInPlayingOrder)
        // Note: we use the simulationAgnets we created
        Game simulation_game = new Game(new Deck(true), hands, gameview, proxyAgents);

        // ----------- Testing ---------
        // while the game is not over keep playing
        // bug: might be the simualation is too mluch make it smaller
        int size = 0;

        // need to make rollout cheaper for testing
        // if we have not finished simualting game
        while (!simulation_game.isOver() && size < 20) {
            // add size each itreation 
            size ++;

            // get the current player index, and get the current card in hand, and set move to null
            int current_player_index = simulation_game.getPlayerOrder().getCurrentLogicalPlayerIdx();
            Hand current_card_hand = simulation_game.getHand(current_player_index);
            Move move = null;

            // if the current card in hand can move legally
            if (current_card_hand.hasLegalMoves(simulation_game)) {
                // store all legal move in the array list legal_moves
                ArrayList<Integer> legal_moves = new ArrayList<>(current_card_hand.getLegalMoves(simulation_game));
                // pick a random card base on the legal move we can make 
                int Pick_random_card = legal_moves.get(this.getRandom().nextInt(legal_moves.size()));
                // call wild move to assign a move if we have wild card
                move = wildMove(simulation_game.getView(simulation_game.getPlayerOrder().getAgentIdx(current_player_index)), current_player_index, Pick_random_card);
            
            
            } else if (simulation_game.getUnresolvedCards().isEmpty()) {
                // we draw one card an and need to deside whether to keep it or play it
                // we draw a card and get the index of the draw card
                int draw_card_index = simulation_game.drawCard(current_card_hand);

                // get the draw card by using the draw card index (the exact card)
                Card draw_card = current_card_hand.getCard(draw_card_index);

                // if the drawn card are legal to pley
                if (draw_card.canBePlayedAsDrawCard(simulation_game)) {
                    // assign the move (call wildmove to detemine whether its a wild move or not a wiild move)
                    move = wildMove(simulation_game.getView(simulation_game.getPlayerOrder().getAgentIdx(current_player_index)), current_player_index, draw_card_index);
                } else {
                    // make sure we end the turn whe call resolvedMove
                    move = null; 
                }
            
            // otherwise we need to draw the card and add the total draw card to the with the unreaoved card that have in total
            } else {
                simulation_game.drawTotal(current_card_hand, simulation_game.getUnresolvedCards().total());

                // if not set move to null the simulation will keep running forever (timeout!!! alert)
                move = null; 
            }

            // if the move is null then call resovlved move to move to the next turn (which mean this simulate game is done)
            simulation_game.resolveMove(move);
        }
        
        // ------------ Win OR Lose --------
        // if we have play all of the card in our hand (0 card in hand)
        if (simulation_game.getHand(this.getLogicalPlayerIdx()).size() == 0) {
            // if return win + 1
            return 1.0f;

        } else {
            // other return lose -1
            return -1.0f;
        }
        
    }

    // Private helper -------------help to backpropagate
    private void backpropagate(ArrayList<Node> node_path, ArrayList<Integer> move_path, float q_value) {
        // Note: ArrayList<Node> node_path, ArrayList<Integer> move_path tell us the path we need to backprageate

        // use a for loop to start the backpropagate
        for (int i = 0; i < move_path.size(); i++) {
            // get the parent of this node path
            Node parent = node_path.get(i);
            
            // get the move that we choose
            int move_that_choose = move_path.get(i);

            // store the current visit time
            long current_count = parent.getQCount(move_that_choose);
            
            // store the current total of q value overall nodes (based on the current move that we choose)
            float Total_q_value = parent.getQValueTotal(move_that_choose);

            // update N(s,a) +=1 (base on current move) Note: here is where we update the N(s,a)
            parent.setQCount(move_that_choose, current_count + 1);

            // update q value in total (base on current move) Note: need to update the q value total for each node 
            parent.setQValueTotal(move_that_choose, Total_q_value + q_value);
        }
    }

    // Private helper getNumberOfchoices ------------ get the number of choice we can have
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

    // Private helper getBestUCB ----------- get the best ucb we can find (note its retuning the index)
    private int getBestUCB(Node node) {
        // set the best ucb value to neagive infinity (we are going to have the maximum ucb, the bigger the better)
        float best_ucb = Float.NEGATIVE_INFINITY;

        // create an array list to store the best ucb index in there
        ArrayList<Integer> best_ucb_index = new ArrayList<>();

        // totoal count of all visit time with all nodes's vists times use math.max to avoid using zero
        long total_count = Math.max(1,node.getStateCount());

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
        // ---------- Testing ------------
        // Add a itration control because it continue to timeout
        // int iterate_count = 0;
        // debuge print statement
        // System.out.println("search start");

        // if the proxyagents is null value
        if (proxyAgents == null) {
            // then store the number of players in to n
            int n = game.getNumPlayers();
            // crerate a size n proxyAgent
            proxyAgents = new ProxyAgent[n];
            
            // use a for loop to default the proxyAgent; with size n 
            for (int i = 0; i < n; i++) {
                // get the current actual player's agent index
                int actualPlayerIdx = game.getPlayerOrder().getAgentIdx(i);
                // crreate a proxyAgent(use the constructor we just create) for this player
                proxyAgents[i] = new ProxyAgent(actualPlayerIdx, this.getMaxThinkingTimeInMS());
                // and set the proxyAgent we just default to a logialplayeridx
                proxyAgents[i].setLogicalPlayerIdx(i);
            }
        }
        
        // Bug: getlogicalPlayerIdx(); change it to game.getplayerOrder().getCurrentLogicalPlayerIdx()
        // store the current logical player index based on the curren player order to player root index
        int player_root_index = game.getPlayerOrder().getCurrentLogicalPlayerIdx();

        // set root_node: which the root node use the drawncradinx and proxyAgnets 
        MCTSNode root_node = new MCTSNode(game, player_root_index, null, proxyAgents,drawnCardIdx);
        
        // if there is no legal move and no unrealoved draw and no drawn card
        if (drawnCardIdx == null && game.getHandView(player_root_index).getLegalMoves(game).isEmpty() && game.getUnresolvedCards().isEmpty()) {
            
            // we wnat to choo keep card move idx to null so use set q method
            root_node.setQCount(Node.NoLegalMovesIdxDefaults.DrawSingleCardIdxs.KEEP_CARD_MOVE_IDX, 1);
            root_node.setQValueTotal(Node.NoLegalMovesIdxDefaults.DrawSingleCardIdxs.KEEP_CARD_MOVE_IDX, 0.0f);
            
            // debug print statement
            // System.out.println("early root return");

            // reutn the roo node
            return root_node; 
        }
        
        // Here is the thinking buget part
        long Start_of_thinking_time = System.currentTimeMillis();
        // the buget can be smaller or bigger i use /4 so the test can be test much more safer
        // if want to make the agent to act more powerful, we can make it to /2 or more Note: just be careful about timeout
        long budget = Math.max(1, this.getMaxThinkingTimeInMS()/4); 

        //---------- while we still have budget to think keep loop running ----------
        while (System.currentTimeMillis() - Start_of_thinking_time < budget) {
            // --- Tesing ---
            // iterate_count++;

            // set the root_node as current
            Node current = root_node; 

            // -- setup for backpropageation --
            // Init: node path, move path, and add current as the head of the node path
            ArrayList<Node> node_path = new ArrayList<>();
            ArrayList<Integer> move_path = new ArrayList<>();
            node_path.add(current);

            // --- Testing ---
            // System.out.println("A");

            // -----------------------------Selection -----------------------------
            // if not a leaf node keep running
            while (!current.isTerminal()) {

                // set not visited as -1
                int  not_visited = -1;

                // choices as the number fo choice from current root node
                int choices = getNumberOfChoices(current);

                // use the for loop to determine the move index that we have not visit
                for (int i = 0; i< choices; i++) {
                    // if visit time (get q count) is zero, mean we have not visit this node before; set to i
                    // and break
                    if (current.getQCount(i) == 0) {
                        not_visited = i;
                        break;          
                    }
                }

                // -------------------------- Expansion ---------------------------------
                // if not visited is Not equal to -1, then we want to try a new move that not been visited before
                if (not_visited != -1) {
                    // create expand move , and based on the current node and not visted move index
                    Move expand_move = choiceToMove(current, not_visited);
                    // update current to its child base on the move epaand move
                    current = current.getChild(expand_move);
                    
                    // update move path and node path
                    move_path.add(not_visited);
                    node_path.add(current);

                    // break the loop and we are going to simulate this new move
                    break; 
                
                //----------------------Selection ----------------------------
                // otherwise (when not visted index in -1, mean we try all move at lealst 1 time)
                } else {
                    // Bug: i should not assume my oppoent also pick the action that maximize my ucb score
                    // use getbestUcb to get the best ucb value we can have
                    // default best_ucb
                    int best_ucb;

                    // if the current turn is my turn i call get best ucb to get the best ucb as my perspective
                    if (current.getLogicalPlayerIdx() == this.getLogicalPlayerIdx()) {
                        best_ucb = getBestUCB(current);
                    
                    // otherwise it is not my turn; which should random choose a best ucb
                    } else {
                        best_ucb = this.getRandom().nextInt(getNumberOfChoices(current));
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

            // --- Testing ---
            // System.out.println("B");

            // ---------------- Rollout -----------------------
            float rollout_value = rollout(current.getGameView());

            // --- Testing ---
            // System.out.println("C");

            // ----------- Backpropagation ------------------
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
// --observability {FULL,PARTIAL_NO_DECK,PARTIAL_NO_DECK_NO_HANDS}
// -m MAXTHINKINGTIMEINMS, --maxThinkingTimeInMS MAXTHINKINGTIMEINMS
// thinking time (for each player) PER MOVE in milli-
// seconds. (default: 4688)
// --colorblind           Use  colorblind-friendly   card   and   asset  set
// (default: false)
// java -cp "./lib/*;." edu.bu.pas.uno.SingleGameMain edu.bu.pas.uno.agents.RandomAgent src.pas.uno.agents.UnoMCTSAgent --colorblind true --observability PARTIAL_NO_DECK_NO_HANDS