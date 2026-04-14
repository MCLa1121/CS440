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
         int myAgentId = this.getAgentId();

        int myTerritories = 0;
        int enemyTerritories = 0;

        int myArmies = 0;
        int enemyArmies = 0;

        Set<Integer> livingOpponents = new HashSet<Integer>();

        //scan the board and count the current data(army, terrioies...)
        for(TerritoryOwnerView ownerView : state.getTerritoryOwners()){
            if(ownerView.isUnclaimed()){
                continue;
            }

            int ownerId = ownerView.getOwner();
            int armies = ownerView.getArmies();

            if(ownerId == myAgentId){
                myTerritories++;
                myArmies += armies;
            }else{
                enemyTerritories++;
                enemyArmies += armies;
                livingOpponents.add(ownerId);
            }
        }
        int totalTerritories = Math.max(1, myTerritories + enemyTerritories);
        int totalArmies = Math.max(1, myArmies + enemyArmies);

        // same normalized measurements as before
        double territoryRatio = myTerritories / (double)totalTerritories;
        double armyRatio = myArmies / (double)totalArmies;
        double myBonus = state.getBonusArmiesFor(myAgentId);
        double bonusScore = Math.max(0.0, Math.min(1.0, myBonus / 10.0));
        double opponentScore = 1.0 / (1.0 + livingOpponents.size());

        // Difference compare to Action reward
        // for placement reward, I care more about army strength / bonus generation
        // because placement mainly helps us stabilize and grow pressure for future turns
        double score = 0.25 * territoryRatio + 0.40 * armyRatio + 0.25 * bonusScore + 0.10 * opponentScore;

        return 200.0 * (score - 0.5);
     } // this sucks you'll need to change this

    /** {@inheritDoc} */
    public double getHalfTransitionReward(final GameView state,
                                          final Territory action) { return 0.0; }

    /** {@inheritDoc} */
    public double getFullTransitionReward(final GameView state,
                                          final Territory action,
                                          final GameView nextState) { return 0.0; }

}

