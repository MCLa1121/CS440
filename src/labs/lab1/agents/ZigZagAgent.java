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


public class ZigZagAgent
    extends Agent
{

    // put your fields here! You will probably want to remember the following information:
    //      - all friendly unit ids (there may be more than one!)
    //      - the location(s) of COIN(s) on the map
    private Integer myUnitId;            // id of the unit we control
    private Integer friendId;          // id of the other friendly unit
    private Coordinate coinLocation;      // Coordinate of the COIN 
    boolean moveUP = false; // checking whether we moved UP last time or not


    /**
     * The constructor for this type. Each agent has a unique ID that you will need to use to request info from the
     * state about units it controls, etc.
     */
	public ZigZagAgent(final int agentId)
	{
		super(agentId); // make sure to call parent type (Agent)'s constructor!

        // initialize your fields here!
        this.myUnitId = null;


        // helpful printout just to help debug
		System.out.println("Constructed ZigZagAgent");
	}

    /////////////////////////////// GETTERS AND SETTERS (this is Java after all) ///////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public final Integer getMyUnitId() { 
        return this.myUnitId; 
    }

    public final Integer getFriendId() {
        return this.friendId;
    }

    public final Coordinate getCoinLocation() {
        return this.coinLocation;
    }

    public void setMyUnitId(Integer i) { 
        this.myUnitId = i; 
    }
    public void setFriendId(Integer i) {
        this.friendId = i;
    }
    public void setCoinLocation(Coordinate c) {
        this.coinLocation = c;
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
        
        // discover my unitId
        // discover friendly units (same as the scripted agent)
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
        this.setMyUnitId(myUnitIds.iterator().next());
	}

    /**
     * This method is called every turn (or "frame") of the game. Your agent is responsible for assigning
     * actions to each of the unit(s) your agent controls. The return type of this method is a mapping
     * from unit ID (that your agent controls) to the Direction you want that unit to move in.
     *
     * If you are trying to collect COIN(s), you do so by walking into the same square as a COIN. Your agent
     * will pick it up automatically (and the COIN will disappear from the map).
     */
	@Override
	public Map<Integer, Direction> assignActions(final StateView state)
    {
        Map<Integer, Direction> actions = new HashMap<>();

        // TODO: your code to give your unit(s) actions for this turn goes here!

        Direction move = null;
        //get the current locatoin 
        Coordinate myPos = state.getUnitView(this.getAgentId(), this.getMyUnitId()).currentPosition();

        //we are going RIGHT first 
        //since moving right is alsways the fist step 
        //so we need to check whether we are at out origin tile or not

        //Problem (cont.d:) the agent is contunously moving right even after reaching the rightmost edge
        if (state.getTileState(myPos) == Tile.State.START) {
            move = Direction.RIGHT;
            moveUP = true; // we just moved right, so next time we move up
        }else{// once the first right move is done, we start zigzagging
            //if last time was up, we now move right and set moveUP to false
            if (moveUP) {
                move = Direction.UP;
            }else {
                move = Direction.RIGHT;
            }
                moveUP = !moveUP; // toggle for next turn
        }
        // assign the move 
        actions.put(this.getMyUnitId(), move);
        return actions;
	}

}

