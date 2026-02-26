package src.pas.pacman.routing;
import src.pas.pacman.routing.ThriftyBoardRouter;

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



        // get remaining pellets in the game and store all the information to collection
        Collection<Coordinate> current_remaining_pellet = src.getRemainingPelletCoordinates();
        //create a arraylist Note: Data type: Arraylist<PelletVertex>; using Arraylist to store the neighbour
        ArrayList<PelletVertex> neighbour = new ArrayList<>(current_remaining_pellet.size()); 
        Coordinate pacman_location = src.getPacmanCoordinate();

        // if the current remaing pellet is empty is empty then return null
        if (current_remaining_pellet.isEmpty()) {
            return neighbour;
        }

        // createa an arraylist that use pair to commputing the didance
        ArrayList<Pair<Float,Coordinate>> distance_List = new ArrayList<>();


        //get all the neighbour coordinate using for loop to iterate over current remaining pellet
        // it help to save all the possible case: what if we eat this pellet. eat this(different pellet); remove this; move there; save to neighbor
        for (Coordinate pel : current_remaining_pellet) {
            float distance = Math.abs(pacman_location.x() - pel.x()) + Math.abs(pacman_location.y() - pel.y());
            distance_List.add(new Pair<>(distance, pel));
            }
        // After we store all the possible case we can eat the pellet, we return it
        
        distance_List.sort((a,b) -> Float.compare(a.getFirst(), b.getFirst()));

        final int num_of_closest_pellet = 4;

        int limitation= Math.min(num_of_closest_pellet, distance_List.size()); // prune here limit the muber to find pellet 

        for (int i = 0; i < limitation; i++) {
            Coordinate pel = distance_List.get(i).getSecond();
            neighbour.add(src.removePellet(pel));
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
        
        // if no pelletes is left, return the cost zero
        if (src.getRemainingPelletCoordinates().isEmpty()) {
            return 0f;
        }

        
        // Default: pacmann location and a linklist of current remaining pellet coordinate
        Coordinate pacmann_location = src.getPacmanCoordinate();
        Collection<Coordinate> current_remaining_pellet = src.getRemainingPelletCoordinates();

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
        PelletVertex start = new PelletVertex(game);
        PriorityQueue<Path<PelletVertex>> openSet = new PriorityQueue<>((p1,p2) -> Float.compare(p1.getTrueCost() + p1.getEstimatedPathCostToGoal(), p2.getTrueCost() + p2.getEstimatedPathCostToGoal()));
        Map<PelletVertex, Double> gScore = new HashMap<>();
        Path<PelletVertex> beginning_path = new Path<>(start);
        ThriftyBoardRouter board_Router = new ThriftyBoardRouter(this.getMyUnidId(), this.getPacmanId(), this.getGhostChaseRadius());

        beginning_path.setEstimatedPathCostToGoal(getHeuristic(start, game, null));
        openSet.add(beginning_path);
        gScore.put(start, 0.0);

        while (!openSet.isEmpty())
        {
            Path<PelletVertex> currentPath = openSet.poll();
            PelletVertex currenVertex = currentPath.getDestination();

            double best_g = gScore.getOrDefault(currenVertex, Double.POSITIVE_INFINITY);
            if (currentPath.getTrueCost() > best_g) {
                continue;
            }

            if (currenVertex.getRemainingPelletCoordinates().isEmpty()) {
                return currentPath;
            }

            for (PelletVertex neighbor : getOutgoingNeighbors(currenVertex, game, null))
            {
                Path<Coordinate> board_Path =board_Router.graphSearch(currenVertex.getPacmanCoordinate(), neighbor.getPacmanCoordinate(), game);

                if (board_Path == null) { 
                    continue;
                }

                float true_edge_cost = board_Path.getTrueCost();
                double newG = gScore.get(currenVertex) + true_edge_cost;

                if (newG < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY))
                {
                    gScore.put(neighbor, newG);
                    Path<PelletVertex> next_path = new Path<>(neighbor, true_edge_cost, currentPath);
                    next_path.setEstimatedPathCostToGoal(getHeuristic(neighbor, game, null));
                    openSet.add(next_path);
                }
            }
        }

        return null;
    }

}

