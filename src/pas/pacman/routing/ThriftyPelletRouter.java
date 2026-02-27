package src.pas.pacman.routing;

import java.util.ArrayList;
// SYSTEM IMPORTS
import java.util.Collection;
import java.util.Comparator;
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



        // get remaining pellets in the game and store all the information to collection
        Collection<Coordinate> current_remaining_pellet = src.getRemainingPelletCoordinates();
        //create a arraylist Note: Data type: Arraylist<PelletVertex>; using Arraylist to store the neighbour
        ArrayList<Coordinate> pellet_arr_list = new ArrayList<>(current_remaining_pellet); 
        Coordinate pacman_location = src.getPacmanCoordinate();

        pellet_arr_list.sort(Comparator.comparingInt(p -> Math.abs(pacman_location.x() - p.x()) + Math.abs(pacman_location.y() - p.y())));
        
        final int set_limit = 4;
        int limit_size = Math.min(set_limit, pellet_arr_list.size()); // pruning for 4 best pellets

        ArrayList<PelletVertex> neighbour = new ArrayList<>(limit_size);
        //get all the neighbour coordinate using for loop to iterate over current remaining pellet
        // it help to save all the possible case: what if we eat this pellet. eat this(different pellet); remove this; move there; save to neighbor
        for (int i = 0; i < limit_size; i++) {
            neighbour.add(src.removePellet(pellet_arr_list.get(i)));
        }
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
        float src_x = src_pac.x();
        float src_y = src_pac.y();
        float dst_x = dst_pac.x();
        float dst_y = dst_pac.y();

        float weight = Math.abs(src_x-dst_x) + Math.abs(src_y - dst_y); 

        // BE SURE weight is NON NEGATIVE to prevent SO WE WILL NOT MESS UP THE ALGO
        return weight;
    }

    @Override
    public float getHeuristic(final PelletVertex src,
                              final GameView game,
                              final ExtraParams params)
    {
        // TODO: implement me!
        Collection<Coordinate> current_remaining_pellet = src.getRemainingPelletCoordinates();
        // if no pelletes is left, return the cost zero
        if (current_remaining_pellet.isEmpty()) {
            return 0f;
        }

        // Default: pacmann location and a linklist of current remaining pellet coordinate
        Coordinate pacmann_location = src.getPacmanCoordinate();
        
        float min_dist_pellet = Float.MAX_VALUE ; 
        // use a for loop to calculate each possibility of the future movement cost, and choose the cost that is 
        // the minimum and return the minimum distance
        for (Coordinate pel: current_remaining_pellet){
            float dx = Math.abs(pacmann_location.x() - pel.x());
            float dy = Math.abs(pacmann_location.y() - pel.y());
            float distance = dx + dy;

            // store the distance that is smaller
            if (distance < min_dist_pellet){
                min_dist_pellet = distance;
            }
        }

        // return the min_dis as float type
        return min_dist_pellet;
    }


    @Override
    public Path<PelletVertex> graphSearch(final GameView game)
    {
        final PelletVertex start = new PelletVertex(game);
        final int counter = start.getRemainingPelletCoordinates().size();
        final int Activate_Full_Plan = 12; // activate full plan when less then 12
        final int Activate_Half_Plan = 6;  // activate half plan for 6 pellets and not touch the sensor of full plan
        final int Pellet_Remain;

        if (counter <= Activate_Full_Plan) {
            // if ther is less then 12 pellet left, make A star umlimited
            Pellet_Remain = 0;
        }else{
            // otherwise, only allowed A start with limitation
            Pellet_Remain = counter - Activate_Half_Plan;
        }
        PriorityQueue<Path<PelletVertex>> openSet = new PriorityQueue<>((p1,p2) -> Float.compare(p1.getTrueCost() + p1.getEstimatedPathCostToGoal(), p2.getTrueCost() + p2.getEstimatedPathCostToGoal()));
        Map<PelletVertex, Float> gScore = new HashMap<>();
        Path<PelletVertex> beginning_path = new Path<>(start);

        beginning_path.setEstimatedPathCostToGoal(getHeuristic(start, game, null));
        openSet.add(beginning_path);
        gScore.put(start, 0f);

        while (!openSet.isEmpty())
        {
            Path<PelletVertex> currentPath = openSet.poll();
            PelletVertex currenVertex = currentPath.getDestination();

            float best_g = gScore.getOrDefault(currenVertex, Float.POSITIVE_INFINITY);
            if (currentPath.getTrueCost() > best_g) {
                continue;
            }

            // we stop if we have enough pellet , and also if empty pellet also return current path, this conditon check both
            if (currenVertex.getRemainingPelletCoordinates().size() <= Pellet_Remain) {
                return currentPath;
            }

            for (PelletVertex neighbor : getOutgoingNeighbors(currenVertex, game, null))
            {
                float lower_bound_weight = getEdgeWeight(currenVertex, neighbor, null);
                float newG = best_g + lower_bound_weight;

                if (newG < gScore.getOrDefault(neighbor, Float.POSITIVE_INFINITY))
                {
                    gScore.put(neighbor, newG);
                    Path<PelletVertex> next_path = new Path<>(neighbor, lower_bound_weight, currentPath);
                    next_path.setEstimatedPathCostToGoal(getHeuristic(neighbor, game, null));
                    openSet.add(next_path);
                }
            }
        }

        return null;
    }

}

