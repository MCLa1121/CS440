package src.pas.pacman.routing;


import java.net.CookieHandler;
// SYSTEM IMPORTS
import java.util.Collection;
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

    // If you want to encode other information you think is useful for Coordinate routing
    // besides Coordinates and data available in GameView you can do so here.
    public static class BoardExtraParams
        extends ExtraParams
    {
        public int Cost( Coordinate a ,Coordinate b ){
            int x1 = a.x();
            int x2 = b.x();
            int y1 = a.y();
            int y2 = b.y();
            int dx = Math.abs(x1 - x2);
            int dy = Math.abs(y1 - y2);
            return dx + dy;
        }
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
        //Create a priotity queue 
        PriorityQueue<Path<Coordinate>> queue = new PriorityQueue<>();
        //initializating a start path 
        Path<Coordinate> start = new Path<Coordinate>(src);
        //current coordinate 
        Coordinate current = start.current();
        return null;
    }

    @Override
    public Path<Coordinate> graphSearch(final Coordinate src,
                                        final Coordinate tgt,
                                        final GameView game)
    {
        // TODO: implement me!
        return null;
    }

}

