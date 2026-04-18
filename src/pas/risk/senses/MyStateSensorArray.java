package pas.risk.senses;


// SYSTEM IMPORTS
import edu.bu.jmat.Matrix;

import edu.bu.pas.risk.GameView;
import edu.bu.pas.risk.agent.senses.StateSensorArray;
import edu.bu.pas.risk.TerritoryOwnerView;

// JAVA PROJECT IMPORTS


/**
 * A suite of sensors to convert a {@link GameView} into a feature vector (must be a row-vector)
 */ 
public class MyStateSensorArray
    extends StateSensorArray
{
    public static final int NUM_FEATURES = 15;

    // Number of risk territories, and 6 contienents
    private static final int Total_Territories = 42;
    private static final int Total_continents = 6;

    public MyStateSensorArray(final int agentId)
    {
        super(agentId);
    }

    public Matrix getSensorValues(final GameView state)
    {   
        // default a double array size NUM_FEATRURS
        double[] features = new double[NUM_FEATURES];
        // get my agent id
        int my_id = this.getAgentId();
        // get the total number of agents
        int num_Agents = state.getNumAgents();

        // We know that there are 15 features, which mean we need to find out what we doing in each feature
        // ----- feature 0 ----- world conqured progress for my agent
        // get the number of territoreis that I(my agent) owned
        int My_Territories = state.getTerritoriesOwnedBy(my_id).size();
        // get the percentage(double here) of progress: e.g world conqured progress
        features[0] = (double) My_Territories / Total_Territories;

        // ----- feature 1 ----- world conqured progress for enemy agents (more than 1 enemy agents)
        // default opponent_terrotories as 0
        int Opponent_Territories = 0;
        // use a for loop to get all the territories that owned by enemy agents
        for (int i = 0; i < num_Agents; i++) {
            // as long as the agent is not my agent, add the terrotires owned by the enemy agents
            if (i != my_id) {
                Opponent_Territories += state.getTerritoriesOwnedBy(i).size();
            }
        }

        // store the information in feature [1]
        features[1] = (double) Opponent_Territories / Total_Territories;

        // ----- feature 2 ----- percentage of continents that my agents have
        // get my continets with my agents id
        int My_Continents = state.getContinentsOwnedBy(my_id).size();
        // calcuate the percentage of continents that my agents have(with all continents)
        features[2] = (double) My_Continents / Total_continents;

        // ----- feature 3 ----- Bounus armies each turn (NOTE: with no card 12 is a good choice: max continet is contraol asia = 7 , and max terrotrey is 15, 15/3= 5; so 7 + 5 = 12  )
        // calculate my bouns army for each term
        features[3] = Math.min(1.0, state.getBonusArmiesFor(my_id) / 12.0);
        
        // ----- feature 4 ---- The total armies I have in the world (the game world)
        // set my number of army to 0
        int My_Armies = 0;
        // use a for itreator to calcualte the total army that i have
        for (TerritoryOwnerView Territory_own_view: state.getTerritoryOwners()) {
            // if there is my agent's army exist 
            if (Territory_own_view.getOwner() == my_id) {
                My_Armies += Territory_own_view.getArmies();
            }
        }
        // store my total armies in feature 4 (remember to normalize it !!)
        features[4] = Math.log1p(My_Armies) / Math.log1p(200.0);

        // ----- feature 5 ----- Total armies for enemy agents in the game world
        // set the number of opponent armies in total to 0
        int Opponent_Armies = 0;
        // use a for itreator to calcualte the total army that i have
        for (TerritoryOwnerView Territory_own_view: state.getTerritoryOwners()) {
            // if there is my agent's army exist 
            if (Territory_own_view.getOwner() != my_id && Territory_own_view.getOwner() != -1) {
                    Opponent_Armies += Territory_own_view.getArmies();
            }
        }
        // store oppoents total armies in feature 5 (remember to normalize it !!)
        features[5] = Math.log1p(Opponent_Armies) / Math.log1p(200.0);

        // ----- feature 6 ----- my army over total armies include oppenents armies
        // calculate the total armies in the map
        int total_armies = My_Armies + Opponent_Armies;

        // if the total army is bigger than 0, then calcualte the ratio
        if (total_armies > 0) {
            features[6] = (double) My_Armies / total_armies;
        }else{
            // otherwise no army on map, assign 0 to feature[6]
            features[6] = 0.0;
        }

        // ----- feature 7 ----- calculate my territoreis over all terrritories
        features[7]= (double) My_Territories / Math.max(1, My_Territories + Opponent_Territories);
        // use 1 do avoid divide by zero

        // ----- feature 8 ----- total number of cards in my hand (normalized by 5 , e.g 5/5 = 1 mean i need to trade my card, it reach the capacity of the hand limit which is 5)
        // get the number of card in my agent hand 
        int my_cards = state.getAgentInventory(my_id).size();
        // normalize and store the value in feature 8
        features[8] =  Math.min(1.0, my_cards / 5.0); 

        // ----- feature 9 ----- calculate number of card pervois redemption(trade in) and remeber we also normalized here (10)
        features[9] = Math.min(1.0, state.getNumPreviousRedemptions() / 10.0);

        // ----- feature 10 ----- calculate how old is this game (use 200 , i think it a safter limit than smaller)
        features[10] = Math.min(1.0, state.getNumTurns() / 200.0);

        // ----- feature 11 ----- number of player still survived on the map (need also normalized)
        // default player survive as 0
        long players_survive = 0;
        // use a for loop to find the survive player
        for (int i = 0; i < num_Agents; i++) {
            // if the territoreis Onwed by someone that is not empty 
            if (!state.getTerritoriesOwnedBy(i).isEmpty()) {
                // add 1 to player survive
                players_survive ++;
            }
        }
        // save the number of player survive to feature 11 and remember to normlize by the number of agents
        features[11] = (double) players_survive / num_Agents;

        // ----- feature 12 ----- Whether I own the most number of territories
        // set a largest number of territoies owned by an opponent
        int max_oppen_Territories = 0;
        // use a for loop to find the largest .. owned by enemies
        for (int i = 0; i < num_Agents; i++) {
            // if i is not my agent id update the max...terriot..
            if (i != my_id) {
                // if its larger update, otherwise keep prevois vlaue
                max_oppen_Territories = Math.max(max_oppen_Territories, state.getTerritoriesOwnedBy(i).size());
            }
        }
        // if we have the most land, return 1 else 0
        if (My_Territories > max_oppen_Territories) {
            features[12] = 1.0;
        }else{
            features[12] = 0.0;
        }

        // ----- feature 13 ----- Whether I owned the most number of armies
        // default a max oppennet armies to 0
        int max_oppen_Armies = 0;
        // outitreate each agent
        for (int i = 0; i < num_Agents; i++) {
            // if it is not my agent id
            if (i != my_id) {
                // set a temp_opp_armies to 0
                int temp_opp_armies = 0;
                // itreate each owner in terrritoy own view
                for (TerritoryOwnerView Territory_own_view: state.getTerritoryOwners()) {
                    // if owner is i 
                    if (Territory_own_view.getOwner() == i) {
                        // store its armies in the temp armies
                        temp_opp_armies += Territory_own_view.getArmies();
                    }
                }
                // pick the largest armies, if it exist update the max.. otherwise keep the prevois max
                max_oppen_Armies = Math.max(max_oppen_Armies, temp_opp_armies);
            }
        }
        // if my agent has largest number of armies return 1, othersiwe return 0
        if (My_Armies > max_oppen_Armies) {
            features[13] = 1.0;
        }else{
            features[13] = 0.0;
        }

        // ----- feature 14 ----- my agent's bouns armies over best oppents bouns armies
        // default a best oppenent bouns armies to 0
        int best_opp_bouns = 0;
        // use a for loop to find the best opp bouns
        for (int i = 0; i < num_Agents; i++) {
            // if i is not my agent id
            if (i != my_id) {
                // if its better update, otherwise keep the prevois value
                best_opp_bouns = Math.max(best_opp_bouns, state.getBonusArmiesFor(i));
            }
        }
        // default a difference between bouns with my agents and best oppents bouns
        double Diff_Bouns = state.getBonusArmiesFor(my_id) - best_opp_bouns;
        // store and noramlize the difference to feature 14 (we want to make the number smal, and we constrain the number to be between -1.0 and 1.0)
        features[14] = (Math.max(-1.0, Math.min(1.0, Diff_Bouns / 10.0)) + 1.0) / 2.0;

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
        gt.set(0, 10, features[10]);
        gt.set(0, 11, features[11]);
        gt.set(0, 12, features[12]);
        gt.set(0, 13, features[13]);
        gt.set(0, 14, features[14]);

        return gt; // row vector

        // return Matrix.randn(1, NUM_FEATURES); // row vector
    }

}

