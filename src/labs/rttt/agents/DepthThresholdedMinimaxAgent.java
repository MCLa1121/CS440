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


public class DepthThresholdedMinimaxAgent
    extends SearchAgent
{

    public static final int DEFAULT_MAX_DEPTH = 3;

    private int maxDepth;

    public DepthThresholdedMinimaxAgent(PlayerType myPlayerType)
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

    public Node minimax(Node node)
    {
        // uncomment if you want to see the tree being made
        // System.out.println(this.getTabs(node) + "Node(currentPlayer=" + node.getCurrentPlayerType() +
        //      " isTerminal=" + node.isTerminal() + " lastMove=" + node.getLastMove() + ")");

        /**
         * TODO: complete me!
         */
        int depth = getMaxDepth();
        //check whether the node it the termal node or we have reach depth = 0
        if(node.isTerminal()){
            return node;
        }
        //if our final node is an artifical node
        //we set the utilty value ourself
        if(node.getDepth() >= depth){
            double estimate = Heuristics.calculateHeuristicValue(node);
            node.setUtilityValue(estimate);
            return node;
        }
        if(node.getCurrentPlayerType() == node.getMyPlayerType()){
            //if this is our turn to move
            //we get the max move 
            //set value a to be negative infinity
            double a = Double.NEGATIVE_INFINITY;
            //first by getting the children of the current node
            for (Node children : node.getChildren()) {
                double value = Heuristics.calculateHeuristicValue(children);
                a = Math.max(a,value);
                minimax(children);
            }
            
            
        }else{
            //our oppotent to move
            //choose the min move
            //set value a to be infinity
            double a = Double.POSITIVE_INFINITY;
            //first by getting the children of the current node
            for (Node children : node.getChildren()) {
                double value = Heuristics.calculateHeuristicValue(children);
                a = Math.max(a,value);
                depth--;
                minimax(children);
            }
        }
        return node;
    }

    public Node search(Node node)
    {
        return this.minimax(node);
    }

    @Override
    public void afterGameEnds(final RecursiveTicTacToeGameView game) {}
}
//javac -cp "./lib/*;." @rttt.srcs
//java -cp "./lib/*;." edu.bu.labs.rttt.Main -o src.labs.rttt.agents.DepthThresholdedMinimaxAgent -x src.labs.rttt.agents.DepthThresholdedMinimaxAgent