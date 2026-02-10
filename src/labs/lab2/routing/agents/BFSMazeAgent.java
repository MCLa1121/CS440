package src.labs.routing.agents;



// SYSTEM IMPORTS
import edu.bu.labs.routing.Coordinate;
import edu.bu.labs.routing.Direction;
import edu.bu.labs.routing.Path;
import edu.bu.labs.routing.State.StateView;
import edu.bu.labs.routing.Tile;
import edu.bu.labs.routing.agents.MazeAgent;


import java.util.Collection;
import java.util.HashSet;       // will need for bfs
import java.util.Queue;         // will need for bfs
import java.util.LinkedList;    // will need for bfs
import java.util.Set;


// JAVA PROJECT IMPORTS


public class BFSMazeAgent
    extends MazeAgent
{

    public BFSMazeAgent(final int agentId)
    {
        super(agentId);
    }

    @Override
    public void initializeFromState(final StateView stateView)
    {
        // find the FINISH tile
        Coordinate finishCoord = null;
        for(int rowIdx = 0; rowIdx < stateView.getNumRows(); ++rowIdx)
        {
            for(int colIdx = 0; colIdx < stateView.getNumCols(); ++colIdx)
            {
                if(stateView.getTileState(new Coordinate(rowIdx, colIdx)) == Tile.State.FINISH)
                {
                    finishCoord = new Coordinate(rowIdx, colIdx);
                }
            }
        }
        this.setFinishCoordinate(finishCoord);

        // make sure to call the super-class' version!
        super.initializeFromState(stateView);
    }

    @Override
    public boolean shouldReplacePlan(final StateView stateView)
    {
        return false;
    }

    @Override
    public Path<Coordinate> search(final Coordinate src,
                                   final Coordinate goal,
                                   final StateView stateView)
    {
        //TODO: complete me!
        //this is the queue used to store the node we have discovered but not finished 
        Queue<Path<Coordinate>> queue = new LinkedList<>();
        //this is the set used to store the node we have already visited
        HashSet<Coordinate> visited = new HashSet<>();

        //initialize a starting path
        Path<Coordinate> start = new Path<>(src);
        //we add the fist path to the queue 
        queue.add(start);
        //implementing BFS
        while (!queue.isEmpty()){

        }
        return null;
    }

}
