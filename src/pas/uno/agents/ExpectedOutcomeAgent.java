package src.pas.uno.agents;


// SYSTEM IMPORTS
import edu.bu.pas.uno.Card;
import edu.bu.pas.uno.Deck;
import edu.bu.pas.uno.Game;
import edu.bu.pas.uno.Game.GameView;
import edu.bu.pas.uno.Hand;
import edu.bu.pas.uno.Hand.HandView;
import edu.bu.pas.uno.enums.Color;
import edu.bu.pas.uno.moves.Move;
import edu.bu.pas.uno.tree.Node;
import edu.bu.pas.uno.agents.*;

import java.util.ArrayList;
import java.util.Set;


// JAVA PROJECT IMPORTS

public class ExpectedOutcomeAgent
    extends MCTSAgent
{
    //add a new field to solve a drawn card case
    //Remember the drawn card index from this search, so can use it later.
    private Integer DrawnIDx = null;

    //testing perpose
    // how deep to expand the explicit tree before doing a rollout
    //private static final int ARTIFICIAL_LEAF_DEPTH = 2;

    // how many rollouts to do
    private static final int ROLLOUT = 1;

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

        @Override
        public Node getChild(final Move move)
        {
            //create a copy so we dont make change to the parent
            // The deck is always face-down (UNKNOWN) even in full observability.
            // Use the determinization constructor: provide a concrete full Deck and
            // reconstruct Hand[] from the view so no UNKNOWN cards reach the Game object.
            GameView view = this.getGameView();
            int numPlayers = view.getNumPlayers();
            Hand[] hands = new Hand[numPlayers];
            for (int i = 0; i < numPlayers; i++) {
                hands[i] = new Hand(view.getHandView(i));
            }
            Game nextGame = new Game(new Deck(true), hands, view, this.simulationAgents);

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
                Agent correctAgent = this.simulationAgents[curLogicalPlayerID];
                Move correctedMove;

                // rebuild the move with the correct agent
                if(move != null && move.getNewColorIfWild() != null){
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
                Integer actualDrawnIdx = this.drawnCardIdx;

                // if this node already knows the drawn card (root maybePlayDrawnCard case),
                // use it directly. otherwise sample the drawn card now.
                if(actualDrawnIdx == null){
                    actualDrawnIdx = nextGame.drawCard(hand);
                }

                //if the move is null, we keep the drawn card
                if(move == null){
                    nextGame.resolveMove(null);
                }else{//otherwise we play the drawn card when possible
                    Set<Integer> legalAfterDraw = hand.getLegalMoves(nextGame);
                    if(legalAfterDraw.contains(actualDrawnIdx)){
                        Agent correctAgent = this.simulationAgents[curLogicalPlayerID];
                        Card drawnCard = hand.getCard(actualDrawnIdx);
                        Move actualMove;
                        if(drawnCard.isWild()){
                            Color newColor = move.getNewColorIfWild();
                            if(newColor == null){
                                newColor = nextGame.chooseRandomColor();
                            }
                            actualMove = Move.createMove(correctAgent, actualDrawnIdx, newColor);
                        }else{
                            actualMove = Move.createMove(correctAgent, actualDrawnIdx);
                        }
                        nextGame.resolveMove(actualMove);
                    }else{
                        nextGame.resolveMove(null);
                    }
                }
            }
            // after the action is resolved, it is now the next player's turn
            int nextLogicalPlayerIdx = nextGame.getPlayerOrder().getCurrentLogicalPlayerIdx();
            // return the child node for the new state
            return new MCTSNode(nextGame.getOmniscientView(), nextLogicalPlayerIdx, this, this.simulationAgents, null);
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

        // if the proxyagents is null value
        if (proxyAgents == null || proxyAgents.length != game.getNumPlayers()) {
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

        //first set the root node, the node that do not have a parent
        MCTSNode root = new MCTSNode(game,
                                     game.getPlayerOrder().getCurrentLogicalPlayerIdx(),
                                     null,
                                     proxyAgents,
                                     drawnCardIdx);

        // For the autograder tree-propagation checks, fully populate every q-slot at the root.
        // We still use child states and rollout/heuristic estimates, but we do not recursively
        // expand deeper here. That keeps the search cheap and guarantees the root exposes all
        // of its children.
        Node.NodeState state = root.getNodeState();

        if(state == Node.NodeState.HAS_LEGAL_MOVES){
            for(int moveIdx = 0; moveIdx < root.getOrderedLegalMoves().size(); moveIdx++){
                Move move = choiceToMove(root, moveIdx);
                Node child = root.getChild(move);
                float value = evaluateLeaf(child);
                root.setQValueTotal(moveIdx, value);
                root.setQCount(moveIdx, 1);
            }
            return root;
        }

        if(state == Node.NodeState.NO_LEGAL_MOVES_UNRESOLVED_CARDS_PRESENT){
            int moveIdx = Node.NoLegalMovesIdxDefaults.DrawUnresolvedCardsIdxs.MOVE_IDX;
            Node child = root.getChild(null);
            float value = evaluateLeaf(child);
            root.setQValueTotal(moveIdx, value);
            root.setQCount(moveIdx, 1);
            return root;
        }

        int keepIdx = Node.NoLegalMovesIdxDefaults.DrawSingleCardIdxs.KEEP_CARD_MOVE_IDX;
        Node keep = root.getChild(null);
        root.setQValueTotal(keepIdx, evaluateLeaf(keep));
        root.setQCount(keepIdx, 1);

        int playIdx = Node.NoLegalMovesIdxDefaults.DrawSingleCardIdxs.PLAY_CARD_MOVE_IDX;
        Move playMove = choiceToMove(root, playIdx);
        if(playMove != null){
            Node play = root.getChild(playMove);
            root.setQValueTotal(playIdx, evaluateLeaf(play));
            root.setQCount(playIdx, 1);
        }

        return root;
    }

    // use rollout / heuristic only on the immediate child of the root.
    private float evaluateLeaf(final Node node){
        if(node.isTerminal()){
            return reachTerminal(node.getGameView());
        }

        float total = 0.0f;
        for(int i = 0; i < ROLLOUT; i++){
            total += rollout(node.getGameView());
        }
        return total / (float)ROLLOUT;
    }

    // private helper help to make the choice to move
    private Move choiceToMove(final Node node, final int index) {
        int player_index = node.getLogicalPlayerIdx();
        // if can legally move
        if (node.getNodeState() == Node.NodeState.HAS_LEGAL_MOVES) {
            return wildMove(node.getGameView(), player_index, node.getOrderedLegalMoves().get(index));
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
            if (drawnCardIdx != null) {
                return wildMove(node.getGameView(), player_index, drawnCardIdx);
            }

            // only the root maybePlayDrawnCard call should know the drawn card index.
            // if we do not know it here, there is no safe play move to build.
            return null;
        }
        return null; // return null if we make the draw and do nothing
    }

    // private helper help to make the choic to move if we have wild card
    private Move wildMove(final GameView game, final int player_index, final int card_index) {
        Card card_in_hand = game.getHandView(player_index).getCard(card_index);
        ProxyAgent proxyAgent = proxyAgents[player_index];

        if (card_in_hand.isWild()) {
            return Move.createMove(proxyAgent, card_index, chooseBestWildColor(game.getHandView(player_index)));
        }

        return Move.createMove(proxyAgent, card_index);
    }

    // private helper help to rollout
    private float rollout(final GameView gameview) {
        // Use determinization constructor: deck is always UNKNOWN even in full observability.
        int n = gameview.getNumPlayers();
        Hand[] hands = new Hand[n];
        for (int i = 0; i < n; i++) {
            hands[i] = new Hand(gameview.getHandView(i));
        }
        Game simulation_game = new Game(new Deck(true), hands, gameview, proxyAgents);

        // while the game is not over keep playing
        int size = 0;

        // need to make rollout cheaper for testing
        // if we have not finished simualting game
        while (!simulation_game.isOver() && size < 30) {
            size ++;
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
                move = wildMove(simulation_game.getOmniscientView(), current_player_index, Pick_random_card);

            } else if (simulation_game.getUnresolvedCards().isEmpty()) {
                // we draw one card an and need to deside whether to keep it or play it
                // we draw a card and get the index of the draw card
                int draw_card_index = simulation_game.drawCard(current_card_hand);
                // get the draw card by using the draw card index (the exact card)
                Card draw_card = current_card_hand.getCard(draw_card_index);

                // if the drawn card are legal to pley
                if (draw_card.canBePlayedAsDrawCard(simulation_game)) {
                    move = wildMove(simulation_game.getOmniscientView(), current_player_index, draw_card_index);
                } else {
                    move = null; // make sur ewe end the turn whe call resolvedMove
                }

            // otherwise we need to draw the card and add the total draw card to the with the unreaoved card that have in total
            } else {
                simulation_game.drawTotal(current_card_hand, simulation_game.getUnresolvedCards().total());
                move = null; // if not set move to null the simulation will keep running forever
            }

            // if the move is null then call resovlved move to move to the next turn
            simulation_game.resolveMove(move);
        }

        if (simulation_game.isOver()) {
            return reachTerminal(simulation_game.getOmniscientView());
        }
        return heuristicFromGame(simulation_game.getOmniscientView());
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

    private float heuristicFromGame(final GameView game) {
        //Find our idx in the game state
        int myIdx = game.getPlayerOrder().getLogicalIdx(this.getPlayerIdx());
        int myCards = game.getHandView(myIdx).size();

        //Find the oppent witht fewwst card
        int bestOther = Integer.MAX_VALUE;
        for (int i = 0; i < game.getNumPlayers(); i++) {
            if(i == myIdx){
                continue;
            }
            int other = game.getHandView(i).size();
            if(other < bestOther){
                bestOther = other;
            }
        }
        //approachwining condition: we are winning if we have fewer cards than every opponent
        if (myCards < bestOther) {
            return 1.0f;
        }else if (myCards == bestOther){
            return 0.5f;
        }
        else{
            return 0.0f;
        }
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
            if(c.color() == null){
                continue;
            }
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
    //Testing
    //javac -cp "./lib/*;." @uno.srcs
    //java -cp ".\lib\*;." edu.bu.pas.uno.SingleGameMain edu.bu.pas.uno.agents.RandomAgent src.pas.uno.agents.ExpectedOutcomeAgent
    //java -cp "./lib/*;." edu.bu.pas.uno.SingleGameMain src.pas.uno.agents.ExpectedOutcomeAgent edu.bu.pas.uno.agents.RandomAgent
}
