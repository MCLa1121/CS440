package pas.risk.agent;

// SYSTEM IMPORTS
import edu.bu.jnn.layers.*;
import edu.bu.jnn.models.Sequential;

import edu.bu.pas.risk.GameView;
import edu.bu.pas.risk.TerritoryOwnerView;
import edu.bu.pas.risk.action.Action;
import edu.bu.pas.risk.action.AttackAction;
import edu.bu.pas.risk.action.NoAction;
import edu.bu.pas.risk.agent.NeuralQAgent;
import edu.bu.pas.risk.agent.rewards.RewardFunction;
import edu.bu.pas.risk.agent.senses.*;
import edu.bu.pas.risk.model.DualDecoderModel;
import edu.bu.pas.risk.territory.Territory;
import edu.bu.pas.risk.action.FortifyAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


// JAVA PROJECT IMPORTS
import pas.risk.rewards.MyActionRewardFunction;
import pas.risk.rewards.MyPlacementRewardFunction;
import pas.risk.senses.MyActionSensorArray;
import pas.risk.senses.MyPlacementSensorArray;
import pas.risk.senses.MyStateSensorArray;

/**
 * Represents a {@link NeuralQAgent} where all of the configuration options are specified. These configuration options
 * are:
 * <ol>
 *     <li>The architecture of the {@link DualDecoderModel} we're using for this assignment. More specifically, what
 *         is the architecture of the encoder, the action decoder, and the placement decoder?</li>
 *     <li>How is a state (e.g. a {@link GameView}) perceived by the model? This is done via a
 *         {@link MyStateSensorArray} object which is responsible for converting a {@link GameView} into a feature
 *         vector which *must* be a row-vector.</li>
 *     <li>How is an {@link Action} perceived by the model? This is done via a
 *         {@link MyActionSensorArray} object which is responsible for converting a {@link Action} into a feature
 *         vector which *must* be a row-vector.</li>
 *     <li>How is a {@link Territory} perceived by the model? This is done via a
 *         {@link MyPlacementSensorArray} object which is responsible for converting a {@link Territory} into a feature
 *         vector which *must* be a row-vector.</li>
 *     <li>How is the model punished/pleasured according to the quality of {@link Action}s that it chooses? This
 *         is done via a {@link MyActionRewardFunction} which you can configure to calculate R(s), R(s,a),
 *         or R(s,a,s')</li>
 *     <li>How is the model punished/pleasured according to the quality of {@link Territory}s that it chooses to place
 *         armies at? This is done via a {@link MyPlacementRewardFunction} which you can configure
 *         to calculate R(s), R(s,t), or R(s,t,s')</li>
 * </ol>
 *
 */
