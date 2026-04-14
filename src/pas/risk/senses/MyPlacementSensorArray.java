package pas.risk.senses;


// SYSTEM IMPORTS
import edu.bu.jmat.Matrix;

import edu.bu.pas.risk.GameView;
import edu.bu.pas.risk.agent.senses.PlacementSensorArray;
import edu.bu.pas.risk.territory.Territory;
import edu.bu.pas.risk.TerritoryOwnerView;
import edu.bu.pas.risk.territory.Territory;

// JAVA PROJECT IMPORTS


/**
 * A suite of sensors to convert a {@link Territory} into a feature vector (must be a row-vector)
 */ 
public class MyPlacementSensorArray
    extends PlacementSensorArray
{

    public static final int NUM_FEATURES = 5;

    public MyPlacementSensorArray(final int agentId)
    {
        super(agentId);
    }

    public Matrix getSensorValues(final GameView state,
                                  final int numRemainingArmies,
                                  final Territory territory)
    {
        // default a feataures(double)  with size number of features
        double[] features = new double[NUM_FEATURES];
        // get my agent id
        int my_id = this.getAgentId();

        // get the territoty own view of the terriroty which we might consider to put take(occupied)
        TerritoryOwnerView Territory_view = state.getTerritoryOwners().getById(territory.id());

        // ----- feature 0 ----- current armies on this territory (also normalized by 20)
        features[0] = Math.min(1.0, Territory_view.getArmies() / 20.0);

        // ----- feature 1 ----- number of (oppenent) neighbours around the territory (normalized by 6)
        // we default a oppoent neghbour to 0
        int oppenent_neighbors = 0;
        // iterate all the neighbor territory of this territory
        for (Territory neigbors: territory.adjacentTerritories()) {
            // get the onwer of the neighbour territory
            TerritoryOwnerView neighbor_owner = state.getTerritoryOwners().getById(neigbors.id());
            // if the neighbor territory is onwed by a opponent ; NOTE: not owned by my agent
            if (neighbor_owner.getOwner() != my_id && neighbor_owner.getOwner() != -1) {
                // increment oppenent neighbors
                oppenent_neighbors ++;
            }
        }
        // normlized oppennt neigbors and store in feature 1
        features[1] = Math.min(1.0, oppenent_neighbors / 6.0);

        // ----- feature 2 ----- number of (allies; friend of my agent) neighbors aournd this territoy
                // we default a oppoent neghbour to 0
        int Good_neighbors = 0;
        // iterate all the neighbor territory of this territory
        for (Territory neigbors: territory.adjacentTerritories()) {
            // get the onwer of the neighbour territory
            TerritoryOwnerView neighbor_owner = state.getTerritoryOwners().getById(neigbors.id());
            // if the neighbor territory is onwed by a Good unit(onwed by my agent id) ; NOTE: not owned by my agent
            if (neighbor_owner.getOwner() == my_id) {
                // increment oppenent neighbors
                Good_neighbors ++;
            }
        }
        // normlized oppennt neigbors and store in feature 2
        features[2] = Math.min(1.0, Good_neighbors / 6.0);
        
        // ----- feature 3 ----- whether the territory is a boarder territory
        // if the number of opponent neighbor is greater than 0, mean it is a boarder territory
        if (oppenent_neighbors > 0) {
            features[3] = 1.0;
        }else{
            features[3] = 0.0;
        }
        
        // ----- feature 4 ----- the number of armies we still can place (like every turn we can place new armies , and that is what reamina aremy mean) (normalized by 20)
        features[4] = Math.min(1.0, numRemainingArmies / 20.0);        
        
        // create a row vector : a row vector is 1 * size of the features
        Matrix gt = Matrix.zeros(1, NUM_FEATURES);
        
        // becaues matrix is a private method, so need to set each feature value by hand
        gt.set(0, 0, features[0]);
        gt.set(0, 1, features[1]);
        gt.set(0, 2, features[2]);
        gt.set(0, 3, features[3]);
        gt.set(0, 4, features[4]);

        return gt; // row vector

        // return Matrix.randn(1, NUM_FEATURES); // row vector
    }

}

