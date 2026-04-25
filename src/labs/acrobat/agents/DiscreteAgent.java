package labs.acrobat.agents;


// SYSTEM IMPORTS
import edu.bu.jmat.Matrix;
import edu.bu.jmat.Pair;
import edu.bu.labs.acrobat.agents.QAgent;
import edu.bu.labs.acrobat.utils.DiscreteUtils;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


// JAVA PROJECT IMPORTS


public class DiscreteAgent
    extends Object
    implements QAgent
{

    private Matrix theta1Cos;
    private Matrix theta1Sin;
    private Matrix theta2Cos;
    private Matrix theta2Sin;
    private Matrix theta1W;
    private Matrix theta2W;
    private double qTable[][][][][][][];

    public DiscreteAgent(final int steps)
    {
        this.theta1Cos = DiscreteUtils.linspace(-1, +1, steps);
        this.theta1Sin = DiscreteUtils.linspace(-1, +1, steps);
        this.theta2Cos = DiscreteUtils.linspace(-1, +1, steps);
        this.theta2Sin = DiscreteUtils.linspace(-1, +1, steps);
        this.theta1W = DiscreteUtils.linspace(-4*Math.PI, +4*Math.PI, steps);
        this.theta2W = DiscreteUtils.linspace(-9*Math.PI, +9*Math.PI, steps);


        this.qTable = new double[this.getTheta1Cos().getShape().numRows()]
            [this.getTheta1Sin().getShape().numRows()]
            [this.getTheta2Cos().getShape().numRows()]
            [this.getTheta2Sin().getShape().numRows()]
            [this.getTheta1W().getShape().numRows()]
            [this.getTheta2W().getShape().numRows()]
            [3];
    }

    public DiscreteAgent(Namespace ns) { this((int)(ns.get("discretizeSteps"))); }

    public final Matrix getTheta1Cos() { return this.theta1Cos; }
    public final Matrix getTheta1Sin() { return this.theta1Sin; }
    public final Matrix getTheta2Cos() { return this.theta2Cos; }
    public final Matrix getTheta2Sin() { return this.theta2Sin; }
    public final Matrix getTheta1W() { return this.theta1W; }
    public final Matrix getTheta2W() { return this.theta2W; }
    public final double[][][][][][][] getQTable() { return this.qTable; }

    public final int getTheta1CosIdx(final double x) { return DiscreteUtils.getIdx(x, this.getTheta1Cos()); }
    public final int getTheta1SinIdx(final double x) { return DiscreteUtils.getIdx(x, this.getTheta1Sin()); }
    public final int getTheta2CosIdx(final double x) { return DiscreteUtils.getIdx(x, this.getTheta2Cos()); }
    public final int getTheta2SinIdx(final double x) { return DiscreteUtils.getIdx(x, this.getTheta2Sin()); }
    public final int getTheta1WIdx(final double x) { return DiscreteUtils.getIdx(x, this.getTheta1W()); }
    public final int getTheta2WIdx(final double x) { return DiscreteUtils.getIdx(x, this.getTheta2W()); }
    public final double[] getQValues(final Matrix state)
    {
        final int theta1CosIdx = this.getTheta1CosIdx(state.get(0, 0));
        final int theta1SinIdx = this.getTheta1SinIdx(state.get(0, 1));
        final int theta2CosIdx = this.getTheta2CosIdx(state.get(0, 2));
        final int theta2SinIdx = this.getTheta2SinIdx(state.get(0, 3));
        final int theta1WIdx = this.getTheta1WIdx(state.get(0, 4));
        final int theta2WIdx = this.getTheta2WIdx(state.get(0, 5));
        return this.getQTable()[theta1CosIdx][theta1SinIdx]
            [theta2CosIdx][theta2SinIdx]
            [theta1WIdx][theta2WIdx];
    }

    public final double getQValue(final Matrix state,
                                  final int action)
    {
        final int theta1CosIdx = this.getTheta1CosIdx(state.get(0, 0));
        final int theta1SinIdx = this.getTheta1SinIdx(state.get(0, 1));
        final int theta2CosIdx = this.getTheta2CosIdx(state.get(0, 2));
        final int theta2SinIdx = this.getTheta2SinIdx(state.get(0, 3));
        final int theta1WIdx = this.getTheta1WIdx(state.get(0, 4));
        final int theta2WIdx = this.getTheta2WIdx(state.get(0, 5));

        // System.out.println("(pos, vel) = (" + position + ", " + velocity + ") => ("
        //     + positionIdx + ", " + velocityIdx + ")");

        return this.getQTable()[theta1CosIdx][theta1SinIdx]
            [theta2CosIdx][theta2SinIdx]
            [theta1WIdx][theta2WIdx]
            [action];
    }

    public final void setQValue(final Matrix state,
                                final int action,
                                final double newQValue)
    {
        final int theta1CosIdx = this.getTheta1CosIdx(state.get(0, 0));
        final int theta1SinIdx = this.getTheta1SinIdx(state.get(0, 1));
        final int theta2CosIdx = this.getTheta2CosIdx(state.get(0, 2));
        final int theta2SinIdx = this.getTheta2SinIdx(state.get(0, 3));
        final int theta1WIdx = this.getTheta1WIdx(state.get(0, 4));
        final int theta2WIdx = this.getTheta2WIdx(state.get(0, 5));
        this.getQTable()[theta1CosIdx][theta1SinIdx]
            [theta2CosIdx][theta2SinIdx]
            [theta1WIdx][theta2WIdx]
            [action] = newQValue;
    }

    private Matrix qTableToMatrix()
    {
        final int t1CosSize = this.getTheta1Cos().getShape().numRows();
        final int t1SinSize = this.getTheta1Sin().getShape().numRows();
        final int t2CosSize = this.getTheta2Cos().getShape().numRows();
        final int t2SinSize = this.getTheta2Sin().getShape().numRows();
        final int t1WSize = this.getTheta1W().getShape().numRows();
        final int t2WSize = this.getTheta2W().getShape().numRows();

        final int numElements = t1CosSize * t1SinSize * t2CosSize * t2SinSize * t1WSize * t2WSize * 3;

        int idx = 0;
        Matrix mat = Matrix.zeros(numElements, 1);
        for(int theta1CosIdx = 0; theta1CosIdx < t1CosSize; ++theta1CosIdx)
        {
            for(int theta1SinIdx = 0; theta1SinIdx < t1SinSize; ++theta1SinIdx)
            {
                for(int theta2CosIdx = 0; theta2CosIdx < t2CosSize; ++theta2CosIdx)
                {
                    for(int theta2SinIdx = 0; theta2SinIdx < t2SinSize; ++theta2SinIdx)
                    {
                        for(int theta1WIdx = 0; theta1WIdx < t1WSize; ++theta1WIdx)
                        {
                            for(int theta2WIdx = 0; theta2WIdx < t2WSize; ++theta2WIdx)
                            {
                                for(int action = 0; action < 3; ++action)
                                {
                                    final double qValue = this.getQTable()[theta1CosIdx][theta1SinIdx]
                                        [theta2CosIdx][theta2SinIdx]
                                        [theta1WIdx][theta2WIdx]
                                        [action];
                                    mat.set(idx, 0, qValue);
                                    idx += 1;
                                }
                            }
                        }
                    }
                }
            }
        }

        return mat;
    }

    private void matrixToQTable(final Matrix mat)
    {
        final int t1CosSize = this.getTheta1Cos().getShape().numRows();
        final int t1SinSize = this.getTheta1Sin().getShape().numRows();
        final int t2CosSize = this.getTheta2Cos().getShape().numRows();
        final int t2SinSize = this.getTheta2Sin().getShape().numRows();
        final int t1WSize = this.getTheta1W().getShape().numRows();
        final int t2WSize = this.getTheta2W().getShape().numRows();

        int idx = 0;
        for(int theta1CosIdx = 0; theta1CosIdx < t1CosSize; ++theta1CosIdx)
        {
            for(int theta1SinIdx = 0; theta1SinIdx < t1SinSize; ++theta1SinIdx)
            {
                for(int theta2CosIdx = 0; theta2CosIdx < t2CosSize; ++theta2CosIdx)
                {
                    for(int theta2SinIdx = 0; theta2SinIdx < t2SinSize; ++theta2SinIdx)
                    {
                        for(int theta1WIdx = 0; theta1WIdx < t1WSize; ++theta1WIdx)
                        {
                            for(int theta2WIdx = 0; theta2WIdx < t2WSize; ++theta2WIdx)
                            {
                                for(int action = 0; action < 3; ++action)
                                {
                                    this.getQTable()[theta1CosIdx][theta1SinIdx]
                                        [theta2CosIdx][theta2SinIdx]
                                        [theta1WIdx][theta2WIdx]
                                        [action] = mat.get(idx, 0);
                                    idx += 1;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public final void update(final Matrix states,
                             final Matrix actions,
                             final Matrix YGt,
                             final double lr) throws Exception
    {
        for(int rowIdx = 0; rowIdx < states.getShape().numRows(); ++rowIdx)
        {
            Matrix state = states.getSlice(rowIdx, rowIdx+1, 0, states.getShape().numCols());
            int action = (int)(actions.get(rowIdx, 0));
            double yGt = YGt.get(rowIdx, 0);

            // update this specific q-value
            this.setQValue(state, action,
                this.getQValue(state, action) + lr*(yGt - this.getQValue(state, action)));
        }
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
        // write all of the params to disk
        List<Matrix> params = new ArrayList<>(7);
        params.add(this.getTheta1Cos());
        params.add(this.getTheta1Sin());
        params.add(this.getTheta2Cos());
        params.add(this.getTheta2Sin());
        params.add(this.getTheta1W());
        params.add(this.getTheta2W());
        params.add(this.qTableToMatrix());

        File path = new File(filePath);
        path.getAbsoluteFile().getParentFile().mkdirs();
        try {
            // open a new file writer. Set append to false
            BufferedWriter writer = new BufferedWriter(new FileWriter(path, false));

            for(Matrix mat : params) {
                writer.write(mat.toStringData() + "\n");
            }
            writer.flush();
            writer.close();
        } catch(IOException ex) {
            System.err.println("Failed to save params to file. Reason: " + ex.getMessage());
        }
    }

    @Override
    public void load(final String filePath) throws Exception
    {
        List<Matrix> params = new ArrayList<>(7);

        File path = new File(filePath);
        if (!path.exists()) {
            System.err.println("Failed to load params. File does not exist");
            return;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line;
            while((line = reader.readLine()) != null) {
                params.add(Matrix.fromStringData(line));
            }
            reader.close();

        } catch(IOException ex) {
            System.err.println("Failed to load params from file. Reason: " + ex.getMessage());
            return;
        }

        // now set the first 6
        this.theta1Cos = params.get(0);
        this.theta1Sin = params.get(1);
        this.theta2Cos = params.get(2);
        this.theta2Sin = params.get(3);
        this.theta1W = params.get(4);
        this.theta2W = params.get(5);
        this.qTable = new double[this.getTheta1Cos().getShape().numRows()]
            [this.getTheta1Sin().getShape().numRows()]
            [this.getTheta2Cos().getShape().numRows()]
            [this.getTheta2Sin().getShape().numRows()]
            [this.getTheta1W().getShape().numRows()]
            [this.getTheta2W().getShape().numRows()]
            [3];
        this.matrixToQTable(params.get(6));
    }
}
