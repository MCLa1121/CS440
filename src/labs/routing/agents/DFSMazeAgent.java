package src.labs.routing.agents;

// SYSTEM IMPORTS
import edu.bu.labs.routing.Coordinate;
import edu.bu.labs.routing.Direction;
import edu.bu.labs.routing.Path;
import edu.bu.labs.routing.State.StateView;
import edu.bu.labs.routing.Tile;
import edu.bu.labs.routing.agents.MazeAgent;

import java.util.Collection;
import java.util.HashSet;   // will need for dfs
import java.util.Stack;     // will need for dfs
import java.util.Set;       // will need for dfs


// JAVA PROJECT IMPORTS


public class DFSMazeAgent
    extends MazeAgent
{

    public DFSMazeAgent(final int agentId)
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

    //creating direction
    Direction UP_Right = Direction.UP_RIGHT;
        Direction UP_Left = Direction.UP_LEFT;
        Direction DOWN_Right = Direction.DOWN_RIGHT;
        Direction DOWN_Left = Direction.DOWN_LEFT;
        Direction Up = Direction.UP;
        Direction Left = Direction.LEFT;
        Direction Right = Direction.RIGHT;
        Direction Down = Direction.DOWN;
    //create an array to store all the direction
        Direction[] AllDir = {Up, Left, Right, Down, UP_Left, UP_Right, DOWN_Left, DOWN_Right  };

        public Direction[] getAllDirections() {
            return AllDir;
        }

    @Override
    public Path<Coordinate> search(final Coordinate src,
                                   final Coordinate goal,
                                   final StateView stateView)
    {
        // TODO: complete me!
        
        //we create a stack to store the node we are going to process 
        Stack<Path<Coordinate>> stack = new Stack<>(); 
        //Create a hashset to store node we have visited 
        HashSet<Coordinate> visited = new HashSet<>();
        //create a path started from the src 
        Path<Coordinate> start = new Path<Coordinate>(src);
        //add the src to the stack
        stack.add(start);
        while(!stack.isEmpty()){
            // first we pop the element in the stack, which is our first or current path 
            Path<Coordinate> currentPath = stack.pop();
            // getting the coordinate of the new path
            Coordinate cur = currentPath.current();
            // we add this to the visited set if it is not in it 
            if(!visited.contains(cur)){
                visited.add(cur);
            }
        }
        return null;
    }

}
