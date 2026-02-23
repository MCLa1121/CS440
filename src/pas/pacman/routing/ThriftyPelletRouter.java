package src.pas.pacman.routing;


import java.util.ArrayList;
// SYSTEM IMPORTS
import java.util.Collection;
import java.util.LinkedList;

// JAVA PROJECT IMPORTS
import edu.bu.pas.pacman.game.Action;
import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.game.Tile;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.graph.PelletGraph.PelletVertex;
import edu.bu.pas.pacman.routing.PelletRouter;
import edu.bu.pas.pacman.routing.PelletRouter.ExtraParams;
import edu.bu.pas.pacman.utils.Coordinate;
import edu.bu.pas.pacman.utils.Pair;


public class ThriftyPelletRouter
    extends PelletRouter
{

    // If you want to encode other information you think is useful for planning the order
    // of pellets ot eat besides Coordinates and data available in GameView
    // you can do so here.
    public static class PelletExtraParams
        extends ExtraParams
    {

    }

    // feel free to add other fields here!

    public ThriftyPelletRouter(int myUnitId,
                               int pacmanId,
                               int ghostChaseRadius)
    {
        super(myUnitId, pacmanId, ghostChaseRadius);

        // if you add fields don't forget to initialize them here!
    }

    @Override
    public Collection<PelletVertex> getOutgoingNeighbors(final PelletVertex src,
                                                         final GameView game,
                                                         final ExtraParams params)
    {
        // TODO: implement me!
        //create a collection Note: Data type: Collection<PelletVertex>; using Arraylist to store the neighbour
        Collection<PelletVertex> neighbour = new ArrayList<>(); 

        //get current coordinate: create object current that gather info from game 
        PelletVertex current = new PelletVertex(game);

        // get pacman location
        Coordinate current_pac_location = current.getPacmanCoordinate();

        // get remaining pellets in the game and store all the information to linklist
        LinkedList<Coordinate> current_remaining_pellet = new LinkedList<>(current.getRemainingPelletCoordinates());

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

        // return null;
    }

    @Override
    public float getEdgeWeight(final PelletVertex src,
                               final PelletVertex dst,
                               final ExtraParams params)
    {
        // TODO: implement me!
        return 1f;
    }

    @Override
    public float getHeuristic(final PelletVertex src,
                              final GameView game,
                              final ExtraParams params)
    {
        // TODO: implement me!
        // function heuristic(node) =
        // dx = abs(node.x - goal.x)
        // dy = abs(node.y - goal.y)
        // return D * (dx + dy)


        return 0f;
    }

    @Override
    public Path<PelletVertex> graphSearch(final GameView game) 
    {
        // TODO: implement me!
        // frontier = PriorityQueue()
        // frontier.put ( start , 0)
        // came_from = dict()
        // cost_so_far = dict()
        // came_from [ start ] = None
        // cost_so_far [ start ] = 0 while not frontier.empty ():
        //    current = frontier.get () if current == goal :
        //       break for next in graph.neighbors( current ):
        //       new_cost = cost_so_far [ current ] + graph.cost( current , next )
        //       if next not in cost_so_far or new_cost < cost_so_far [ next ]:
        //          cost_so_far [ next ] = new_cost 
        //         priority = new_cost + heuristic( goal , next ) frontier.put ( next , priority)
        //          came_from [ next ] = current
        return null;
    }

}

