package pas.risk.rewards;


// SYSTEM IMPORTS
import edu.bu.jmat.Pair;

import edu.bu.pas.risk.GameView;
import edu.bu.pas.risk.TerritoryOwnerView;
import edu.bu.pas.risk.action.Action;
import edu.bu.pas.risk.agent.rewards.RewardFunction;
import edu.bu.pas.risk.agent.rewards.RewardType;

import java.util.HashSet;
import java.util.Set;


// JAVA PROJECT IMPORTS


/**
 * <p>Represents a function which punishes/pleasures your model according to how well the {@link Action}s its been
 * choosing have been. Your reward function could calculate R(s), R(s,a), or (R,s,a'): whichever is easiest for you to
 * think about (for instance does it make more sense to you to evaluate behavior when you see a state, the action you
 * took in that state, and how that action resolved? If so you want to pick R(s,a,s')).
 *
 * <p>By default this is configured to calculate R(s). If you want to change this you need to change the
 * {@link RewardType} enum in the constructor *and* you need to implement the corresponding method. Refer to
 * {@link RewardFunction} and {@link RewardType} for more details.
 */
public class MyActionRewardFunction
    extends RewardFunction<Action>
{

    public MyActionRewardFunction(final int agentId)
    {
        super(RewardType.STATE, agentId); // change this enum if you don't want to do R(s)
    }

    public double getLowerBound() { return -110.0; }
    public double getUpperBound() { return 110.0; }

    /** {@inheritDoc} */
    public double getStateReward(final GameView state) { 
        // general idea:
        // evaluate how good this state is for my agent
        // reward states where:
        // 1. I own more territories
        // 2. I control more armies
        // 3. fewer opponents are still alive
        // 4. I receive more bonus armies

        //initialization 
        int myAgentId = this.getAgentId();

        int myTerritories = 0;
        int enemyTerritories = 0;

        int myArmies = 0;
        int enemyArmies = 0;

        //a set that keep track of the eneny player owning 
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

        //we dont want to have a division by zero in the ratio
        int totalTerritories = Math.max(1, myTerritories + enemyTerritories);
        int totalArmies = Math.max(1, myArmies + enemyArmies);

        // fraction of owned territories that belong to me
        double territoryRatio = myTerritories / (double)totalTerritories;

        // fraction of armies on the board that belong to me
        double armyRatio = myArmies / (double)totalArmies;

        // bonus armies at the start of a turn matter a lot in risk
        // normalize it roughly into [0, 1] using 10 as a soft scale
        double myBonus = state.getBonusArmiesFor(myAgentId);
        double bonusScore = Math.max(0.0, Math.min(1.0, myBonus / 10.0));

        // fewer living opponents is better
        // if there are no opponents left, this term becomes 1.0
        double opponentScore = 1.0 / (1.0 + livingOpponents.size());

        // combine the parts into one score in [0, 1]
        // for action reward, I care a bit more about expansion / board control
        double score = 0.40 * territoryRatio + 0.30 * armyRatio + 0.20 * bonusScore + 0.10 * opponentScore;
        double lengthPenalty = -0.5 * Math.min(1.0, state.getNumTurns() / 200.0);
        // convert [0, 1] into [-100, 100]
        // 0.5 becomes 0, larger than 0.5 is good, smaller is bad
        return 200.0 * (score - 0.5) + lengthPenalty * 10.0;
     } // this sucks you'll need to change this

    /** {@inheritDoc} */
    public double getHalfTransitionReward(final GameView state,
                                          final Action action) { return 0.0; }

    /** {@inheritDoc} */
    public double getFullTransitionReward(final GameView state,
                                          final Action action,
                                          final GameView nextState) { return 0.0; }

}

