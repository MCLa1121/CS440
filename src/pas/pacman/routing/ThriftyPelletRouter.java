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
        // get remaining pellets in the game and store all the information to collection<coordinate>
        Collection<Coordinate> current_remaining_pellet = src.getRemainingPelletCoordinates();

        //create a arraylist for pellet that remain Note: Data type: Arraylist<Coordinate>; using Arraylist to store the neighbour so we are able to use the comparator (pellvertex do not have it)
        ArrayList<Coordinate> pellet_arr_list = new ArrayList<>(current_remaining_pellet); 

        // create pacman location to store the pacman location (src)
        Coordinate pacman_location = src.getPacmanCoordinate();

        // we sort the pellet array list so we can be sure to choose the closest pellet in our current list (and we sort by distance)
        pellet_arr_list.sort(Comparator.comparingInt(dis_in_list -> Math.abs(pacman_location.x() - dis_in_list.x()) + Math.abs(pacman_location.y() - dis_in_list.y())));
        
        // we set a bound to activate the prune; which i set to 12(mean we need at least 12 pellet to activate purning); test aroud 9 to 16 (12 to 10 will be a great range to choose)
        final int prune_requirment = 12;

        // create final int type set_limmt : is the number of closest 4 pellet (in other word purning the first cloest four before we go any further) NOTE: witout set a limit, OOM will trap us
        final int set_limit = 4; 

        // store the size of pellet array list to limit size 
        int limit_size = pellet_arr_list.size();

        // if the size of pellet array list is larger the prune requrement(12) Note: we want to prun when its bigger than 12; so we can avoid OOM happen; 
        if (pellet_arr_list.size() > prune_requirment){

            // if the size of the pellet array list is larger than the set limit our limit size is 4(set limit)
            // if the size of the pellet array list is less than the set limit our limit size is the size of the pellet array 
            limit_size = Math.min(set_limit, pellet_arr_list.size()); 
        }

        // Here we use the limit size to create the ArrayList<PelletVertex> neighbour NOTE: it give a fixibility that we can choose limit size to 4 or worst case as the size of pellet array (when too big we can puring so its still greate for us)
        ArrayList<PelletVertex> neighbour = new ArrayList<>(limit_size);

        // get all the neighbour using for loop to iterate over current closet pellet
        for (int i = 0; i < limit_size; i++) {

            // the removePellet returnh a new pellet vertex and remove the pellet we get from the set and we added to the neighbor
            neighbour.add(src.removePellet(pellet_arr_list.get(i)));
        }

        // return neighbor; NOTE: the neighbor list in other word is the next state after we eat the pellet that we choosen to process(with prunning 4 closet pellet)
        return neighbour;
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
        // get src and dst pacman locatoin
        Coordinate src_pac = src.getPacmanCoordinate();
        Coordinate dst_pac = dst.getPacmanCoordinate();

        // get the x,y coordinate from src and dst
        float src_x = src_pac.x();
        float src_y = src_pac.y();
        float dst_x = dst_pac.x();
        float dst_y = dst_pac.y();

        // use manhattan distance to calcualte the edgs weight
        float weight = Math.abs(src_x-dst_x) + Math.abs(src_y - dst_y); 

        // return weight Note:BE SURE weight is NON NEGATIVE to prevent SO WE WILL NOT MESS UP THE ALGO (so we use abs)
        return weight;
    }

    @Override
    public float getHeuristic(final PelletVertex src,
                              final GameView game,
                              final ExtraParams params)
    {
        // TODO: implement me!
        // create collection coordtinate to store the coordiantes of remaing pellet current have (which mean have not eat the pellet yet)
        Collection<Coordinate> current_remaining_pellet = src.getRemainingPelletCoordinates();

        // if no pelletes is left, return the cost zero; because we reach our goal to eat all the pellet
        if (current_remaining_pellet.isEmpty()) {
            return 0f;
        }

        // store current pacmann location from src.getPacmanCoordinate
        Coordinate pacmann_location = src.getPacmanCoordinate();
        
        // we set the min dist pellet to folat max value (so if there is a smaller exist will be replaced eaily)
        float min_dist_pellet = Float.MAX_VALUE ; 

        // use a for loop to iterate each reaming pellet and we calculate the distance with pacman
        for (Coordinate pel: current_remaining_pellet){
            float dx = Math.abs(pacmann_location.x() - pel.x());
            float dy = Math.abs(pacmann_location.y() - pel.y());
            float distance = dx + dy;

            // store the distance that is smallest(the closest to pacman)
            if (distance < min_dist_pellet){

                // we store the smallest dist to minimum distance pellet (the closest pellet to pacman)
                min_dist_pellet = distance;
            }
        }

        // return the distance of the closest pellet (whcih we can avoid over value the heristic value)
        return min_dist_pellet;
    }


    @Override
    public Path<PelletVertex> graphSearch(final GameView game){
        // start: the current game start status of the pellet
        final PelletVertex start = new PelletVertex(game);
        // counter: the number of the pellets that reamin in the current game's status
        final int counter = start.getRemainingPelletCoordinates().size();

        // activate full plan when less then 12
        final int Activate_Full_Plan = 12; 
        final int Activate_Half_Plan = 6;  // activate half plan for 6 pellets and not touch the sensor of full plan
        final int Pellet_Remain;  // set the pellet reamin: mean eat until the pellet reamin some number

        // create visted to store the value thae we alreary process; max x and my is the borader dimention 
        Map<Integer, int[]> visited = new HashMap<>();
        int max_X = game.getXBoardDimension();
        int max_Y = game.getYBoardDimension();

        // if not limit the expansion of eating all the pellet, oom rise(because there are too many pellet to eat);
        if (counter <= Activate_Full_Plan) {
            // if ther is less then 12 pellet left, make A star umlimited to plan to eat all of the pellet
            // eat until the pellet remain 0
            Pellet_Remain = 0;
        }else{
            // otherwise, only allowed A start with limitation to plan to eat only 6 pellets
            Pellet_Remain = counter - Activate_Half_Plan;
        }

        // create an priority queue name open set; and we compare f (g + h) and we chhose the smallest one 
        // create an map named gscore to store the best value of g we have encounter 
        // createa a path name begining path to store the path that we are using with start 
        PriorityQueue<Path<PelletVertex>> openSet = new PriorityQueue<>((p1,p2) -> Float.compare(p1.getTrueCost() + p1.getEstimatedPathCostToGoal(), p2.getTrueCost() + p2.getEstimatedPathCostToGoal()));
        Map<PelletVertex, Float> gScore = new HashMap<>();
        Path<PelletVertex> beginning_path = new Path<>(start);

        //set the beginning estimate path cost to goal based on our heuristic funtion
        beginning_path.setEstimatedPathCostToGoal(getHeuristic(start, game, null));

        // add the beginning path to PQ
        openSet.add(beginning_path);

        // update the start gscore to zero (at gscore[start])
        gScore.put(start, 0f);

        // while the priority queue is not empty
        while (!openSet.isEmpty()) {    
            // pop the path that with them smallest f(g+_h) value 
            Path<PelletVertex> currentPath = openSet.poll();

            // set currevertex as the current path' s destinatoin
            PelletVertex currenVertex = currentPath.getDestination();

            // create best_g to store the best g we have 
            float best_g = gScore.getOrDefault(currenVertex, Float.POSITIVE_INFINITY);
            // if the currentpath has a higer g value then we shoudl skip it; the bigger the better
            if (currentPath.getTrueCost() > best_g) {
                continue;
            }

            // we stop if we have enough pellet , and also if empty pellet also return current path, this conditon check both (meaning we reach the goal)
            if (currenVertex.getRemainingPelletCoordinates().size() <= Pellet_Remain) {
                return currentPath;
            }

            // store the current pellet's pacman loccatin in start_c
            Coordinate start_c = currenVertex.getPacmanCoordinate();

            // y times x help us  went to the start of row y; + x will let us move to col x at that row
            int key_value = start_c.y() * max_X + start_c.x();

            // dist is going to store the shorstest distancd to that grid (if we calcuate before, we can use the value directly)
            int[] dist = visited.get(key_value);

            // if dist is empty we get in and find the s.p (a mini bfs implementation here)
            if (dist == null) {

                // here we need to init the dist before we calculate, and the size of the int array is the size of the game borad
                dist = new int[max_X * max_Y];

                // use a for loop to init all the dist that is not visit to -1
                for (int i = 0; i < dist.length; i++) { 
                    dist[i] = -1;
                }

                //  create a link list to get the shortest path (queue)
                LinkedList<Coordinate> queue = new LinkedList<>();

                // start_c.y() * max_X + start_c.x(); that the key value same explaintain at line 237
                dist[start_c.y() * max_X + start_c.x()] = 0;
                // push the staring coordinate to the queue
                queue.add(start_c);

                // if the queue is not empty
                while (!queue.isEmpty()) {

                    // pop the first coordinate in queue and store in to current coordinate
                    Coordinate current_coordinate = queue.removeFirst();

                    // calcuate the current coordinate id value with its own x, y ; same explaination at line 237
                    int current_id = current_coordinate.y() * max_X + current_coordinate.x();
                    // store the dist[current_id]'s s.p in current dist
                    int current_Dist = dist[current_id];

                    // create a for loop itereate all the action that can make (call Action.values())
                    for (Action Direction : Action.values()) {
                        // if the direction lead us to a wall, we skip, and try the next direcction
                        if (!game.isLegalPacmanMove(current_coordinate, Direction)){ 
                            continue;
                        }

                        // take direction and return a coordinate that store in the next_coordinate (call apply in Action.clss)
                        Coordinate next_coordinate = Direction.apply(current_coordinate);
                        
                        // if this direction result the same as before skip the rest
                        if (next_coordinate.equals(current_coordinate)) {
                            continue;
                        }

                        // calculate the key value for next coordinate so it can be added to dist properly;
                        int next_cord_key = next_coordinate.y() * max_X + next_coordinate.x();
                        
                        // if this key value indicate that we have visit it; skip the rest
                        if (dist[next_cord_key] != -1) {
                            continue;
                        }

                        // if not visit add one to current dist (not like dikjstra; bfs method is unweight; here we assume it 1 as edgewiegh[more abstratly we see it as take one move])
                        dist[next_cord_key ] = current_Dist + 1;

                        // add the next coordinate to the queue
                        queue.add(next_coordinate);
                    }
                }
                // save the dist to visited; so it can avoid recalculating the value;
                visited.put(key_value, dist);
            }
            
            // use a for loop to iterate each neighbor from currentVertex outgoingNeighbors
            for (PelletVertex neighbor : getOutgoingNeighbors(currenVertex, game, null)) {
                
                Coordinate goal_c = neighbor.getPacmanCoordinate();

                // use the same key formular to obtain the dist value in dist[] , the value is the edge cost from the current to the neighbor
                int dist_to_neighbor = dist[goal_c.y() * max_X + goal_c.x()];
                
                // if the value is one, mean we have not visit it yet, because we have not calulate yet
                if (dist_to_neighbor == -1) {
                    continue;
                }

                // if has a value; we store the value to lower_bound_weight (cast to float because the calcuation we did is based on integer type)
                float best_neighbor = (float) dist_to_neighbor;

                // newG: is the sum of the cost to each neighbor on this path (best_g + best neighbor)
                float newG = best_g + best_neighbor;
                
                // if newG is smaller mean the path is good to go; (if the neighbor no t been visit the defalut will choose +infinity here)
                if (newG < gScore.getOrDefault(neighbor, Float.POSITIVE_INFINITY)) {

                    // store the best g(cost) to this neighbor to gscore
                    gScore.put(neighbor, newG);

                    // create a next path that from the current path (we eat the pellet)
                    Path<PelletVertex> next_path = new Path<>(neighbor, best_neighbor, currentPath);

                    // set the next path estimatepathcost to goal based on our heuristic vlaue 
                    next_path.setEstimatedPathCostToGoal(getHeuristic(neighbor, game, null));
                    
                    // add the next path to the pq for later extract 
                    openSet.add(next_path);
                }
            }
        }
    
        return null;
    }
}

// javac -cp "./lib/*;." @pacman.srcs   
// java -cp "./lib/*;." edu.bu.pas.pacman.Main -a src.pas.pacman.agents.PacmanAgent -x 9 -y 9 -g 0
// javac -cp "./lib/*;." @pacman.srcs   
// java -cp "./lib/*;." edu.bu.pas.pacman.Main -a src.pas.pacman.agents.PacmanAgent -x 9 -y 9 -g 0
// javac -cp "./lib/*;." @pacman.srcs   
// java -cp "./lib/*;." edu.bu.pas.pacman.Main -a src.pas.pacman.agents.PacmanAgent -x 9 -y 9 -g 0