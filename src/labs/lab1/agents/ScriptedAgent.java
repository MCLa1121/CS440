package src.labs.lab1.agents;


// SYSTEM IMPORTS
import edu.bu.labs.lab1.Coordinate;
import edu.bu.labs.lab1.Direction;
import edu.bu.labs.lab1.State.StateView;
import edu.bu.labs.lab1.Tile; // find it at the stateview class 
                                //public final Tile.State getTileState(Coordinate c) {
                                //  return this.getMaze().getTile(c).state();
                                        //}
import edu.bu.labs.lab1.agents.Agent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


// JAVA PROJECT IMPORTS


public class ScriptedAgent
    extends Agent
{

	private Integer    myUnitId;            // id of the unit we control (used to lookop UnitView from state)
    private Coordinate coinLocation;        // Coordinate of the COIN (only one) on the map


    /**
     * The constructor for this type. Each agent has a unique ID that you will need to use to request info from the
     * state about units it controls, etc.
     */
	public ScriptedAgent(final int agentId)
	{
		super(agentId); // make sure to call parent type (Agent)'s constructor!

        // even though we have fields for my unit's ID and the COIN's location
        // we don't have access to that information in the constructor. So, we are forced to rely on the
        // initializeFromState method (where we have access to the state of the game) to set these fields.
        // You should think of the method initializeFromState as a "secondary constructor"
        this.myUnitId = null;
        this.coinLocation = null;

        // helpful printout just to help debug
		System.out.println("Constructed ScriptedAgent");
	}

    /////////////////////////////// GETTERS AND SETTERS (this is Java after all) ///////////////////////////////
	public final Integer getMyUnitId() { return this.myUnitId; }
    public final Coordinate getCoinLocation() { return this.coinLocation; }

    private void setMyUnitId(Integer i) { this.myUnitId = i; }
    private void setCoinLocation(Coordinate c) { this.coinLocation = c; }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Agents in Sepia have five abstract methods that we must override. The first three are the most important:
     *    - initialStep
     *    - middleStep
     *    - terminalStep
     * When a new game is started, the Agent objects are created using their constructors. However, the game itself
     * has not been started yet, so if an Agent wishes to keep track of units/resources in the game, that info is
     * not available to it yet. The first turn of the game, each agent's initialStep method is called by sepia
     * automatically and provided the *initial* state of the game. So, this method is often used as a
     * secondary constructor to "discover" the ids of units we control, the number of players in the game, etc.
     *
     * This method produces a mapping from the ids of units we control to the actions those units will take this turn.
     * Note we are only allowed to map units that we *control* (their ids technically) to actions, we are not allowed
     * to try to control units that aren't on our team!
     */
	@Override
	public void initializeFromState(final StateView stateView)
	{

		// discover friendly units
        Set<Integer> myUnitIds = new HashSet<>();
		for(Integer unitID : stateView.getUnitIds(this.getAgentId())) // for each unit on my team
        {
            myUnitIds.add(unitID);
        }

        // check that we only have a single unit
        if(myUnitIds.size() != 1)
        {
            System.err.println("[ERROR] ScriptedAgent.initialStep: I should control only 1 unit");
			System.exit(-1);
        }

        //TODO: discover the location of the coin. See the documentation!
        Coordinate coinLocation = null;
         // Loop through every row and column in the maz 
        for (int r = 0; r < stateView.getNumRows(); r++) {
            for (int c = 0; c < stateView.getNumCols(); c++) {
                // Create a coordinate for this tile
                Coordinate here = new Coordinate(r, c);

            // Check if this tile contains a COIN
            if (stateView.getTileState(here) == Tile.State.COIN) {
                coinLocation = here;
                break; // we find the coin !
            }
        }
        if (coinLocation != null){
            break;// we found the coin, exit the outer loop 
        }
    }
        this.setMyUnitId(myUnitIds.iterator().next());
        this.setCoinLocation(coinLocation);
	}

    /**
     * This method is called every turn (or "frame") of the game. Your agent is responsible for assigning
     * actions to each of the unit(s) your agent controls. The return type of this method is a mapping
     * from unit ID (that your agent controls) to the Direction you want that unit to move in.
     *
     * If you are trying to collect COIN(s), you do so by walking into the same square as a COIN. Your agent
     * will pick it up automatically (and the COIN will dissapear from the map).
     */
	@Override
	public Map<Integer, Direction> assignActions(final StateView state)
    {
        Map<Integer, Direction> actions = new HashMap<>();

        //Find the FINISH point by scanning the whole map
        Coordinate finish = null;
        // Loop through every row and column in the maze
        for (int r = 0; r < state.getNumRows(); r++) {
            // For each column in this row
            for (int c = 0; c < state.getNumCols(); c++) {
                //if we find the finish point 
                Coordinate here = new Coordinate(r, c);
                if (state.getTileState(here) == Tile.State.FINISH) { // see if this tile is the FINISH
                    finish = here;
                    break;
            }
        }
        if (finish != null){ 
            break;
        }
    }

    // Get current position of our unit from the StateView
    Coordinate myPos = state.getUnitView(this.getAgentId(), this.getMyUnitId()).currentPosition();

    // Declar e a variable to hold our move
    Direction move = null;

    // Move vertically first in rows
    if (myPos.row() < finish.row()) {
        move = Direction.DOWN; 
    } else if (myPos.row() > finish.row()) {
        move = Direction.UP;

    // If same row, move in the same column
    } else if (myPos.col() < finish.col()) {
        move = Direction.RIGHT;
    } else if (myPos.col() > finish.col()) {
        move = Direction.LEFT;
    }
    // assign the move 
    actions.put(this.getMyUnitId(), move);

    return actions;
    }


}

