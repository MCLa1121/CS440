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

    //create ordinal direction
        Direction UP_Right = Direction.UP_RIGHT;
        Direction UP_Left = Direction.UP_LEFT;
        Direction DOWN_Right = Direction.DOWN_RIGHT;
        Direction DOWN_Left = Direction.DOWN_LEFT;
    //create an array to store the ordinal direction
        Direction[] OrdinalDir = {UP_Left, UP_Right, DOWN_Left, DOWN_Right};

        public Direction getOrdinalDirections(int i) {
            return OrdinalDir[i];
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
            //enqueue so we know the current path 
            Path<Coordinate> CurrentPath = queue.poll();
            //get the current coordinate
            Coordinate cur = CurrentPath.current();

            for(Direction dir : Direction.getCardinalDirections()){
                //checking the new row and col for each move
                int newrow = cur.row() + dir.getDy();
                int newcol = cur.col() + dir.getDx();

                //checking whether the new row and col are vaild or nor
                if(newrow < 0 || newrow >= stateView.getNumRows() || newcol < 0 
                || newcol >= stateView.getNumCols()){
                    continue;
                }

                // if it is vaild, we create a new coordinate for it
                // since this is the neighbour of the current path coordination
                Coordinate neighbour = new Coordinate(newrow, newcol);

                // Check whether the Tile of the new coordinate is a wall or not 
                if(stateView.getTileState(neighbour) == Tile.State.WALL){
                    continue;
                }

                // check if have been visited or not
                if (visited.contains(neighbour)){
                    continue;
                }

                // now we can extend the path if it is a vaild path
                //since BFS is unweighted, we assume it is 1d
                Path<Coordinate> newPath = new Path<Coordinate>(CurrentPath, neighbour, 1d);

                //Check whether we have reach the goal for when reaching neighbour 
                if(neighbour.equals(goal)){
                    return newPath; //since BFS gives us the shortest once we reach the goal 
                }

                //if not the goal, we add it to the queue and mark it been visited
                queue.add(newPath);
                visited.add(neighbour); 
            }
        }
        return null;
    }
    
}
