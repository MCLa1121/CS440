package src.pas.pacman.routing;


import java.util.ArrayList;
// SYSTEM IMPORTS
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;

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
        PelletVertex current = src;

        // get remaining pellets in the game and store all the information to linklist
        LinkedList<Coordinate> current_remaining_pellet = new LinkedList<>(current.getRemainingPelletCoordinates());

        //get all the neighbour coordinate using for loop to iterate over current remaining pellet
        // it help to save all the possible case: what if we eat this pellet. eat this(different pellet); remove this; move there; save to neighbor
        for (Coordinate d : current_remaining_pellet) {

            // the removePellet: remove the pellet at d, and move pacman to d, and the current pelletvertex will have the update status
            neighbour.add(current.removePellet(d));
        }
        
        // After we store all the possible case we can eat the pellet, we return it
        return neighbour;

        // return null;
    }

    // getEdgeWeight. This method takes two PelletVertex objects that are assumed to be neighbors
    // and a PelletExtraParams object. This method should decide how expensive the edge weight
    // is to go from src and arrive at dst. Be careful! Remember that edge weights should be nonnegative, 
    // and be sure not to break admissibility and consistency of your heuristic!
    @Override
    public float getEdgeWeight(final PelletVertex src,
                               final PelletVertex dst,
                               final ExtraParams params)
    {   
        // TODO: implement me!
        // src and dst is neighbors, and the edge weight is src - dst using distance funtion
        // Calculate the actual distance between these two specific neighbors
        Coordinate src_pac = src.getPacmanCoordinate();
        Coordinate dst_pac = dst.getPacmanCoordinate();
        double src_x = src_pac.x();
        double src_y = src_pac.y();
        double dst_x = dst_pac.x();
        double dst_y = dst_pac.y();

        double weight = Math.hypot(src_x-dst_x, src_y - dst_y); 

        // BE SURE weight is NON NEGATIVE to prevent SO WE WILL NOT MESS UP THE ALGO
        return (float)Math.max(0f, weight);
    }

    @Override
    public float getHeuristic(final PelletVertex src,
                              final GameView game,
                              final ExtraParams params)
    {
        // TODO: implement me!
        
        // if no pelletes is left, return the cost zero
        if (src.getRemainingPelletCoordinates().isEmpty()) {
            return 0f;
        }

        
        // Default: pacmann location and a linklist of current remaining pellet coordinate
        Coordinate pacmann_location = src.getPacmanCoordinate();
        Collection<Coordinate> current_remaining_pellet = src.getRemainingPelletCoordinates();

        // --------------------------Avoid Ghost and Remaining Pellets --------------------------------------
        // if (current_remaining_pellet.isEmpty()){
        //     float min_dis = Float.MAX_VALUE ; 
        //     LinkedList<Coordinate> all_ghost_location = new LinkedList<>(game.);
        //     // avoid ghost become the first thing to do, else find the pellet and avoid gohst
        //     for (Coordinate c: all_ghost_location){
        //         double dx = Math.abs(pacmann_location.x() - c.x());
        //         double dy = Math.abs(pacmann_location.y() - c.y());
        //         double distance = dx + dy;
    
        //         // store the distance that is smaller
        //         if (distance < min_dis){
        //             min_dis = (float) distance;
        //         }
        //     } 
        // }

        // -------------------------- Find Pellet ------------------------------------------------
        float max_dis = 0f ; 
        // use a for loop to calculate each possibility of the future movement cost, and choose the cost that is 
        // the minimum and return the minimum distance
        for (Coordinate c: current_remaining_pellet){
            float dx = Math.abs(pacmann_location.x() - c.x());
            float dy = Math.abs(pacmann_location.y() - c.y());
            float distance = dx + dy;

            // store the distance that is smaller
            if (distance > max_dis){
                max_dis = distance;
            }
        }

        // return the min_dis as float type
        return max_dis;
    }

    @Override
    public Path<PelletVertex> graphSearch(final GameView game) 
    {
        // TODO: implement me!
        PelletVertex start = new PelletVertex(game);
        PriorityQueue<Path<PelletVertex>> openSet = new PriorityQueue<>( (p1,p2) -> Double.compare(p1.getEstimatedPathCostToGoal(), p2.getEstimatedPathCostToGoal()));
        Map<PelletVertex, Double> gScore = new HashMap<>();

        // set a visitedset so we can fix the outof memory issue
        HashSet<PelletVertex> visitedSet = new HashSet<>();

        openSet.add(new Path<>(start, (float) getHeuristic(start, game, null), null));
        gScore.put(start, 0.0);

        while (!openSet.isEmpty()) {
            Path<PelletVertex> currentPath = openSet.poll();
            PelletVertex currenVertex = currentPath.getDestination();

            // if visited we do not recalcuate the path; save memory
            if (visitedSet.contains(currenVertex)) {
                continue;
            }

            // if not visited add to the visied set
            visitedSet.add(currenVertex);


            if (currenVertex.getRemainingPelletCoordinates().isEmpty()) {
                return currentPath;
            }
            
            for (PelletVertex neighbor : getOutgoingNeighbors(currenVertex, game, null)) {
                double newG = gScore.get(currenVertex) + getEdgeWeight(currenVertex, neighbor, null);
                if (newG < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    gScore.put(neighbor, newG);
                    double newnewG = newG + getHeuristic(neighbor, game, null);
                    openSet.add(new Path<>(neighbor, (float)newG, (float)newnewG, currentPath));
                }
            }
        }
        return null;
    }

}

