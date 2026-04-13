package pas.risk.senses;


// SYSTEM IMPORTS
import edu.bu.jmat.Matrix;

import edu.bu.pas.risk.GameView;
import edu.bu.pas.risk.action.Action;
import edu.bu.pas.risk.agent.senses.ActionSensorArray;
import edu.bu.pas.risk.TerritoryOwnerView;
import edu.bu.pas.risk.action.AttackAction;
import edu.bu.pas.risk.action.FortifyAction;
import edu.bu.pas.risk.action.NoAction;
import edu.bu.pas.risk.action.RedeemCardsAction;

// JAVA PROJECT IMPORTS


/**
 * A suite of sensors to convert a {@link Action} into a feature vector (must be a row-vector)
 */ 
public class MyActionSensorArray
    extends ActionSensorArray
{

    public static final int NUM_FEATURES = 10;

    public MyActionSensorArray(final int agentId)
    {
        super(agentId);
    }

    public Matrix getSensorValues(final GameView state,
                                  final int actionCounter,
                                  final Action action)
    {
        // get my agent id
        int my_id = this.getAgentId();
        // default features (double) with size num of features
        double [] features = new double[NUM_FEATURES];

        // ----- feature 0 ----- are we useing attack action (use instance of to check whether it belong to attackaction class)
        // if action is instance of attackaction class
        if (action instanceof AttackAction) {
            features[0] = 1.0; // true
        }else{
            features[0] = 0.0; // false 
        }

        // ----- feature 1 ----- are we useing FortifyAction (use instance of to check whether it belong to FortifyAction class)
        // if action is instance of FortifyAction class
        if (action instanceof FortifyAction) {
            features[1] = 1.0; // true
        }else{
            features[1] = 0.0; // false 
        }

        // ----- feature 2 ----- are we useing NoAction (use instance of to check whether it belong to NoAction class)
        // if action is instance of NoAction class
        if (action instanceof NoAction) {
            features[2] = 1.0; // true
        }else{
            features[2] = 0.0; // false 
        }

        // ----- feature 3 ----- are we useing RedeemCardsAction (use instance of to check whether it belong to RedeemCardsAction class)
        // if action is instance of RedeemCardsAction class
        if (action instanceof RedeemCardsAction) {
            features[3] = 1.0; // true
        }else{
            features[3] = 0.0; // false 
        }

        // ================== FEATURE 4 TO 8 ==================== (WILL BE COMPUTE IN IF ELSE STATEMENT LATER, HERE IS INITIALIZE)
        // ----- feature 4 ----- armies on the original territiories (normalized by 20; its a thersshold i set e.g 40 armies / 20 = 2.0 > 1.0 means its armies is strong)
        features[4] = 0.0;

        // ----- feature 5 ----- armies on the territory that we are targetting (same as feature 4 normalized by 20)
        features[5] = 0.0;

        // ----- feature 6 ----- nummber of armies used for attacked actoin (AttactAction) (normalized by 3)
        features[6] = 0.0;
        
        // ----- feature 7 ----- number of armies beging moved to target territory (FortyAction) (normalized by 20)
        features[7] = 0.0;
        
        // ----- feature 8 ----- number of neighours (oppnents)armies around the orginal territories (my land) (normalized by 6 )
        features[8] = 0.0;
        // ================== END OF FEATURE 4 TO 8 INIT ====================

        // ----- FEATURE 9 ----- what is the current turn's state, e.g start of the turn or already done many actions
        features[9] = Math.min(1.0, actionCounter/ 20.0);

        // ================== FILL FEATURE VALUE INTO FEATURE 4 TO FEATURE 8 =================
        // ----- if action is instance of attack action class -----
        if (action instanceof AttackAction) {
            // we need to cast acctacation on action so we can acess to the filed in attackaction
            AttackAction attack = (AttackAction) action;

            // get id of the territory view of orginal territoreis and owner of id of  target territories by Territoryownerviews
            TerritoryOwnerView Origin_territory = state.getTerritoryOwners().getById(attack.from().id());
            TerritoryOwnerView Target_territory = state.getTerritoryOwners().getById(attack.to().id());
            
            // Now we can update feature 4: detail explain in init
            features[4] = Math.min(1.0, Origin_territory.getArmies() / 20.0);

            // Now we can update feature 5: detail explain in init
            features[5] = Math.min(1.0, Target_territory.getArmies() / 20.0);

            // Now we can update feature 6: detail explain in init (Note: store the number of dice we are  using when attaking)
            features[6] = attack.attackingArmies() / 3.0;
            
            // Now we can update feature 8: detail explain in init
            // default number of oppnent neigbors to 0
            int oppnent_neighbors = 0;

            // itreate the onwer of territory with territoty owner view
            for (TerritoryOwnerView Terr_Own_view: state.getTerritoryOwners()) {
                // if the current territiroy is next to the orgin land and the land is owned by the enemies unit (not onwed by our agent id)
                if (attack.from().adjacentTerritories().contains(Terr_Own_view.getTerritory()) && Terr_Own_view.getOwner() != my_id && Terr_Own_view.getOwner() != -1) {
                    // increment the oppent neighbors (its NOT about armies)
                    oppnent_neighbors ++;
                }
            }
            // after iteration ,  normalize the value of oppenent neighbors and store in feature 8
            features[8] = Math.min(1.0, oppnent_neighbors / 6.0 );
        
        // ----- else if the actoin is instanceof FortifyActoin -----
        } else if (action instanceof FortifyAction) {
            // smae case the action to FortifyActon
            FortifyAction Fort_act = (FortifyAction) action;

            // smae as we done in attac action class get the id of the terrioty of original terriroty and target territory
            TerritoryOwnerView Origin_territory = state.getTerritoryOwners().getById(Fort_act.from().id());
            TerritoryOwnerView Target_territory = state.getTerritoryOwners().getById(Fort_act.to().id());
            
            // Now we can update feature 4: detail explain in init
            features[4] = Math.min(1.0, Origin_territory.getArmies() / 20.0);

            // Now we can update feature 5: detail explain in init
            features[5] = Math.min(1.0, Target_territory.getArmies() / 20.0);

            // Now we can update feature 7: detail explain in init (arimes are being moved)
            features[7] = Math.min(1.0, Fort_act.deltaArmies() / 20.0);
        }
        // ================== END OF FILLING FEAUTURE VALUE FROM 4 TO 8  =====================
        
        // create a row vector : a row vector is 1 * size of the features
        Matrix gt = Matrix.zeros(1, NUM_FEATURES);
        
        // becaues matrix is a private method, so need to set each feature value by hand
        gt.set(0, 0, features[0]);
        gt.set(0, 1, features[1]);
        gt.set(0, 2, features[2]);
        gt.set(0, 3, features[3]);
        gt.set(0, 4, features[4]);
        gt.set(0, 5, features[5]);
        gt.set(0, 6, features[6]);
        gt.set(0, 7, features[7]);
        gt.set(0, 8, features[8]);
        gt.set(0, 9, features[9]);
        
        return gt; // row vector

        // return Matrix.randn(1, NUM_FEATURES); // row vector
    }

}

