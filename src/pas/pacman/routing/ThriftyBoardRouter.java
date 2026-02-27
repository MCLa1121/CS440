package src.pas.pacman.routing;


import java.net.CookieHandler;
import java.util.ArrayList;
// SYSTEM IMPORTS
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.HashMap;

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
        Collection<Coordinate> neighbour = new ArrayList<Coordinate>(); 
        for (Action action : Action.values()){
            if (game.isLegalPacmanMove(src, action)){
                Coordinate next = action.apply(src);
                neighbour.add(next);
            }
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
        //get the current estimate cost
        start.setEstimatedPathCostToGoal(Cost(src, tgt));
        //record the best heuristic cost using a hashmap 
        HashMap<Coordinate, Float> best = new HashMap<>();
        best.put(src, 0.0f); 
        while(!checked.isEmpty()){
            Path<Coordinate> current = checked.poll();
            //general idea
            //get the current coordinate 
            //find all the cost of the neighbour 
            //by adding all cost with the heuristic function, find the smallest
            //update current node to be the parent of the smallest node
            //add current node to the came_from list, meaning it is fixed
            Coordinate cur = current.getDestination();
            //Define a current best cost so far so we can keep track of it  
            Float currentBest = best.get(cur);

            //Now check whether the current best is the best or not
            //For the next pop sprcificly (each iteration)
            //meaning if this path is worse than the best path we already found to this node, ignore it
            // if(currentBest != null && current.getTrueCost() > currentBest){
            //     continue; // meaning this is not the best cost path, we skip it 
            // }

            // //check whether the current coordinate is the target
            if(cur.equals(tgt)){
                return current;
            }
            //get the true cost of the current path
            float c = current.getTrueCost();
            

            //get and record  all the neighbour of the currnet coordinate 
            for(Coordinate nbr : getOutgoingNeighbors(cur, game, null)){
                //since each move cost one, each move we add one to the current cost 
                float Cost = c + 1;
                //getting the current best cost, which is parent cost 
                //if we have not seem it before, it should equal to null, vise versa 
                Float oldCost = best.get(nbr);
                //by comparing the two cost, first check whether we have seen the old cost before 
                if(oldCost == null || Cost < oldCost){
                    //if the current cost is less than the oldCost, we update the best map
                    best.put(nbr, Cost);

                    //set the next path, src are the nbr, 1 cost and parent parh os the current path 
                    Path<Coordinate> next = new Path<Coordinate>(nbr, Cost, current);
                    //set the cost of the neighbour to the goal
                    next.setEstimatedPathCostToGoal(Cost(nbr,tgt));
                    //add next to the check list
                    checked.add(next);
                }

            }
            
        }
        return null;
    }

}

