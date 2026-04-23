package labs.acrobat;


// SYSTEM IMPORTS
import edu.bu.jmat.Matrix;
import edu.bu.jmat.Pair;
import edu.bu.jnn.Model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


// JAVA PROJECT IMPORTS


public class ReplayBuffer
    extends Object
{

    public static enum ReplacementType
    {
        RANDOM,
        OLDEST;
    }

    private ReplacementType     type;
    private int                 size;
    private int                 newestSampleIdx;

    private Matrix              prevStates;
    private Matrix              rewards;
    private Matrix              nextStates;
    private boolean             isStateTerminalMask[];

    private Random              rng;

    public ReplayBuffer(ReplacementType type,
                        int numSamples,
                        int dim,
                        Random rng)
    {
        this.type = type;
        this.size = 0;
        this.newestSampleIdx = -1;

        this.prevStates = Matrix.zeros(numSamples, dim);
        this.rewards = Matrix.zeros(numSamples, 1);
        this.nextStates = Matrix.zeros(numSamples, dim);
        this.isStateTerminalMask = new boolean[numSamples];

        this.rng = rng;

    }

    public int size() { return this.size; }
    public final ReplacementType getReplacementType() { return this.type; }
    private int getNewestSampleIdx() { return this.newestSampleIdx; }
    private Matrix getPrevStates() { return this.prevStates; }
    private Matrix getNextStates() { return this.nextStates; }
    private Matrix getRewards() { return this.rewards; }
    private boolean[] getIsStateTerminalMask() { return this.isStateTerminalMask; }

    private Random getRandom() { return this.rng; }

    private void setSize(int i) { this.size = i; }
    private void setNewestSampleIdx(int i) { this.newestSampleIdx = i; }

    private int chooseSampleToEvict()
    {
        int idxToEvict = -1;

        switch(this.getReplacementType())
        {
            case RANDOM:
                idxToEvict = this.getRandom().nextInt(this.getNextStates().getShape().numRows());
                break;
            case OLDEST:
                idxToEvict = (this.getNewestSampleIdx() + 1) % this.getNextStates().getShape().numRows();
                break;
            default:
                System.err.println("[ERROR] ReplayBuffer.chooseSampleToEvict: unknown replacement type "
                    + this.getReplacementType());
                System.exit(-1);
        }

        return idxToEvict;
    }

    public void addSample(Matrix prevState,
                          double reward,
                          Matrix nextState)
    {
        // TODO: complete me!

        // This method should add a new transition (prevState, reward, nextState) to the replayBuffer
        // However, we cannot just add this transition right away, we first have to check that there is space!
        //
        // A replay buffer can be configured to act like a circular buffer (i.e. overwrite the OLDEST transitions
        // first when we run out of space) OR it can be configured to overwrite RANDOM transitions.
        // This value is already provided for you when the ReplayBuffer object is created,
        // and can be accessed with the this.getReplacementType() method.

        // your method should work for both types of replacement!

        // After we determine the row index to insert this new transition into
        // there are several fields that need to be updated.
        //      - We want to put the prevState in the Matrix returned by this.getPrevStates()
        //      - We want to put the reward in the Matrix returned by this.getRewards()
        //      - We want to put nextState in the Matrix returned by this.getNextStates() but ONLY if it isnt Null!
        //          Since we need to store terminal transitions (i.e. transitions that end the game)
        //          its possible for nextState to be null. If it is, we don't want to add it
        //      - We want to update the array returned by this.getIsStateTerminalMask() with whether nextState
        //          is null or not. Put a true value if nextState is null, and false otherwise
        //      - We want to update any indexing information that we would need to keep the replacementType going
        //          - if there is space left, we need to increment this.getSize()
        //          - if there isn't space left and we have OLDEST replacement, we need to increment this.getNewestSampleIdx

        int idxToAssignTo = this.size();
        if(this.size() >= this.getNextStates().getShape().numRows())
        {
            // no space...evict something
            idxToAssignTo = this.chooseSampleToEvict();
        }

        try
        {
            this.getPrevStates().copySlice(idxToAssignTo, idxToAssignTo+1,
                                           0, this.getPrevStates().getShape().numCols(),
                                           prevState);
            this.getRewards().set(idxToAssignTo, 0, reward);
            if(nextState != null)
            {
                this.getNextStates().copySlice(idxToAssignTo, idxToAssignTo+1,
                                               0, this.getNextStates().getShape().numCols(),
                                               nextState);
            }
            this.getIsStateTerminalMask()[idxToAssignTo] = (nextState == null);
        } catch(Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        if(this.size() < this.getNextStates().getShape().numRows())
        {
            this.setSize(this.size() + 1);
        }

        // make a circular buffer
        if(this.getReplacementType().equals(ReplacementType.OLDEST))
        {
            this.setNewestSampleIdx((idxToAssignTo + 1) % this.getNextStates().getShape().numRows());
        }
    }

    public static double max(Matrix qValues) throws IndexOutOfBoundsException
    {
        double maxVal = 0;
        boolean initialized = false;

        for(int colIdx = 0; colIdx < qValues.getShape().numCols(); ++colIdx)
        {
            double qVal = qValues.get(0, colIdx);
            if(!initialized || qVal > maxVal)
            {
                maxVal = qVal;
                initialized = true;
            }
        }
        return maxVal;
    }


    public double getGroundTruth(Model qFunction,
                                 double discountFactor,
                                 int sampleIdx)
    {
        // TODO: complete me!

        // This method should calculate the bellman update for temporal difference learning so that
        // we can use it as ground truth for updating our neural network
        //
        // Remember, the bellman ground truth we want for a Q function looks like this:
        //      R(s) + \gamma * max_{a'} Q(s', a')

        // Since the number of actions is fixed in the CartPole (cp) world, we don't need to include
        // action information directly in the input vector to the q function. Instead, we'll make the neural
        // network always produce (in this case since there are 2 actions) 2 q values: one per action.
        // So whenever we need to max_{a'} Q(s', a'), we're literally going to feed s' into our network,
        // which will produce two scores, one for a_1' and one for a_2'. We can choose max_{a'} Q(s', a')
        // by choosing whichever value is largest!

        // Now note that this bellman update reduces to just R(s) whenever we're processing a terminal transition
        // (so s' doesn't exist).

        // This method should calculate a column vector. The number of rows in this column vector is equal to the
        // number of transitions currently stored in the ReplayBuffer. Each row corresponds to a transition
        // which could either be (s, r, s') or (s, r, null), so when calculating the bellman update for that row,
        // you need to check the mask to see which version you're calculating! 


        // return null;
        double ygt = this.getRewards().get(sampleIdx, 0);

        try
        {
            // if the input state is not terminal, then we need to ask the qFunction to rank that state
            if(this.getIsStateTerminalMask()[sampleIdx] == false)
            {
                ygt += discountFactor * max(qFunction.forward(this.getNextStates().getRow(sampleIdx)));
            }
        } catch(Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
        return ygt;
    }

    public Set<Integer> chooseUniqueSamples(final int batchSize)
    {
        Set<Integer> s = new HashSet<>();
        if(batchSize >= this.size())
        {
            for(int rIdx = 0; rIdx < this.size(); ++rIdx)
            {
                s.add(rIdx);
            }
        } else
        {
            // choose unique ones
            while(s.size() < batchSize)
            {
                int rIdx = this.getRandom().nextInt(this.size());
                if(!s.contains(rIdx)) { s.add(rIdx); }
            }
        }
        return s;
    }

    public Pair<Matrix, Matrix> sampleBatch(Model qFunction,
                                            double discountFactor,
                                            final int batchSize)
    {
        Matrix X = Matrix.zeros(batchSize, this.getPrevStates().getShape().numCols());
        Matrix YGt = Matrix.zeros(batchSize, 1);
        try
        {
            // choose sample (cannot select the same row twice)
            Set<Integer> sampleIdxs = this.chooseUniqueSamples(batchSize);

            int rowIdx = 0;
            for(Integer sampleIdx : sampleIdxs)
            {
                X.copySlice(rowIdx, rowIdx+1, 0, X.getShape().numCols(),
                            this.getPrevStates().getRow(sampleIdx));
                YGt.set(rowIdx, 0, this.getGroundTruth(qFunction, discountFactor, sampleIdx));
                rowIdx += 1;
            }
        } catch(Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        return new Pair<Matrix, Matrix>(X, YGt);
    }

}

