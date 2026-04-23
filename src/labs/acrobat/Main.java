package labs.acrobat;


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
import edu.bu.labs.acrobat.game.Game;
import edu.bu.labs.acrobat.utils.Triple;
import edu.bu.labs.acrobat.agents.QAgent;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Queue;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.*;
import java.awt.*;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;


// JAVA PROJECT IMPORTS
import labs.acrobat.agents.DiscreteAgent;
import labs.acrobat.agents.NeuralAgent;


public class Main
    extends Object
{

    public static QAgent loadBestModel(final String filePath) throws Exception
    {
        return null; // TODO: instantiate your best model, load the file and return it!
    }

    public static void printProgress(final String header,
                                     final long current,
                                     final long total) {
        long percent = (current * 100) / total;
        long barLength = 20; // Number of characters in the bar
        long completedBars = (percent * barLength) / 100;

        StringBuilder bar = new StringBuilder(header + "[");
        for (long i = 0; i < barLength; i++) {
            if (i < completedBars) bar.append("=");
            else if (i == completedBars) bar.append(">");
            else bar.append(" ");
        }
        bar.append("] ").append(percent).append("%\r");
        System.out.print(bar.toString());
    }

    public static Pair<QAgent, Double> train(long numEpisodes,
                                             QAgent agent,
                                             double epsilon,
                                             Namespace ns,
                                             ReplayBuffer rb) throws Exception
    {
        long maxTimeSteps = 1_000_000;

        Random random = new Random();
        Game game = new Game();

        double decayRate = ns.get("epsilonDecayRate");
        double epsilonMin = ns.get("epsilonMin");

        double lr = ns.get("lr");
        double gamma = ns.get("gamma");

        boolean isNeural = rb != null;
        int batchSize = ns.get("miniBatchSize");
        int updatePeriod = ns.get("updateMovePeriod");
        int numUpdates = ns.get("numUpdates");

        int numMoves = 0;
        for(long episodeIdx = 0; episodeIdx < numEpisodes; ++episodeIdx)
        {
            printProgress("Train: ", episodeIdx, numEpisodes);
            Matrix state = game.reset();
            boolean done = false;
            double reward = 0.0;

            int t = 0;
            while(!done && (t < maxTimeSteps))
            {
                int action;
                if(random.nextDouble() < epsilon) { action = random.nextInt(3); }
                else { action = agent.argmax(state); }

                Triple<Matrix, Double, Boolean> triple = game.step(action);
                Matrix nextState = triple.first();
                reward = triple.second();
                done = triple.third();
                if(isNeural){
                    // add every transition to the replay buffer
                    if(done){
                        rb.addSample(state, reward, null);
                    }else{
                        rb.addSample(state, reward, nextState);
                    }
                // Only train every updatePeriod moves.
                // every updatePeriod moves, sample a batch and train numUpdates times
                //so we seperate the isNeural check and adding a new sizw check 
                if((numMoves > 0) && ((numMoves % updatePeriod) == 0)&& (rb.size() > 0))
                {
                    // Ask the replay buffer for:
                    //   batch.first()  = batch of previous states
                    //   batch.second() = Bellman ground-truth targets
                    Pair<Matrix, Matrix> batch = rb.sampleBatch(((NeuralAgent)agent).getQFunction(),gamma,batchSize);

                    Matrix batchStates = batch.first();
                    Matrix batchYGt = batch.second();

                    // NeuralAgent.update requires an actions matrix parameter,
                    // but the current NeuralAgent ignores it, so we can maintain a empty matrix.
                    // Causing bug, instead we create a dummy matrix with the correct number of rows
                    Matrix dummyActions = Matrix.zeros(batchStates.getShape().numRows(), 1);

                    // iterate the number of gradient updates on this same batch.
                    for(int updateIdx = 0; updateIdx < numUpdates; ++updateIdx){
                        agent.update(batchStates,
                        dummyActions,
                        batchYGt,
                        lr);
                    }
                }
            }else{
                //where we will have the discretized agrnt trianing
                 double yGt = reward;
                 // If the episode is not over, 
                 // include the estimate of the best future Q-value.
                 if(!done){
                    yGt = yGt + gamma * agent.max(nextState);
                }
                    // agent.update(state,
                    //              Matrix.full(1, 1, action),
                    //              Matrix.full(1, 1, reward + gamma*agent.max(nextState)),
                    //              lr);
                    agent.update(state,Matrix.full(1, 1, action),Matrix.full(1, 1, yGt),lr);
                }
                state = nextState;
                numMoves += 1;
                t += 1;
            }

            epsilon = Math.max(epsilon - decayRate, epsilonMin);
        }
        System.out.println();
        return new Pair<>(agent, epsilon);
    }

    public static Pair<Double, Double> eval(long numEpisodes,
                                            QAgent agent,
                                            Namespace ns) throws Exception
    {
        long maxTimeSteps = 100_000;

        Random random = new Random();
        Game game = new Game();

        double lr = ns.get("lr");
        double gamma = ns.get("gamma");

        double trajectoryUtilitySum = 0.0;
        double episodeLengthSum = 0.0;

        for(long episodeIdx = 0; episodeIdx < numEpisodes; ++episodeIdx)
        {
            printProgress("Eval:  ", episodeIdx, numEpisodes);
            double trajectoryUtility = 0.0;
            double t = 0.0;

            Matrix state = game.reset();
            boolean done = false;
            double rewards = 0.0;

            while(!done && t < maxTimeSteps)
            {
                int action = agent.argmax(state);

                Triple<Matrix, Double, Boolean> triple = game.step(action);
                Matrix nextState = triple.first();
                double reward = triple.second();
                done = triple.third();

                state = nextState;
                trajectoryUtility += Math.pow(gamma, t) * reward;
                t += 1;
            }

            trajectoryUtilitySum += trajectoryUtility;
            episodeLengthSum += t;
        }
        System.out.println();
        return new Pair<>(trajectoryUtilitySum/numEpisodes, episodeLengthSum/numEpisodes);
    }

    public static void main(String[] args) throws Exception
    {
        ArgumentParser parser = ArgumentParsers.newFor("Main").build()
            .defaultHelp(true)
            .description("Play openai-gym Acrobat (double pendulumn) in Java");

        parser.addArgument("type")
            .choices("DISCRETIZED", "NEURAL")
            .help("The type of q-function you want to use");

        parser.addArgument("-p", "--numCycles")
            .type(long.class)
            .setDefault(1L)
            .help("the number of times the training/testing cycle is repeated");
        parser.addArgument("-t", "--numTrainingGames")
            .type(long.class)
            .setDefault(10L)
            .help("the number of training games to collect training data from before an evaluation phase");
        parser.addArgument("-v", "--numEvalGames")
            .type(long.class)
            .setDefault(5L)
            .help("the number of evaluation games to play while fixing the agent " +
                  "(the agent can't learn from these games)");

        parser.addArgument("-n", "--lr")
            .type(double.class)
            .setDefault(0.1)
            .help("the learning rate to use.");
        parser.addArgument("-g", "--gamma")
            .type(double.class)
            .setDefault(0.9)
            .help("discount factor for the Bellman equation.");
        parser.addArgument("-d", "--epsilonDecayRate")
            .type(double.class)
            .setDefault(0.0005)
            .help("epsilon decay rate for epsilon-greedy exploration");
        parser.addArgument("-l", "--epsilonMin")
            .type(double.class)
            .setDefault(0.05)
            .help("minimum value of epsilon-greedy parameter");

        parser.addArgument("-m", "--miniBatchSize")
            .type(int.class)
            .setDefault(128)
            .help("batch size to use when performing an epoch of training (only used if NeuralAgent is selected).");
        parser.addArgument("-u", "--numUpdates")
            .type(int.class)
            .setDefault(1)
            .help("number of epochs to update the neural model with (only used if NeuralAgent is selected).");
        parser.addArgument("-r", "--updateMovePeriod")
            .type(int.class)
            .setDefault(100)
            .help("number of moves in between updating the NeuralAgent (only used if NeuralAgent is selected)");

        // replay buffer config
        parser.addArgument("-b", "--maxBufferSize")
            .type(int.class)
            .setDefault(1280)
            .help("The max number of samples to store in the replay buffer (only used if NeuralAgent is selected).");

        parser.addArgument("-e", "--replacementType")
            .type(ReplayBuffer.ReplacementType.class)
            .setDefault(ReplayBuffer.ReplacementType.RANDOM)
            .help("replay buffer replacement type for when a new sample is added to a full buffer");

        parser.addArgument("-x", "--discretizeSteps")
            .type(int.class)
            .setDefault(15)
            .help("If using the DiscreteAgent, the amount to discretize states with");

        // model saving/loading config
        parser.addArgument("-i", "--inFile")
            .type(String.class)
            .setDefault("")
            .help("params file to load");
        parser.addArgument("-o", "--outFile")
            .type(String.class)
            .setDefault("./params/qFunction")
            .help("where to save the model to (will append XX.model where XX is the number of training/eval " +
                  "cycles performed). Only used if NeuralAgent is selected.");
        parser.addArgument("--outOffset")
            .type(Long.class)
            .setDefault(0l)
            .help("offset to XX value appended to end of --outFile arg. Useful if you want to resume training from " +
                  "a previous training point and don't want to overwrite any subsequent files. (XX + offset) will " +
                  "be used instead of (XX) when appending to the --outFile arg. Only used if NeuralAgent is selected.");

        Namespace ns = parser.parseArgsOrFail(args);

        long numCycles = ns.get("numCycles");
        long numTrainingGames = ns.get("numTrainingGames");
        long numEvalGames = ns.get("numEvalGames");
        String checkpointFileBase = ns.get("outFile");
        String inFile = ns.get("inFile");
        long offset = ns.get("outOffset");

        // allocate different child classes depending on type of model. Only allocate a replay buffer if neural
        boolean isNeural = ns.get("type").equals("NEURAL");
        QAgent agent = isNeural
            ? new NeuralAgent(ns)
            : new DiscreteAgent(ns);
        ReplayBuffer rb = isNeural
            ? new ReplayBuffer(ns.get("replacementType"), ns.get("maxBufferSize"), 6, new Random())
            : null;

        if(!inFile.equals(""))
        {
            agent.load(inFile);
        }

        double epsilon = 1.0; // this will decay over time
        for(long cycleIdx = 0; cycleIdx < numCycles; ++cycleIdx)
        {
            Pair<QAgent, Double> out = train(numTrainingGames, agent, epsilon, ns, rb);
            epsilon = out.second(); // new value of epsilon (in case it decayed during training)

            // save the model
            if(numTrainingGames > 0)
            {
                agent.save(checkpointFileBase + (cycleIdx + offset) + ".model");
            }

            Pair<Double, Double> results = eval(numEvalGames, agent, ns);
            System.out.println("After " + cycleIdx + "/" + numCycles + " cycles: avg(trajectory_utility)= "
                + results.first() + " avg(game_length)=" + results.second());
        }
    }
    //testing:java -cp ".\lib\*;.\src;." labs.acrobat.Main NEURAL -p 3 -t 50 -v 10 -g 0.99 -n 0.01 -u 1 -b 500 -m 64 -r 50
}
