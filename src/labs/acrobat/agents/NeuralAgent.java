package labs.acrobat.agents;


// SYSTEM IMPORTS
import edu.bu.jmat.Matrix;
import edu.bu.jmat.Pair;
import edu.bu.jnn.LossFunction;
import edu.bu.jnn.Module;
import edu.bu.jnn.Model;
import edu.bu.jnn.Optimizer;
import edu.bu.jnn.layers.*;
import edu.bu.jnn.losses.MeanSquaredError;
import edu.bu.jnn.models.Sequential;
import edu.bu.jnn.optimizers.*;
import edu.bu.labs.acrobat.agents.QAgent;
import edu.bu.labs.acrobat.utils.DiscreteUtils;
import net.sourceforge.argparse4j.inf.Namespace;

import java.util.Iterator;
import java.util.Random;


// JAVA PROJECT IMPORTS
import labs.acrobat.ReplayBuffer;
import labs.acrobat.Dataset;


public class NeuralAgent
    extends Object
    implements QAgent
{

    private Model qFunction;
    private Optimizer optimizer;
    private LossFunction lossFunction;

    public NeuralAgent(final double lr)
    {
        Sequential m = new Sequential();

        // input is 6d
        m.add(new Dense(6, 36));
        m.add(new Sigmoid());

        // since the number of actions in this world is fixed, we can ask the network to predict
        // one q-value per (fixed ahead of time) actions. In this world there are three actions.
        m.add(new Dense(36, 3));

        this.qFunction = m;
        this.optimizer = new SGDOptimizer(qFunction.getParameters(), lr);
        this.lossFunction = new MeanSquaredError();
    }

    public NeuralAgent(Namespace ns) { this((double)(ns.get("lr"))); }

    public final Model getQFunction() { return this.qFunction; }
    public final Optimizer getOptimizer() { return this.optimizer; }
    public final LossFunction getLossFunction() { return this.lossFunction; }

    public final double getQValue(final Matrix state,
                                  final int action) throws Exception
    {
        return this.getQFunction().forward(state).get(0, action);
    }

    @Override
    public final void update(final Matrix states,
                             final Matrix actions,
                             final Matrix YGt,
                             final double lr) throws Exception
    {
        Matrix YHat = this.getQFunction().forward(states);
        this.getOptimizer().reset();
        this.getQFunction().backwards(states, this.getLossFunction().backwards(YHat, YGt));
        this.getOptimizer().step();
    }

    @Override
    public final int argmax(final Matrix state) throws Exception
    {
        Double bestQValue = null;
        int bestAction = -1;

        for(int action = 0; action < 3; ++action)
        {
            final double qValue = this.getQValue(state, action);
            if(bestQValue == null || qValue > bestQValue)
            {
                bestQValue = qValue;
                bestAction = action;
            }
        }
        return bestAction;
    }

    @Override
    public final double max(final Matrix state) throws Exception
    {
        return this.getQValue(state, this.argmax(state));
    }


    @Override
    public void save(final String filePath) throws Exception
    {
        this.getQFunction().save(filePath);
    }

    @Override
    public void load(final String filePath) throws Exception
    {
        this.getQFunction().load(filePath);
    }
}
