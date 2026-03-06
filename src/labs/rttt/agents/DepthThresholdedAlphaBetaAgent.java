package src.labs.rttt.agents;


// SYSTEM IMPORTS
import edu.bu.labs.rttt.agents.SearchAgent;
import edu.bu.labs.rttt.game.CellType;
import edu.bu.labs.rttt.game.PlayerType;
import edu.bu.labs.rttt.game.RecursiveTicTacToeGame;
import edu.bu.labs.rttt.game.RecursiveTicTacToeGame.RecursiveTicTacToeGameView;
import edu.bu.labs.rttt.traversal.Node;
import edu.bu.labs.rttt.utils.Coordinate;
import edu.bu.labs.rttt.utils.Pair;

import java.util.List;
import java.util.Map;


// JAVA PROJECT IMPORTS
import src.labs.rttt.heuristics.Heuristics;
import src.labs.rttt.ordering.MoveOrderer;


public class DepthThresholdedAlphaBetaAgent
    extends SearchAgent
{

    public static final int DEFAULT_MAX_DEPTH = 3;

    private int maxDepth;

    public DepthThresholdedAlphaBetaAgent(PlayerType myPlayerType)
    {
        super(myPlayerType);
        this.maxDepth = DEFAULT_MAX_DEPTH;
    }

    public final int getMaxDepth() { return this.maxDepth; }
    public void setMaxDepth(int i) { this.maxDepth = i; }

    public String getTabs(Node node)
    {
        StringBuilder b = new StringBuilder();
        for(int idx = 0; idx < node.getDepth(); ++idx)
        {
            b.append("\t");
        }
        return b.toString();
    }

    public Node alphaBeta(Node node,
                          double alpha,
                          double beta)
    {
        // uncomment if you want to see the tree being made
        // System.out.println(this.getTabs(node) + "Node(currentPlayer=" + node.getCurrentPlayerType() +
        //      " isTerminal=" + node.isTerminal() + " lastMove=" + node.getLastMove() + ")");

        /**
         * TODO: complete me!
         */

                Node bestNode = null;
        //check whether the node it the termal node or we have reach depth = 0
        if(node.isTerminal()){
            return node;
        }
        //if our final node is an artifical node
        //we set the utilty value ourself
        if(node.getDepth() >= this.maxDepth){
            double estimate = Heuristics.calculateHeuristicValue(node);
            node.setUtilityValue(estimate);
            return node;
        }
        if(node.getCurrentPlayerType() == node.getMyPlayerType()){
            //if this is our turn to move
            //we get the max move 
            //set alpha value a to be negative infinity
            double a = Double.NEGATIVE_INFINITY;
            //first by getting the children of the current node
            for (Node children : node.getChildren()) {
                Node current = alphaBeta(children, alpha, beta);
                //check whether a is less than the current utility value
                if((a < current.getUtilityValue())){
                    //update the current best node 
                    bestNode = children;
                    //update a to the current utility value
                    a = bestNode.getUtilityValue();
                    alpha = current.getUtilityValue();
                }
                if(alpha >= beta){
                    break; //purn
                }
                
            }
            node.setUtilityValue(a);
        }else{
            //our oppotent to move
            //choose the min move
            //set beta value a to be infinity
            double b = Double.POSITIVE_INFINITY;
            //first by getting the children of the current node
            for (Node children : node.getChildren()) {
                Node current2 = alphaBeta(children, alpha, beta);
                if((b > current2.getUtilityValue())){
                    bestNode = children;
                    b = bestNode.getUtilityValue(); 
                    beta = current2.getUtilityValue();
                }
                if(alpha >= beta){
                    break; //purn
                }
            }
            node.setUtilityValue(b);
        }
        return bestNode;
    }


    public Node search(Node node)
    {
        return this.alphaBeta(node, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    @Override
    public void afterGameEnds(final RecursiveTicTacToeGameView game) {}
}
