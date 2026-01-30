package src.labs.lab1.agents;


import edu.bu.labs.lab1.Coordinate;
// SYSTEM IMPORTS
import edu.bu.labs.lab1.Direction;
import edu.bu.labs.lab1.State.StateView;
import edu.bu.labs.lab1.agents.Agent;
import edu.bu.labs.lab1.Tile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


// JAVA PROJECT IMPORTS


public class ClosestUnitAgent
    extends Agent
{

    // put your fields here! You will probably want to remember the following information:
    //      - all friendly unit ids (there may be more than one!)
    //      - the location(s) of COIN(s) on the map
    private Integer    AllUnitId;            // id of the unit we control (used to lookop UnitView from state)
    private Coordinate coinLocation;        // Coordinate of the COIN (only one) on the map
    Set<Integer> AllUnitIds = new HashSet<>(); // set of all unit ids
    Integer Bestid; // id of the closest unit
    private Coordinate finishLocation; // location of the finish tile


    /**
     * The constructor for this type. Each agent has a unique ID that you will need to use to request info from the
     * state about units it controls, etc.
     */
	public ClosestUnitAgent(final int agentId)
	{
		super(agentId); // make sure to call parent type (Agent)'s constructor!

        // initialize your fields here!
        this.AllUnitId = null;
        this.coinLocation = null;

        // helpful printout just to help debug
		System.out.println("Constructed ClosestUnitAgent");
	}

    /////////////////////////////// GETTERS AND SETTERS (this is Java after all) ///////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public final Integer getAllUnitId() { 
        return this.AllUnitId; 
    }

    public final Coordinate getCoinLocation() {
        return this.coinLocation;
    }
    
    public final Coordinate getFinishLocation() {
        return this.finishLocation;
    }

    public void setAllUnitId(Integer i) { 
        this.AllUnitId = i; 
    }
    public void setCoinLocation(Coordinate c) {
        this.coinLocation = c;
    }
    public void setFinishLocation(Coordinate c) {
        this.finishLocation = c;
    }
    /**
     * This method is called by our game engine once: before any moves are made. You are provided with the state of
     * the game before any actions have been taken. This is in case you have some fields you need to set but are
     * unable to in the constructor of this class (like keeping track of units on the map, etc.).
     */
	@Override
	public void initializeFromState(final StateView stateView)
	{
        // TODO: identify units, set fields that couldn't be initialized in the constructor because
        // of a lack of game data in the constructor.
        for(Integer unitID : stateView.getUnitIds(this.getAgentId())) // find all units on my team
        {
            AllUnitIds.add(unitID);
        }
	
    }

    // compute the distance between two coordinates
    public double distance(Coordinate a, Coordinate b) {
        int row = a.row() - b.row(); // change in row as the formula suggests (distance changes)
        int col = a.col() - b.col(); // change in column 
        return Math.sqrt(row * row + col * col);
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

        // TODO: your code to give your unit(s) actions for this turn goes here!
        // to find the closest unit, we need to first where is the coin 
        for(int i = 0; i < state.getNumRows(); i++) {
            for(int j = 0; j < state.getNumCols(); j++) {
                Coordinate here = new Coordinate(i, j);
                if(state.getTileState(here) == Tile.State.FINISH) { // Tile imported 
                    finishLocation = here; // we found the finish location
                }
            }
        }
        double best = Double.MAX_VALUE; // initialize best distance to max value, so once we find a unit, we replace it
        // now we need to find the closest unit to the coin
        // Get current position of our unit from the StateView
        for (Integer id : this.AllUnitIds) {
            Coordinate pos = state.getUnitView(this.getAgentId(), id).currentPosition();
            double d = distance(pos, coinLocation);
            if (d < best){
                best =d;
                Bestid = id;
            }
            
        Direction move = null;
        //we want to move the closest unit to the coin
        Coordinate bestPos = state.getUnitView(this.getAgentId(), Bestid).currentPosition();

        // Match column first 
        if (bestPos.col() < coinLocation.col()) 
            move = Direction.RIGHT;
        else if (bestPos.col() > coinLocation.col()) 
            move = Direction.LEFT;
        // If column matches, then match row 
        else if (bestPos.row() < coinLocation.row()) 
            move = Direction.DOWN;
        else if (bestPos.row() > coinLocation.row()) 
            move = Direction.UP;
       
        actions.put(Bestid, move);
        return actions;
	}

}

