package src.pas.pacman.agents;


// SYSTEM IMPORTS
import edu.bu.pas.pacman.agents.Agent;
import edu.bu.pas.pacman.agents.SearchAgent;
import edu.bu.pas.pacman.game.Action;
import edu.bu.pas.pacman.game.Game.GameView;
import edu.bu.pas.pacman.graph.Path;
import edu.bu.pas.pacman.graph.PelletGraph.PelletVertex;
import edu.bu.pas.pacman.routing.BoardRouter;
import edu.bu.pas.pacman.routing.PelletRouter;
import edu.bu.pas.pacman.utils.Coordinate;
import edu.bu.pas.pacman.utils.Pair;

import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

// JAVA PROJECT IMPORTS
import src.pas.pacman.routing.ThriftyBoardRouter;  // responsible for how to get somewhere
import src.pas.pacman.routing.ThriftyPelletRouter; // responsible for pellet order


public class PacmanAgent
    extends SearchAgent
{

    private final Random random;
    private BoardRouter  boardRouter;
    private PelletRouter pelletRouter;

    public PacmanAgent(int myUnitId,
                       int pacmanId,
                       int ghostChaseRadius)
    {
        super(myUnitId, pacmanId, ghostChaseRadius);
        this.random = new Random();

        this.boardRouter = new ThriftyBoardRouter(myUnitId, pacmanId, ghostChaseRadius);
        this.pelletRouter = new ThriftyPelletRouter(myUnitId, pacmanId, ghostChaseRadius);
    }

    public final Random getRandom() { return this.random; }
    public final BoardRouter getBoardRouter() { return this.boardRouter; }
    public final PelletRouter getPelletRouter() { return this.pelletRouter; }

    @Override
    public void makePlan(final GameView game)
    {
        // TODO: implement me! This method is responsible for calculating
        // the "plan" of Coordinates you should visit in order to get from a starting
        // location and another ending location. I recommend you use
        // this.getBoardRouter().graphSearch(...) to get a path and convert it into
        // a Stack of Coordinates (see the documentation for SearchAgent)
        // which your makeMove can do something with!
        
        //get the current location 
        Coordinate current = (game.getEntity(game.getPacmanId())).getCurrentCoordinate();
        
        // get the path of the pallet 
        Path<PelletVertex> pellet = this.getPelletRouter().graphSearch(game);

        //Check whether there is a path to the pellet
        //if we dont have a path, set the targrt and the plan to null 
        //method extend from the file (SearchAgent)
        if(pellet == null ){
            this.setPlanToGetToTarget(null);
            this.setTargetCoordinate(null);
            return; 
        }
        //Convert pelletPath (backwards) into a forward list of PelletVertex states
        Stack<PelletVertex> pelletPath = new Stack<>();
        Path<PelletVertex> p = pellet;
        while (p != null){
            //we push the destination to the stack since it is the "start" of the path
            pelletPath.push(p.getDestination());
            //update p to its parent, which is the neighbour of the destination
            p = p.getParentPath();
        }
        // If then there are no pellets left, we finish the currnet plan
        if (pelletPath.size() <= 1){
            this.setPlanToGetToTarget(null);
            this.setTargetCoordinate(null);
            return;
        }
        //get the starting pellet state 
        //PelletVertex startState = pelletPath.pop();

        //continue pop to get the next state 
        PelletVertex nextState = pelletPath.pop();

        //the next pellet coordiante, which is the coordiante of the pacman after we ate the first pellet
        Coordinate nextPelletCoord = nextState.getPacmanCoordinate();
        this.setTargetCoordinate(nextPelletCoord);

        //now we need to find the path to the next coordinate from the board 
        Path<Coordinate> boardPath = this.getBoardRouter().graphSearch(current, nextPelletCoord, game);
       // if there is not path for the next coordiante, we reach to an end 
        if (boardPath == null) {
            this.setPlanToGetToTarget(null);
            return;
        }

        // Convert the path to a Stack<Coordinate> that pops next move
        //same logic as the pelletpath
        Stack<Coordinate> plan = new Stack<>();
        Path<Coordinate> q = boardPath;
        while (q != null) {
            plan.push(q.getDestination());
            q = q.getParentPath();
        }

        if (!plan.isEmpty()) {
            plan.pop();
        }

        this.setPlanToGetToTarget(plan);
    }

    @Override
    public Action makeMove(final GameView game)
    {
        // This is currently configured to choose a random action
        // TODO: change me!
        
        // current pacman locatio
        Coordinate curPos = game.getEntity(game.getPacmanId()).getCurrentCoordinate();

        // If no plan or plan finished, compute a new plan
        if (this.getPlanToGetToTarget() == null || this.getPlanToGetToTarget().isEmpty()){
            this.makePlan(game);
        }

        Stack<Coordinate> plan = this.getPlanToGetToTarget();

        // if plan remain null, then we do nothing 
        if (plan == null || plan.isEmpty()){
            return null;
        }

        // if top of plan equals current position, remove it
        while (!plan.isEmpty() && curPos.equals(plan.peek())){
            plan.pop();
        }

        if (plan.isEmpty()){
            return null;
        }

        // Next coordinate to move into
        Coordinate next = plan.pop();

        try{
            return Action.inferFromCoordinates(curPos, next);
        }
        catch (Exception e){
            // Plan became invalid
            this.setPlanToGetToTarget(null);
            return null;
        }
            //return Action.values()[this.getRandom().nextInt(Action.values().length)];
        }

    @Override
    public void afterGameEnds(final GameView game)
    {
        // if you want to log stuff after a game ends implement me!
    }
}
