package src.pas.pacman.routing;


import java.net.CookieHandler;
// SYSTEM IMPORTS
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;

// JAVA PROJECT IMPORTS
import edu.bu.pas.pacman.agents.Agent;
import edu.bu.pas.pacman.game.Action;
import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.game.Tile;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.routing.BoardRouter;
import edu.bu.pas.pacman.routing.BoardRouter.ExtraParams;
import edu.bu.pas.pacman.utils.Coordinate;
import edu.bu.pas.pacman.utils.Pair;



// This class is responsible for calculating routes between two Coordinates on the Map.
// Use this in your PacmanAgent to calculate routes that (if followed) will lead
// Pacman from some Coordinate to some other Coordinate on the map.
public class ThriftyBoardRouter
    extends BoardRouter
{
// this is the estimate function 
        public int Cost( Coordinate a ,Coordinate b ){
            
            int x1 = a.x();
            int x2 = b.x();
            int y1 = a.y();
            int y2 = b.y();
            int dx = Math.abs(x1 - x2);
            int dy = Math.abs(y1 - y2);
            return dx + dy;
        }
    // If you want to encode other information you think is useful for Coordinate routing
    // besides Coordinates and data available in GameView you can do so here.
    public static class BoardExtraParams
        extends ExtraParams
    {
        
    }

    // feel free to add other fields here!
    

    public ThriftyBoardRouter(int myUnitId,
                              int pacmanId,
                              int ghostChaseRadius)
    {
        super(myUnitId, pacmanId, ghostChaseRadius);

        // if you add fields don't forget to initialize them here!
    }


    @Override
    public Collection<Coordinate> getOutgoingNeighbors(final Coordinate src,
                                                       final GameView game,
                                                       final ExtraParams params)
    {
        // TODO: implement me!
        //create a collection (data type to be considered)
        Collection<Coordinate> neighbour = new LinkedList<Coordinate>(); 
        //get current coordinate 
        Coordinate current = src;
        int x = current.x();
        int y = current.y();

        //get all the neighbour coordinate 
        //moving right 
        int dx = x + 1; 
        Coordinate right = new Coordinate(dx, y);
        //checking the tile 
        Tile move = game.getTile(right);
        //check whether the new tile is wall or not, if it is not a wall
        //add it in to the list 
        if(!move.getState().equals(Tile.State.WALL)){
            neighbour.add(right);
        }
        
        //moving uo 
        int dy = y + 1; 
        Coordinate up = new Coordinate(x, dy);
        Tile moveUP = game.getTile(up);
        if(!moveUP.getState().equals(Tile.State.WALL)){
            neighbour.add(up);
        }

        //moving left 
        int L = x - 1;
        Coordinate left = new Coordinate(L, y);
        Tile moveL = game.getTile(left);
        if(!moveL.getState().equals(Tile.State.WALL)){
            neighbour.add(left);
        }

        //moving down 
        int D = y - 1;
        Coordinate down = new Coordinate(x, D);
        Tile moveD = game.getTile(down);
        if(!moveD.getState().equals(Tile.State.WALL)){
            neighbour.add(down);
        }
        
        
        return neighbour;
    }

    @Override
    public Path<Coordinate> graphSearch(final Coordinate src,
                                        final Coordinate tgt,
                                        final GameView game)
    {
        // TODO: implement me!
        //Create a priotity queue with f = g + h 
        PriorityQueue<Path<Coordinate>> checked = new PriorityQueue<>(Comparator.comparingDouble(p -> p.getTrueCost() + p.getEstimatedPathCostToGoal()));
        
        //initializating a start path 
        Path<Coordinate> start = new Path<Coordinate>(src);
        checked.add(start);
        start.setEstimatedPathCostToGoal(Cost(src, tgt));
        while(!checked.isEmpty()){
            Path<Coordinate> current = checked.poll();
            //general idea
            //get the current coordinate 

            //get and record  all the e=neighbour of the currnet coordinate 

            //find all the cost of the neighbour 

            //by adding all cost with the heuristic function, find the smallest

            //update current node to be the parent of the smallest node

            //add current node to the came_from list, meaning it is fixed
            
            
        }
        return null;
    }

}

