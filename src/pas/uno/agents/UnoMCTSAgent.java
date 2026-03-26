package src.pas.uno.agents;


// SYSTEM IMPORTS
import edu.bu.pas.uno.Card;
import edu.bu.pas.uno.Game;
import edu.bu.pas.uno.Game.GameView;
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


public class UnoMCTSAgent
    extends MCTSAgent
{
    // create a proxyAgentclass and create the proxyAgent constructor
    private static class ProxyAgent extends Agent {
        public ProxyAgent(int playerIdx, long maxThinkingTimeInMS) {
            super(playerIdx, maxThinkingTimeInMS);
        }

        @Override
        public Move chooseCardToPlay(Game.GameView game) {
            return null; // never used
        }

        @Override
        public Move maybePlayDrawnCard(Game.GameView game, int drawnCardIdx) {
            return null; // never used
        }
    }
    private ProxyAgent[] proxyAgents;

    public static class MCTSNode
        extends Node
    {
        private final Agent[] simulationAgents;
        private final Integer drawnCardIdx;
        public MCTSNode(final GameView game,
                        final int logicalPlayerIdx,
                        final Node parent,
                        final Agent[] simulationAgents,
                        final Integer drawnCardIdx)
        {
            super(game, logicalPlayerIdx, parent);
            this.simulationAgents = simulationAgents; 
            this.drawnCardIdx = drawnCardIdx;
        }

        public Integer getDrawnCardIdx() {
            return this.drawnCardIdx;
        }

        // getChild method: return a mctsnode
        @Override
        public Node getChild(final Move move)
        {
            throw new UnsupportedOperationException("Task 6 version does not use child simulation");
        }
    }

    public UnoMCTSAgent(final int playerIdx,
                    final long maxThinkingTimeInMS)
    {
        super(playerIdx, maxThinkingTimeInMS);
    }



    // ---------------------------- PRIVATE HELPER -------------------------------------------------------------------------------------------------------------------------
    // private helper -------------- help to make the choice to move
    private Move choiceToMove(Node node, int index) {
        int player_index = node.getLogicalPlayerIdx();
        // if can legally move
        if (node.getNodeState() == Node.NodeState.HAS_LEGAL_MOVES) {
            return wildMove(node.getGameView(),player_index, node.getOrderedLegalMoves().get(index));
        }

        // if no leagal move and no unresloved card persent reutnr null which here we need to draw card
        if (node.getNodeState() == Node.NodeState.NO_LEGAL_MOVES_UNRESOLVED_CARDS_PRESENT) {
            return null;
        }

        // if no legal move , and may play the drawn card 
        if (node.getNodeState() == Node.NodeState.NO_LEGAL_MOVES_MAY_PLAY_DRAWN_CARD) {
            // if we can play the card
            if (index == Node.NoLegalMovesIdxDefaults.DrawSingleCardIdxs.KEEP_CARD_MOVE_IDX) {
                // return null if no legal move allowed
                return null;
            }
            Integer drawnCardIdx = ((MCTSNode) node).getDrawnCardIdx();

            if (drawnCardIdx == null) {
                return null; 
            }

            return wildMove(node.getGameView(), player_index, drawnCardIdx);
        }
        return null; // return null if we make the draw and do nothing
    }

    // private helper ------------- help to make the choic to move if we have wild card
    private Move wildMove(GameView game, int player_index, int card_index) {
        Card card_in_hand = game.getHandView(player_index).getCard(card_index);
        ProxyAgent proxyAgent = proxyAgents[player_index];

        if (card_in_hand.isWild()) {
            return Move.createMove(proxyAgent, card_index,Color.getRandomColor(getRandom()));
        }
        
        return Move.createMove(proxyAgent, card_index);
    }

    // private helper -------------help to rollout
    private float rollout(Node node, int index) {
        int playerIdx = node.getLogicalPlayerIdx();
        HandView hand = node.getGameView().getHandView(playerIdx);
    
        if (node.getNodeState() == Node.NodeState.NO_LEGAL_MOVES_UNRESOLVED_CARDS_PRESENT) {
            return -0.5f;
        }
    
        if (node.getNodeState() == Node.NodeState.NO_LEGAL_MOVES_MAY_PLAY_DRAWN_CARD) {
            if (index == Node.NoLegalMovesIdxDefaults.DrawSingleCardIdxs.KEEP_CARD_MOVE_IDX) {
                return -0.1f;
            }
    
            Integer drawnCardIdx = ((MCTSNode) node).getDrawnCardIdx();
            if (drawnCardIdx == null) {
                return -0.1f;
            }
    
            Card drawn = hand.getCard(drawnCardIdx);
            float score = 0.4f;
            if (drawn.isAction()) {
                score += 0.2f;
            }
            if (drawn.isWild()) {
                score += 0.1f;
            }
            if (hand.size() == 1) {
                score += 1.0f;
            }
            return score;
        }
    
        int cardIdx = node.getOrderedLegalMoves().get(index);
        Card c = hand.getCard(cardIdx);
    
        float score = 1.0f; // playing a legal card is already good
    
        if (c.isAction()) {
            score += 0.25f;
        }
        if (c.isWild()) {
            score += 0.10f;
        }
        if (hand.size() == 1) {
            score += 1.0f; // this play empties your hand
        } else if (hand.size() == 2) {
            score += 0.35f; // this play leaves you with one card
        }
    
        return score;
        
    }

    // private helper getNumberOfchoices ------------ get the number of choice we can have
    private int getNumberOfChoices(Node node) {
        if (node.getNodeState() == Node.NodeState.HAS_LEGAL_MOVES) {
            return node.getOrderedLegalMoves().size();
        } else if (node.getNodeState() == Node.NodeState.NO_LEGAL_MOVES_MAY_PLAY_DRAWN_CARD) {
            Integer drawn = ((MCTSNode) node).getDrawnCardIdx();
            return drawn == null ? 1 : 2;
        } else {
            return 1;
        }
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
        if (proxyAgents == null) {
            int n = game.getNumPlayers();
            proxyAgents = new ProxyAgent[n];
    
            for (int i = 0; i < n; i++) {
                int actualPlayerIdx = game.getPlayerOrder().getAgentIdx(i);
                proxyAgents[i] = new ProxyAgent(actualPlayerIdx, this.getMaxThinkingTimeInMS());
                proxyAgents[i].setLogicalPlayerIdx(i);
            }
        }
    
        int player_root_index = game.getPlayerOrder().getCurrentLogicalPlayerIdx();
        MCTSNode root_node = new MCTSNode(game, player_root_index, null, proxyAgents, drawnCardIdx);
    
        int choices = getNumberOfChoices(root_node);
    
        for (int i = 0; i < choices; i++) {
            float value = rollout(root_node, i);
            root_node.setQCount(i, 1);
            root_node.setQValueTotal(i, value);
        }
    
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
// java -cp "./lib/*;." edu.bu.pas.uno.SingleGameMain edu.bu.pas.uno.agents.RandomAgent src.pas.uno.agents.UnoMCTSAgent 
// --observability {FULL,PARTIAL_NO_DECK,PARTIAL_NO_DECK_NO_HANDS}
// -m MAXTHINKINGTIMEINMS, --maxThinkingTimeInMS MAXTHINKINGTIMEINMS
// thinking time (for each player) PER MOVE in milli-
// seconds. (default: 4688)
// --colorblind           Use  colorblind-friendly   card   and   asset  set
// (default: false)
// java -cp "./lib/*;." edu.bu.pas.uno.SingleGameMain --colorblind -o PARTIAL_NO_DECK_NO_HANDS edu.bu.pas.uno.agents.RandomAgent src.pas.uno.agents.UnoMCTSAgent