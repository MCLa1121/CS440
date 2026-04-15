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
    private static final double EXPLORE_DECAY = 10000.0;
    private static final double EVAL_EPSILON  = 0.5;

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
        double rate = getCurrentExploreDecayRate();
        explore_counter++;
        return (new Random()).nextDouble() < rate;
    }

    // ----------------------------------------------------------------
    // ARGMAX OVERRIDE — fixes forced-redeem elimination + eval stalling
    // ----------------------------------------------------------------
    @Override
    public Action argmax(final GameView game,
                         final int actionCounter,
                         final List<Action> actions)
    {
        int cardCount = game.getAgentInventory(this.agentId()).size();
        boolean forcedRedeem = (actionCounter == 0 && cardCount >= 5) || cardCount >= 6;

        // if forced to redeem, never let model pick NoAction
        if (forcedRedeem) {
            List<Action> redeemOnly = new ArrayList<>();
            for (Action a : actions) {
                if (!(a instanceof NoAction)) redeemOnly.add(a);
            }
            if (!redeemOnly.isEmpty()) {
                return super.argmax(game, actionCounter, redeemOnly);
            }
        }

        // during eval, inject small epsilon to prevent stalling
        if (!this.isTraining() && (new Random()).nextDouble() < EVAL_EPSILON) {
            List<Action> preferred = new ArrayList<>();
            List<Action> anyAttack = new ArrayList<>();
            for (Action a : actions) {
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
            if (!preferred.isEmpty()) return chooseRandom(preferred, new Random());
            if (!anyAttack.isEmpty()) return chooseRandom(anyAttack, new Random());
        }

        return super.argmax(game, actionCounter, actions);
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
        List<Action> options = this.getRedeemActions(game, actionCounter, canRedeemCards, false);
        if (!options.isEmpty()) {
            return chooseRandom(options, new Random());
        }
        return chooseRandom(
            this.getRedeemActions(game, actionCounter, canRedeemCards, true),
            new Random()
        );
    }

    @Override
    public boolean shouldExploreRedeemMovePhase(final GameView game,
                                                final int actionCounter,
                                                final boolean canRedeemCards)
    {
        int cardCount = game.getAgentInventory(this.agentId()).size();
        if ((actionCounter == 0 && cardCount >= 5) || cardCount >= 6) {
            return true; // forced redeem — always explore, never let argmax pick NoAction
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

        if (actionCounter >= 10) {
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