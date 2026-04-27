package pas.risk.rewards;


import java.util.HashSet;
import java.util.Set;

// SYSTEM IMPORTS
import edu.bu.jmat.Pair;

import edu.bu.pas.risk.GameView;
import edu.bu.pas.risk.TerritoryOwnerView;
import edu.bu.pas.risk.agent.rewards.RewardFunction;
import edu.bu.pas.risk.agent.rewards.RewardType;
import edu.bu.pas.risk.territory.Territory;


// JAVA PROJECT IMPORTS


/**
 * <p>Represents a function which punishes/pleasures your model according to how well the {@link Territory}s its been
 * choosing to place armies have been. Your reward function could calculate R(s), R(s,t), or (R,t,a'): whichever
 * is easiest for you to think about (for instance does it make more sense to you to evaluate behavior when you see a
 * state, the action you took in that state, and how that action resolved? If so you want to pick R(s,t,s')).
 *
 * <p>By default this is configured to calculate R(s). If you want to change this you need to change the
 * {@link RewardType} enum in the constructor *and* you need to implement the corresponding method. Refer to
 * {@link RewardFunction} and {@link RewardType} for more details.
 */
public class MyPlacementRewardFunction
    extends RewardFunction<Territory>
{

    public MyPlacementRewardFunction(final int agentId)
    {
        super(RewardType.STATE, agentId); // change this enum if you don't want to do R(s)
    }

    //changed the lowered bound since we could probably get a negative number
    public double getLowerBound() { return -100.0; }
    public double getUpperBound() { return 100.0; }

    /** {@inheritDoc} */
    public double getStateReward(final GameView state) { 
        // general idea:
        // placement decisions should help us build a strong board
        // so this reward looks at how safe / strong our current position is overall
        //generally look the same as the action reward function
        
        //initialization
        // get my agent id
        int my_agentId = this.getAgentId();

        // default my territoreis and enemy territotyes as 0
        int my_territories = 0;
        int enemy_territories = 0;

        // default my armies and enemy armies as 0
        int my_armies = 0;
        int enemy_armies = 0;

        // a set of living opponents
        Set<Integer> living_opponents = new HashSet<Integer>();

        //scan the board and count the current data(army, terrioies...)
        // use for loop to iterate from the state 
        for(TerritoryOwnerView ownerView : state.getTerritoryOwners()){
            // is the terro is unclamed then continue
            if(ownerView.isUnclaimed()){
                continue;
            }

            // get the owner id
            int ownerId = ownerView.getOwner();
            // get the armeis nuimber of the current ownver view
            int armies = ownerView.getArmies();

            // if the agent ide is our agent
            if(ownerId == my_agentId){   
                // increment to our territory
                my_territories++;

                // add the ariems to our armies
                my_armies += armies;
            
            // otherwise if it is not our agent id
            }else{
                // increment the eneny territoreis
                enemy_territories++;

                // add the armeis to the enemies armies
                enemy_armies += armies;

                // add the ownver id to the set of living opponents
                living_opponents.add(ownerId);
            }
        }

        // Here is the winning bouns, if the agent win, then the agent will face a huge winning bonous
        if(living_opponents.isEmpty()){
            return 100.0;
        }

        // Here is the losing bouns, if the agent lose, then the agent will fase huge penaflty
        if(my_territories == 0) { 
            return -100.0;
        }

        // set the total territoereis between1 aand myterroo + enemy terr
        int totalTerritories = Math.max(1, my_territories + enemy_territories);
        // same, set the total armies between my arimes and enemyariems
        int totalArmies = Math.max(1, my_armies + enemy_armies);

        // same normalized measurements as before
        double territoryRatio = my_territories / (double)totalTerritories;
        double armyRatio = my_armies / (double)totalArmies;
        double myBonus = state.getBonusArmiesFor(my_agentId);
        double bonusScore = Math.max(0.0, Math.min(1.0, myBonus / 10.0));
        double opponentScore = 1.0 / (1.0 + living_opponents.size());

        // Difference compare to Action reward
        // for placement reward, I care more about army strength / bonus generation
        // because placement mainly helps us stabilize and grow pressure for future turns
        double score = 0.25 * territoryRatio + 0.40 * armyRatio + 0.25 * bonusScore + 0.10 * opponentScore;
        
        return score * 2.0;

     } // this sucks you'll need to change this

    /** {@inheritDoc} */
    public double getHalfTransitionReward(final GameView state,
                                          final Territory action) { return 0.0; }

    /** {@inheritDoc} */
    public double getFullTransitionReward(final GameView state,
                                          final Territory action,
                                          final GameView nextState) { return 0.0; }

}