public class RiskQAgent
    extends NeuralQAgent
{

    // Here set up Explration start at           FOR EARLY TRAING 0.90   FOR LATER TRAING  0.30
    private static final double EXPLORE_START = 0.30;
    // Here set up Exploation end at             FOR EARLY TRAING 0.05   FOR LATER TRAING: 0.03
    private static final double EXPLORE_END   = 0.03;
    // Here we set the exploration decay rate at FOR EARLY TRAING:500000 FOR LATER TRAING: 100000.0 
    private static final double EXPLORE_DECAY = 100000.0;
    // Here set the eval epilion to 0.1 Later traing: we set to 0 becauase we do not want to learn randomly , we want to use the neraul network
    private static final double EVAL_EPSILON  = 0.0;

    // minimum exploration rate during training games — keeps games finishing fast, and set to 0.03 so we cap the traiing explroeaton floor 
    private static final double TRAIN_EXPLORE_FLOOR = 0.03;

    // max actions to evaluate in argmax, it prevents slowdown when armies accumulate, it will cause in the game be infinte game RAISE TO 10000 TO REDUCE NOISE
    private static final int MAX_ACTIONS = 10000;

    // max turns per game, after this force aggressive attacks to end game because we want to restrict the progress of the game
    private static final int MAX_TURNS = 3000;

    // set up a explore counter to know how many time it explre
    private int explore_counter = 0;
    
    public RiskQAgent(int agentId)
    {
        super(agentId);
    }
    // get current explortaoin decay rate is the current chance that the agent chooses exploration instead of trusting the model IT SELF.
    private double getCurrentExploreDecayRate()
    {   
        // Here apply the math formular to calcluate the current explroe decay rate
        return EXPLORE_END + (EXPLORE_START - EXPLORE_END) * Math.exp(-1.0 * explore_counter / EXPLORE_DECAY);
    }

    // a private method to decide whether we should go for an explore or not
    private boolean go_Explore()
    {   
        // if it is not training, then we do not want to move randomly in a evaluatoin
        if(!this.isTraining()) {
            return false;
        }

        // get the decay rate 
        double rate = getCurrentExploreDecayRate();
        
        // increment the explore counter
        explore_counter++;

        //
        double random_value = new Random().nextDouble();
        double threshold = Math.max(TRAIN_EXPLORE_FLOOR, rate);
        
        // if the random number is less then the threshold, then we explore
        if (random_value < threshold) {
            return true;
        
        // otherwise we do not explore
        }else{
            return false;
        }
    }

    // helper to reduce the size of the actions size being too large, it only keep up to the max actions
    private List<Action> CutActionSize(final List<Action> actions)
    {   
        // if the size of the action is samler then the max actions then just return the action
        if(actions.size() <= MAX_ACTIONS) {
            return actions;
        }

        // the list of reuslt will store the smallerlist of actions
        List<Action> result = new ArrayList<>();

        // a list of all non no actions will store in this array list
        List<Action> All_non_no_actions = new ArrayList<>();

        // itreate all action in actons
        for(Action act : actions) {
            // if the action is a not actions instance
            if(act instanceof NoAction) {
                // then added to the result list
                result.add(act);

            // otherwise, add the the non no actoin list
            }else{ 
                All_non_no_actions.add(act);
            }
        }

        // Here we shuffle the non no actions list, so it will make use to pick radomly from the list
        Collections.shuffle(All_non_no_actions, new Random());

        // add all the non no actons with size Max actons size(will always choose the smallwer size), math min will cap the size
        result.addAll(All_non_no_actions.subList(0, Math.min(MAX_ACTIONS, All_non_no_actions.size())));
        return result;
    }

    // a private helper method to find NoAction from a list
    private Action findNoAction(final List<Action> actions)
    {   
        // itrate through all actions
        for(Action act : actions) {
            // if it is an instance of no actions
            if(act instanceof NoAction) {
                // return act
                return act;
            }
        }
        // just return the first index in the list so we can have sth to return 
        return actions.get(0);
    }

    // This is a override method that controls our agent how to chooses the best action from a list of legal actions that it can chooose.
    @Override
    public Action argmax(final GameView game, final int Action_Counter, final List<Action> actions)
    {
        // if number of turns has reach to the max turn we set, then we would like to end the game quickly
        if(game.getNumTurns() > MAX_TURNS) {
                // crerate a list of attaks to store
                List<Action> attacks = new ArrayList<>();
                // create a list of attack that is less stable
                List<Action> some_attacks = new ArrayList<>();

                // we want to ensure our size of the action need to be small so do not stuck
                List<Action> Cut_actions = CutActionSize(actions);

                // itreate through the actions in the cut acctions
                for(Action act : Cut_actions) {
                    // if the act is an instance of attacakctions then we added to the list attacks
                    if(act instanceof AttackAction) {
                        // cast the act as attack actions
                        AttackAction attack = (AttackAction) act;

                        // get the number of our aramy reeady to attack
                        int Our_army_to_attack = game.getTerritoryOwners().getById(attack.from().id()).getArmies();
                        // get the number of enemy they are defence
                        int Enemy_targeting  = game.getTerritoryOwners().getById(attack.to().id()).getArmies();
            
                        // twice the enemy armies size, we added to attacks, we are in the advantage 
                        if(Our_army_to_attack >= 2 * Enemy_targeting) {
                            attacks.add(act);
                        }
                        
                        // if we have more army but not that much 
                        else if(Our_army_to_attack > Enemy_targeting) {
                            // then add the act to some attack
                            some_attacks.add(act);
                        }
                    }
                }
            
                // If attacks list is not empty, let the Q learning net work to choose the best attack.
                if(!attacks.isEmpty()) {
                    return super.argmax(game, Action_Counter, attacks);
                }
            
                // If some attacks list is not empty, let the Q learning net work to choose the best attack
                if(!some_attacks.isEmpty()) {
                    return super.argmax(game, Action_Counter, some_attacks);
                }
            
                
                // if we have no attck to make then we reutrn no actions
                return findNoAction(Cut_actions);
            }

        // Here cretae a list of actions for our model to use for the best actions
        List<Action> Model_Actions;

        // if it is training we want to cap the size of the actions 
        if (this.isTraining()) {
            Model_Actions = CutActionSize(actions);
        
        // if the agent is not traing
        }else{
            // check the action size , are we each the max actions that we set
            if (actions.size() <= MAX_ACTIONS) {
                // store the actinos to moedl actions
                Model_Actions = actions;
            
            // if the size is too big then cut the action size smaler and store the smaller size to model actions
            }else{
                Model_Actions = CutActionSize(actions);
            }
        }

        // if  there is only one action  that is available , just return the only action that we can make
        if(Model_Actions.size() == 1) {
            return Model_Actions.get(0);
        }

        // Here get get the number of card in hand of our aggent
        int card_counter = game.getAgentInventory(this.agentId()).size();
        
        // set foreced redemeed as boolean type
        boolean forced_redeemd;

        // if the actions counter is 0 and the card carder is larger then equal to 5
        if (Action_Counter == 0 && card_counter >= 5) {
            // then forced redemmed to true
            forced_redeemd = true;

        // otherwise if the card counter is larger then equal to 6 
        }else if (card_counter >= 6) {
            // then forced redemmed to true
            forced_redeemd = true;
        
        // if non of the statement can be satisfy, then foreced redeemed must be false
        }else{
            forced_redeemd = false;
        }

        // if foreced remedmmed is true
        if (forced_redeemd) {
            // create a list redemmed to store the non no actons
            List<Action> redemmed = new ArrayList<>();

            // itreateat all action from the model actions
            for (Action act : Model_Actions) {
                // if the act is not an instance of no actions            
                if (!(act instanceof NoAction)) {
                    // then add the acttion to the redeemed list
                    redemmed.add(act);
                }
            }

            // if the redemmed is not empty
            if (!redemmed.isEmpty()) {
                // return the current setting directly, it avoid choose no actions when redemmed cards
                return super.argmax(game, Action_Counter, redemmed);
            }
        }

        // if it is not training, and readom number is less then eval epsion NOTE; WHEN WE SET EVAL ESPSION, THIS BLOCK OF IF STATEMENT WILL NEVER BE USED!!!!
        if (!this.isTraining() && (new Random()).nextDouble() < EVAL_EPSILON) {
            
            // crate two list, one to store the aggrressive attack that is strong attack
            List<Action> Aggressive_attack = new ArrayList<>();
            // crreate a list of any attack that our agetn would make with non no action attack move
            List<Action> Any_Attack = new ArrayList<>();

            // loop every possible actions. in model actions
            for (Action act : Model_Actions) {
                // if the action is an intance of attack actoins
                if (act instanceof AttackAction) {
                    // then we cast this act to attack actions 
                    AttackAction  Attack_act = (AttackAction) act;

                    // we get the number of army we are going to attack from the attacking place
                    int Our_army_to_attack = game.getTerritoryOwners().getById(Attack_act.from().id()).getArmies();
                    // get the number of army that on the map of the enemy armies
                    int Enemy_targeting  = game.getTerritoryOwners().getById(Attack_act.to().id()).getArmies();
                    // add this action to the any attack liskt
                    Any_Attack.add(act);

                    // if we are twice of the power of the enemy , then BEAT THE HELL OF THE ENEMY
                    if (Our_army_to_attack >= 2 * Enemy_targeting) {
                        // Add the actions to the aggrtesive attack
                        Aggressive_attack.add(act);
                    }
                
                // otherwise if it is not an instance of attack actions, and not a action of no action
                } else if (!(act instanceof NoAction)) {
                    // we store the move directly to the any attack list
                    Any_Attack.add(act);
                }
            }
            
            // if after the for iteration, the aggresive attack is not empty, we return any aggressive attack (randomly)
            if (!Aggressive_attack.isEmpty()) {
                return chooseRandom(Aggressive_attack, new Random());
            }
                
            // if the agreesive attack is empty, but the any attack list is not empy, just pick a randonmm acctck from the any attack list
            if (!Any_Attack.isEmpty()) return chooseRandom(Any_Attack, new Random());
        }

        // if non, then just use the argmax from neraul network to decidde what actoin to do 
        return super.argmax(game, Action_Counter, Model_Actions);
    }

    // Here we set up override argmax territory so it will not get stuck placemtn during the eval procees
    @Override
    public Territory argmax(final GameView game, final boolean isSet, final int Remain_Armies)
    {   
        // if is it currently not tranning, then get into the if statement
        if(!this.isTraining()) {   
            // choices will store all the places that we could place our armies
            List<Territory> Choices = this.getPotentialPlacements(game, isSet, Remain_Armies);
            
            // List of boreders to store the borders
            List<Territory> List_of_Borders = new ArrayList<>();

            // itraste through all the choices that we have, which mean itrate each teeritory that we could place amery
            for(Territory Choice : Choices) {
                 // loop all hte neighorbour territory with current choices
                for(Territory Territory : Choice.adjacentTerritories()) {
                    // Here we get the teriitory owener informtain of the neighbour
                    TerritoryOwnerView Territory_owner = game.getTerritoryOwners().getById(Territory.id());
                    // set an if statement to check whether the teriritory is onwed by an enmpy
                    if(Territory_owner.getOwner() != this.agentId() && Territory_owner.getOwner() != -1){
                        // if the negobor is not owned by us and the enemy, add to the list of border
                        List_of_Borders.add(Choice);
                        // then break the statment, becuase one of the neighbour is border, mean there is no need to check further
                        break;
                    }
                }
            }
            // if the list of border is not empty , then just randomly return a territory
            if(!List_of_Borders.isEmpty()) {
                // default a best territory 
                Territory best_territory = null;
                // default a best q value as negative infinity
                double best_QValue = Double.NEGATIVE_INFINITY;
                
                // itrate through all choice from list of borders
                for(Territory choice : List_of_Borders) {

                    // get the q value
                    double q_value = this.eval(game, Remain_Armies, choice);
                    
                    // if the best terro is null or the q value is better then the current best
                    if(best_territory == null || q_value > best_QValue) {
                        // then update the best terr
                        best_territory = choice;
                        // update the best q value
                        best_QValue = q_value;
                    }
                }
                // then after the for loop return the best terr
                return best_territory;
            }

            // if not border can be used, then pick from choices in ramain armies
            if(!Choices.isEmpty()) {
                return super.argmax(game, isSet, Remain_Armies);
            }
        }
        // If the agent is training, or if there is no choice can be  returned above, then just return the normal argmax.
        return super.argmax(game, isSet, Remain_Armies);
    }

    /**
     * A method to create your neural network architecture. This is done by making three separate {@link Sequential}
     * instances (with appropriate dimensions) and then chucking them into the {@link DualDecoderModel} class I made
     * for you which coordinates them.
     *
     * @return  The {@link DualDecoderModel} which coordinates the three neural networks you make here.
     */
    public DualDecoderModel initModel()
    {
        // default model..you will likely want to change this

        // lookup how many features each item has
        final int numStateFeatures = MyStateSensorArray.NUM_FEATURES;
        final int numActionFeatures = MyActionSensorArray.NUM_FEATURES;
        final int numPlacementFeatures = MyPlacementSensorArray.NUM_FEATURES;

        // build the encoder...it is a sequential neural network that (eventually) converts
        // a state feature vector into a state encoding (with the size specified below)
        final int stateEncodingDim = 64;
        Sequential encoder = new Sequential();
        encoder.add(new Dense(numStateFeatures, 128));
        encoder.add(new Tanh());
        encoder.add(new Dense(128, stateEncodingDim));

        // build the action decoder...also a sequential model whose input vector has size
        // (stateEncodingDim + numActionFeatures) that (eventually) produces a single q-value
        final int actionDecoderInputDim = stateEncodingDim + numActionFeatures;
        Sequential actionDecoder = new Sequential();
        actionDecoder.add(new Dense(actionDecoderInputDim, 32));
        actionDecoder.add(new Sigmoid());
        actionDecoder.add(new Dense(32, 1));

        // build the placement decoder...also a sequential model whose input vector has size
        // (stateEncodingDim + numPlacementFeatures) that (eventually) produces a single q-value
        final int placementDecoderInputDim = stateEncodingDim + numPlacementFeatures;
        Sequential placementDecoder = new Sequential();
        placementDecoder.add(new Dense(placementDecoderInputDim, 32));
        placementDecoder.add(new Sigmoid());
        placementDecoder.add(new Dense(32, 1));

        return new DualDecoderModel(encoder, actionDecoder, placementDecoder);
    }

    /**
     * A method to create your state sensor suite.
     *
     * @return  Your state sensor suite
     */
    @Override
    public StateSensorArray createStateSensors()
    {
        return new MyStateSensorArray(this.agentId());
    }

    /**
     * A method to create your action sensor suite.
     *
     * @return  Your action sensor suite
     */
    @Override
    public ActionSensorArray createActionSensors()
    {
        return new MyActionSensorArray(this.agentId());
    }

    /**
     * A method to create your placement sensor suite.
     *
     * @return  Your placement sensor suite
     */
    @Override
    public PlacementSensorArray createPlacementSensors()
    {
        return new MyPlacementSensorArray(this.agentId());
    }

    /**
     * A method to create your action reward function.
     *
     * @return  Your action reward function
     */
    @Override
    public RewardFunction<Action> createActionReward()
    {
        return new MyActionRewardFunction(this.agentId());
    }

    /**
     * A method to create your placement reward function.
     *
     * @return  Your placement reward function
     */
    @Override
    public RewardFunction<Territory> createPlacementReward()
    {
        return new MyPlacementRewardFunction(this.agentId());
    }

    public static <T> T chooseRandom(final List<T> list,
                                     final Random random)
    {
        return list.get(random.nextInt(list.size()));
    }

    /**
     * A method to choose an {@link Action} when it is in the redeem phase of a turn. You are free to write your own
     * code to choose which move to explore however your decision should be stochastic (e.g. determinism is bad).
     *
     * @param game              the current state of the game
     * @param actionCounter     how many actions you've made so far in this turn
     * @param canRedeemCards    can you redeem cards
     * @return the {@link Action} to do
     */
    @Override
    public Action getExplorationRedeemAction(final GameView game,
                                             final int actionCounter,
                                             final boolean canRedeemCards)
    {
        // get the number of card in our agent hands
        int card_counter = game.getAgentInventory(this.agentId()).size();
        //force redeemd card when it is more than or equal to 5
        boolean forced_redeemd = card_counter >= 5;
        // If redeeming is forced to make, then we wnat to exclude NoAction move. Otherwise, allow NoAction as an option at the end as what we return .
        final List<Action> options = this.getRedeemActions(game, actionCounter, canRedeemCards, !forced_redeemd);
        
        // if the list of options is not empty then choose a random redeem move 
        if (!options.isEmpty()){
            return chooseRandom(options, new Random());
        }

        // other wise we can only make the move that is no actions just return no acitons for this agent
        return new NoAction(this.agentId());
    }

    /**
     * A method to decide whether to listen to your q-function or not. This will be called ever time your agent
     * needs to decide what move to make in the redeem phase of your turn.
     *
     * @param game              the current state of the game
     * @param actionCounter     how many actions you've made so far in this turn
     * @param canRedeemCards    can you redeem cards
     * @return <code>true</code> if <code>getExplorationRedeemAction</code> should be called or if your action
     *         q-function should be argmaxed.
     */
    @Override
    public boolean shouldExploreRedeemMovePhase(final GameView game,
                                                final int actionCounter,
                                                final boolean canRedeemCards)
    {
        // go explore will handle it 
        return go_Explore();
    }

    /**
     * A method to choose an {@link Action} when it is in the attacking phase of a turn. You are free to write your own
     * code to choose which move to explore however your decision should be stochastic (e.g. determinism is bad).
     *
     * @param game              the current state of the game
     * @param actionCounter     how many actions you've made so far in this turn
     * @param canRedeemCards    can you redeem cards
     * @return the {@link Action} to do
     */
    @Override
    public Action getExplorationAttackActionRedeemIfForced(final GameView game,
                                                           final int actionCounter,
                                                           final boolean canRedeemCards)
    {
        // options store all legal move  for this phase
        final List<Action> options = this.getAttackRedeemActions(game, actionCounter, canRedeemCards);
        // createa a random nubmer, so when exploratoin , choose a random number
        final Random random = new Random();

        // if the actions counter is larger then equal to ten, then stop the agent doing more actons
        if(actionCounter >= 10) {
            return findNoAction(options);
        }
    
        // Here crate thress lit
        // store all the aggressive attack that our agent could do
        List<Action> Aggressive_attack = new ArrayList<>();

        // store the action that the agent could take that is meduim
        List<Action> Any_Attack = new ArrayList<>();

        // store all the non no actions attack to this list
        List<Action> All_non_no_actions = new ArrayList<>();
        
        // itrate through all the actions from options
        for(Action action : options) {
            // if the current action is an inscntace of attack atoins
            if(action instanceof AttackAction) {
                // cast the actoins to attac actions
                AttackAction Attack_act = (AttackAction) action;
                
                // get the nubmer of our amry going to attack
                int Our_army_to_attack = game.getTerritoryOwners().getById(Attack_act.from().id()).getArmies();
                // get the number of emety at this place
                int Enemy_targeting  = game.getTerritoryOwners().getById(Attack_act.to().id()).getArmies();
                
                // if the amry of us is twice as big as the enemly
                if(Our_army_to_attack >= 2 * Enemy_targeting) {
                    // add the action to the list of aggressive attack
                    Aggressive_attack.add(action);

                // othereise if the we are just larger than the enmey size but not twice as large, add to the any attack
                } else if(Our_army_to_attack > Enemy_targeting) {
                    Any_Attack.add(action);
                }
                
                // add all actions that is not no actions to the all non no actions list
                All_non_no_actions.add(action);
            
            // othersie the instance of actions is not no actions just added to the list of all non no actions
            } else if(!(action instanceof NoAction)) {
                All_non_no_actions.add(action);
            }
        }
        
        // if the list of aggressive attack is not empty and the random number is less then 0.8, we make the action aggressive
        if(!Aggressive_attack.isEmpty() && random.nextDouble() < 0.80) {
            return chooseRandom(Aggressive_attack, random);
        }
    
        // is the list of the any attack is not empty , and the radnom number is less then 0.4 , we make the action at any attack
        if(!Any_Attack.isEmpty() && random.nextDouble() < 0.40) {
            return chooseRandom(Any_Attack, random);
        }
        
        // if the list of all non no actions is not empty, and the radndom number is less than 0.15, then we should be pick what every we ahve to aatack or not
        if(!All_non_no_actions.isEmpty() && random.nextDouble() < 0.15) {
            return chooseRandom(All_non_no_actions, random);
        }
        
        // if no actions can be made, then we can only do is no actons
        return findNoAction(options);
    }

    /**
     * A method to decide whether to listen to your q-function or not. This will be called ever time your agent
     * needs to decide what move to make in the attacking phase of your turn.
     *
     * @param game              the current state of the game
     * @param actionCounter     how many actions you've made so far in this turn
     * @param canRedeemCards    can you redeem cards
     * @return <code>true</code> if <code>getExplorationAttackActionRedeemIfForced</code> should be called or
     *         if your action q-function should be argmaxed.
     */
    @Override
    public boolean shouldExploreAttackRedeemIfForcedMovePhase(final GameView game,
                                                              final int actionCounter,
                                                              final boolean canRedeemCards)
    {
        // go explore will handle it
        return go_Explore();
    }

    /**
     * A method to choose an {@link Action} when it is in the fortifying phase of a turn. You are free to write your own
     * code to choose which move to explore however your decision should be stochastic (e.g. determinism is bad).
     *
     * @param game              the current state of the game
     * @param actionCounter     how many actions you've made so far in this turn
     * @param canRedeemCards    can you redeem cards
     * @return the {@link Action} to do
     */
    @Override
    public Action getExplorationFortifySkipAction(final GameView game,
                                                  final int actionCounter,
                                                  final boolean canRedeemCards)
    {
        final List<Action> options = this.getFortifyActions(game, actionCounter, canRedeemCards);
        // set a random nunber
        final Random random = new Random();
        
        // a fortify action list to store the actoin of fortify actions
        List<Action> fortify_actions = new ArrayList<>();
        
        // itreate through the optoins that is legal 
        for(Action action : options) {
            // if the actions is the instance of fortify actions
            if(action instanceof FortifyAction) {
                // then acced the acton to the list
                fortify_actions.add(action);
            }
        }
        
        // becaxuse most of the time, we want to skip fortify because bad random fortify can hurt our traiing process.
        if(random.nextDouble() < 0.70) {
            // so if the number is not larger then 0.7 then we make no actonsn
            return findNoAction(options);
        }
    
        // if the fortiy action list is not empy and it is not cap at 0.7, then Sometimes explore a fortify move.
        if(!fortify_actions.isEmpty()) {
            // return a fority actions randomly
            return chooseRandom(fortify_actions, random);
        }
        
        // if non move can be make, return no actions
        return findNoAction(options);
    }

    /**
     * A method to decide whether to listen to your q-function or not. This will be called ever time your agent
     * needs to decide what move to make in the fortifying phase of your turn.
     *
     * @param game              the current state of the game
     * @param actionCounter     how many actions you've made so far in this turn
     * @param canRedeemCards    can you redeem cards
     * @return <code>true</code> if <code>getExplorationFortifySkipAction</code> should be called or
     *         if your action q-function should be argmaxed.
     */
    @Override
    public boolean shouldExploreFortifySkipMovePhase(final GameView game,
                                                     final int actionCounter,
                                                     final boolean canRedeemCards)
    {
        // go explore will handle it
        return go_Explore();
    }

    /**
     * A method to choose an {@link Territory} when it is in the army placing phase of a turn (or during game setup).
     * You are free to write your own code to choose which move to explore however your decision should be stochastic
     * (e.g. determinism is bad).
     *
     * @param game              the current state of the game
     * @param isDuringSetup     is this during the game setup or at the beginning of your move
     * @param remainingArmies   number of armies left to place
     * @return the {@link Territory} to place an army at
     */
    @Override
    public Territory getExplorationPlacement(final GameView game,
                                             final boolean isDuringSetup,
                                             final int remainingArmies)
    {
        final List<Territory> options = this.getPotentialPlacements(game, isDuringSetup, remainingArmies);
        // set a reandom number
        final Random random = new Random();
        // a list continent to store the continent terro we could take
        List<Territory> continent = new ArrayList<>();
        // creata a border list to store
        List<Territory> border = new ArrayList<>();
        
        // itrate the territory from optoins
        for(Territory territory : options) {
            boolean is_border = false;
            // itrate the neibhour teriritory from the territory adj
            for(Territory neighgour_terr : territory.adjacentTerritories()) {
                // get the onver view of the neighbour terr
                TerritoryOwnerView Neighour_terr_Owner = game.getTerritoryOwners().getById(neighgour_terr.id());
                
                // if the neighrobur ownerwhip is not us and not others then add to the border, so make is boader is true
                if(Neighour_terr_Owner.getOwner() != this.agentId() && Neighour_terr_Owner.getOwner() != -1) {
                    is_border = true;
                    // and break is no need to further iteration
                    break;
                }
            }
            
            // if is border is true add to the border list
            if(is_border) {
                border.add(territory);
            }
    
            // set thenumber of continent terror nuimber of eneemy and our agent
            int my_terro_counter = 0;
            int enemy_terror_counter = 0;
            // freee land counter and the total number of it
            int freeland_counter = 0;
            
            // iterate all the conteinet terrorties in the territores
            for(Territory continent_terr : territory.continent().territories()) {
                // get the view of the conieint owner view
                TerritoryOwnerView continent_ownerView = game.getTerritoryOwners().getById(continent_terr.id());
                
                // if this contient is our agent then increnet in our terro counter
                if(continent_ownerView.getOwner() == this.agentId()) {
                    my_terro_counter++;

                // if the ouner ship is no one, its a free land than add to free continent counter 
                } else if(continent_ownerView.getOwner() == -1) {
                    freeland_counter++;
                
                // othersiwe the conntinent is already owned by the enemy, increment the enemy continent counter
                }else{
                    enemy_terror_counter++;
                }
            }
            
            // calcualte the total number of continent that is not own by use
            int Not_own_by_us = enemy_terror_counter + freeland_counter;
    
            // if it is still During setup, we prefer continuing in a continent where we already own territory.
            if(isDuringSetup) {
                if(my_terro_counter > 0) {
                    // so add the territoty to the continent
                    continent.add(territory);
                }
            
            // other wise we perfer the border
            }else{
                // if is border is ture and the number of coninent not own by use is less then 2 and our terro of continent is euqal to the total count, and perturn amries is larger then 3 in the continect and the terro of mine continent is larger than equal to 2
                if(is_border && (Not_own_by_us <= 2 || (territory.continent().armiesPerTurn() >= 3 && my_terro_counter >= 2))) {
                    // then add this territory to the continent
                    continent.add(territory);
                }
            }
        }

        // if it is During setup, then random is okay because territories are still being claimed (free land to take).
        if(isDuringSetup) {
            // if it is during set up and we still perfer to have continent as a 40 percentage change
            if(!continent.isEmpty() && random.nextDouble() < 0.40) {
                return chooseRandom(continent, random);
            }
    
            return chooseRandom(options, random);
        }


        // if the list of continent is not emtpy and have a 70 chance of doing it
        if(!continent.isEmpty() && random.nextDouble() < 0.70) {
            // then pick randomly from the continent list
            return chooseRandom(continent, random);
        }
    
        // if during the set up the border list is not empty, and the radnom nuimber give 80 percentage of change to choose
        if(!border.isEmpty() && random.nextDouble() < 0.80) {
            // we pick randly from the border list
            return chooseRandom(border, random);
        }   
        
        // otherwise just pick the valid optoins 
        return chooseRandom(options, random);
    }

    /**
     * A method to decide whether to listen to your q-function or not. This will be called ever time your agent
     * needs to decide what {@link Territory} to place an army at.
     *
     * @param game              the current state of the game
     * @param isDuringSetup     is this during the game setup or at the beginning of your move
     * @param remainingArmies   number of armies left to place
     * @return <code>true</code> if <code>getExplorationPlacement</code> should be called or
     *         if your action q-function should be argmaxed.
     */
    @Override
    public boolean shouldExplorePlacementPhase(final GameView game,
                                               final boolean isDuringSetup,
                                               final int remainingArmies)
    {
        // go explore will handle it
        return go_Explore();
    }

}

