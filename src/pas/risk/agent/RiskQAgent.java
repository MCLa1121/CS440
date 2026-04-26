package pas.risk.agent;

// SYSTEM IMPORTS
import edu.bu.jnn.layers.*;
import edu.bu.jnn.models.Sequential;
import edu.bu.pas.risk.action.AttackAction;
import edu.bu.pas.risk.GameView;
import edu.bu.pas.risk.action.Action;
import edu.bu.pas.risk.agent.NeuralQAgent;
import edu.bu.pas.risk.agent.rewards.RewardFunction;
import edu.bu.pas.risk.agent.senses.*;
import edu.bu.pas.risk.model.DualDecoderModel;
import edu.bu.pas.risk.territory.Territory;
import edu.bu.pas.risk.action.NoAction;
import edu.bu.pas.risk.TerritoryOwnerView;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;

// JAVA PROJECT IMPORTS
import pas.risk.rewards.MyActionRewardFunction;
import pas.risk.rewards.MyPlacementRewardFunction;
import pas.risk.senses.MyActionSensorArray;
import pas.risk.senses.MyPlacementSensorArray;
import pas.risk.senses.MyStateSensorArray;

public class RiskQAgent
    extends NeuralQAgent
{
    private static final double EXPLORE_START = 0.90;
    private static final double EXPLORE_END   = 0.05;
    private static final double EXPLORE_DECAY = 500000.0;
    private static final double EVAL_EPSILON  = 0.0;

    // minimum exploration rate during training games — keeps games finishing fast
    private static final double TRAIN_EXPLORE_FLOOR = 0.05;

    // max actions to evaluate in argmax — prevents slowdown when armies accumulate
    private static final int MAX_ACTIONS = 6000;

    // max turns per game — after this force aggressive attacks to end game
    private static final int MAX_TURNS = 2000;

    private static final boolean DEBUG = false; // set to true to enable debug prints

    private int explore_counter = 0;

    public RiskQAgent(int agentId)
    {
        super(agentId);
    }

    private double getCurrentExploreDecayRate()
    {
        return EXPLORE_END + (EXPLORE_START - EXPLORE_END)
               * Math.exp(-1.0 * explore_counter / EXPLORE_DECAY);
    }

    private boolean go_Explore()
    {
        // during eval NEVER explore — always use the trained Q-function
        if(!this.isTraining()) return false;
        // during training explore at decayed rate (with floor)
        double rate = getCurrentExploreDecayRate();
        explore_counter++;
        return (new Random()).nextDouble() < Math.max(TRAIN_EXPLORE_FLOOR, rate);
    }

    private void debug(String msg)
    {
        if(DEBUG) System.out.println(msg);
    }

    // helper to subsample a large action list — always keeps NoAction, samples rest
    private List<Action> subsample(final List<Action> actions)
    {
        if(actions.size() <= MAX_ACTIONS) return actions;

        debug("[DEBUG] subsampling actions from " + actions.size() + " to " + MAX_ACTIONS);

        List<Action> result = new ArrayList<>();
        List<Action> attackOnly = new ArrayList<>();

        for(Action a : actions) {
            if(a instanceof NoAction) result.add(a);
            else attackOnly.add(a);
        }

        Collections.shuffle(attackOnly, new Random());
        result.addAll(attackOnly.subList(0, Math.min(MAX_ACTIONS, attackOnly.size())));
        return result;
    }

    // helper to find NoAction from a list
    private Action findNoAction(final List<Action> actions)
    {
        for(Action a : actions) {
            if(a instanceof NoAction) return a;
        }
        return actions.get(0);
    }

    // ----------------------------------------------------------------
    // ARGMAX OVERRIDE (Action) — fixes forced-redeem + eval stalling
    // ----------------------------------------------------------------
    @Override
    public Action argmax(final GameView game,
                         final int actionCounter,
                         final List<Action> actions)
    {
        // ---- after MAX_TURNS force aggressive attack to end game quickly ----
        if(game.getNumTurns() > MAX_TURNS)
            {
                debug("[DEBUG] MAX_TURNS exceeded, forcing aggressive attack");
                List<Action> attacks = new ArrayList<>();
                // subsample first to avoid iterating 187k actions
                List<Action> sampled = subsample(actions);
                for(Action a : sampled) {
                    if(a instanceof AttackAction) attacks.add(a);
                }
                if(!attacks.isEmpty()) return chooseRandom(attacks, new Random());
                return findNoAction(sampled);
            }

        // Only subsample during training; use all actions during eval for better decisions
        final List<Action> workingActions = this.isTraining() ? subsample(actions) : 
        (actions.size() <= MAX_ACTIONS ? actions : subsample(actions));

        debug("[DEBUG] argmax called: isTraining=" + this.isTraining() + " actionCounter=" + actionCounter + " actions.size()=" + workingActions.size());

        // if only one action available just return it immediately
        if(workingActions.size() == 1)
        {
            debug("[DEBUG] size=1: myTerritories=" +
                game.getTerritoriesOwnedBy(this.agentId()).size() +
                " isOver=" + game.isOver() +
                " actionCounter=" + actionCounter);
            return workingActions.get(0);
        }

        int cardCount = game.getAgentInventory(this.agentId()).size();
        boolean forcedRedeem = (actionCounter == 0 && cardCount >= 5) || cardCount >= 6;

        if (forcedRedeem) {
            List<Action> redeemOnly = new ArrayList<>();
            for (Action a : workingActions) {
                if (!(a instanceof NoAction)) redeemOnly.add(a);
            }
            if (!redeemOnly.isEmpty()) {
                debug("[DEBUG] forced redeem, redeemOnly.size()=" + redeemOnly.size());
                return super.argmax(game, actionCounter, redeemOnly);
            }
        }

        if (!this.isTraining() && (new Random()).nextDouble() < EVAL_EPSILON) {
            debug("[DEBUG] EVAL_EPSILON triggered");
            List<Action> preferred = new ArrayList<>();
            List<Action> anyAttack = new ArrayList<>();
            for (Action a : workingActions) {
                if (a instanceof AttackAction) {
                    AttackAction atk = (AttackAction) a;
                    int mine = game.getTerritoryOwners().getById(atk.from().id()).getArmies();
                    int foe  = game.getTerritoryOwners().getById(atk.to().id()).getArmies();
                    anyAttack.add(a);
                    if (mine >= 2 * foe) preferred.add(a);
                } else if (!(a instanceof NoAction)) {
                    anyAttack.add(a);
                }
            }
            debug("[DEBUG] preferred.size()=" + preferred.size() + " anyAttack.size()=" + anyAttack.size());
            if (!preferred.isEmpty()) return chooseRandom(preferred, new Random());
            if (!anyAttack.isEmpty()) return chooseRandom(anyAttack, new Random());
        }

        debug("[DEBUG] falling through to super.argmax");
        return super.argmax(game, actionCounter, workingActions);
    }

    // ----------------------------------------------------------------
    // ARGMAX OVERRIDE (Territory) — prevents stuck placement during eval
    // ----------------------------------------------------------------
    @Override
    public Territory argmax(final GameView game, final boolean isDuringSetup, final int numRemainingArmies)
    {
        if(!this.isTraining())
        {
            List<Territory> options = this.getPotentialPlacements(game, isDuringSetup, numRemainingArmies);
            List<Territory> border = new ArrayList<>();
            for(Territory t : options)
            {
                for(Territory adj : t.adjacentTerritories())
                {
                    TerritoryOwnerView adjOv = game.getTerritoryOwners().getById(adj.id());
                    if(adjOv.getOwner() != this.agentId() && adjOv.getOwner() != -1)
                    {
                        border.add(t);
                        break;
                    }
                }
            }
            if(!border.isEmpty()) return chooseRandom(border, new Random());
            if(!options.isEmpty()) return chooseRandom(options, new Random());
        }
        return super.argmax(game, isDuringSetup, numRemainingArmies);
    }

    // ----------------------------------------------------------------
    // MODEL ARCHITECTURE
    // ----------------------------------------------------------------
    @Override
    public DualDecoderModel initModel()
    {
        final int numStateFeatures     = MyStateSensorArray.NUM_FEATURES;
        final int numActionFeatures    = MyActionSensorArray.NUM_FEATURES;
        final int numPlacementFeatures = MyPlacementSensorArray.NUM_FEATURES;

        final int stateEncodingDim = 64;
        Sequential encoder = new Sequential();
        encoder.add(new Dense(numStateFeatures, 128));
        encoder.add(new Tanh());
        encoder.add(new Dense(128, stateEncodingDim));

        final int actionDecoderInputDim = stateEncodingDim + numActionFeatures;
        Sequential actionDecoder = new Sequential();
        actionDecoder.add(new Dense(actionDecoderInputDim, 32));
        actionDecoder.add(new Sigmoid());
        actionDecoder.add(new Dense(32, 1));

        final int placementDecoderInputDim = stateEncodingDim + numPlacementFeatures;
        Sequential placementDecoder = new Sequential();
        placementDecoder.add(new Dense(placementDecoderInputDim, 32));
        placementDecoder.add(new Sigmoid());
        placementDecoder.add(new Dense(32, 1));

        return new DualDecoderModel(encoder, actionDecoder, placementDecoder);
    }

    // ----------------------------------------------------------------
    // SENSORS AND REWARDS
    // ----------------------------------------------------------------
    @Override
    public StateSensorArray createStateSensors()
    {
        return new MyStateSensorArray(this.agentId());
    }

    @Override
    public ActionSensorArray createActionSensors()
    {
        return new MyActionSensorArray(this.agentId());
    }

    @Override
    public PlacementSensorArray createPlacementSensors()
    {
        return new MyPlacementSensorArray(this.agentId());
    }

    @Override
    public RewardFunction<Action> createActionReward()
    {
        return new MyActionRewardFunction(this.agentId());
    }

    @Override
    public RewardFunction<Territory> createPlacementReward()
    {
        return new MyPlacementRewardFunction(this.agentId());
    }

    public static <T> T chooseRandom(final List<T> list, final Random random)
    {
        return list.get(random.nextInt(list.size()));
    }

    // ----------------------------------------------------------------
    // EXPLORATION METHODS
    // ----------------------------------------------------------------
    @Override
    public Action getExplorationRedeemAction(final GameView game,
                                             final int actionCounter,
                                             final boolean canRedeemCards)
    {
        final List<Action> options = this.getRedeemActions(game, actionCounter, canRedeemCards, false);
        if(!options.isEmpty())
        {
            return chooseRandom(options, new Random());
        }
        return new NoAction(this.agentId());
    }

    @Override
    public boolean shouldExploreRedeemMovePhase(final GameView game,
                                                final int actionCounter,
                                                final boolean canRedeemCards)
    {
        int cardCount = game.getAgentInventory(this.agentId()).size();
        if ((actionCounter == 0 && cardCount >= 5) || cardCount >= 6) {
            return true;
        }
        return go_Explore();
    }

    @Override
    public Action getExplorationAttackActionRedeemIfForced(final GameView game,
                                                           final int actionCounter,
                                                           final boolean canRedeemCards)
    {
        final List<Action> options = this.getAttackRedeemActions(game, actionCounter, canRedeemCards);
        final Random random = new Random();

        // after MAX_TURNS force aggressive attacks to end game quickly
        if(game.getNumTurns() > MAX_TURNS)
        {
            debug("[DEBUG] MAX_TURNS exceeded in exploration, forcing attack");
            List<Action> anyAttack = new ArrayList<>();
            for(Action a : options) {
                if(a instanceof AttackAction) anyAttack.add(a);
            }
            if(!anyAttack.isEmpty()) return chooseRandom(anyAttack, random);
            return findNoAction(options);
        }

        // normal turn limit
        if(actionCounter >= 10)
        {
            for (Action a : options) {
                if (a instanceof NoAction) return a;
            }
        }

        List<Action> preferred = new ArrayList<>();
        List<Action> anyAttack = new ArrayList<>();

        for (Action a : options) {
            if (a instanceof AttackAction) {
                AttackAction atk = (AttackAction) a;
                int mine = game.getTerritoryOwners().getById(atk.from().id()).getArmies();
                int foe  = game.getTerritoryOwners().getById(atk.to().id()).getArmies();
                anyAttack.add(a);
                if (mine >= 2 * foe) preferred.add(a);
            } else if (!(a instanceof NoAction)) {
                anyAttack.add(a);
            }
        }

        if (!preferred.isEmpty() && random.nextDouble() < 0.80)
            return chooseRandom(preferred, random);
        if (!anyAttack.isEmpty())
            return chooseRandom(anyAttack, random);
        return chooseRandom(options, random);
    }

    @Override
    public boolean shouldExploreAttackRedeemIfForcedMovePhase(final GameView game,
                                                              final int actionCounter,
                                                              final boolean canRedeemCards)
    {
        return go_Explore();
    }

    @Override
    public Action getExplorationFortifySkipAction(final GameView game,
                                                  final int actionCounter,
                                                  final boolean canRedeemCards)
    {
        final List<Action> options = this.getFortifyActions(game, actionCounter, canRedeemCards);
        return chooseRandom(options, new Random());
    }

    @Override
    public boolean shouldExploreFortifySkipMovePhase(final GameView game,
                                                     final int actionCounter,
                                                     final boolean canRedeemCards)
    {
        return go_Explore();
    }

    @Override
    public Territory getExplorationPlacement(final GameView game,
                                             final boolean isDuringSetup,
                                             final int remainingArmies)
    {
        final List<Territory> options = this.getPotentialPlacements(game, isDuringSetup, remainingArmies);
        return chooseRandom(options, new Random());
    }

    @Override
    public boolean shouldExplorePlacementPhase(final GameView game,
                                               final boolean isDuringSetup,
                                               final int remainingArmies)
    {
        return go_Explore();
    }
}
