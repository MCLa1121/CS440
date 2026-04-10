package labs.doppeltree.models;


// SYSTEM IMPORTS
import edu.bu.jmat.Matrix;
import edu.bu.jmat.Pair;
import edu.bu.labs.doppeltree.features.*;
import edu.bu.labs.doppeltree.enums.*;
import edu.bu.labs.doppeltree.models.Model;


// JAVA PROJECT IMPORTS
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class DecisionTreeModel
    extends Model
{
    // an abstract Node type. This is extended to make Interior Nodes and Leaf Nodes
    public static abstract class Node
        extends Object
    {

        // the dataset that was used to construct this node
        private Matrix X;
        private Matrix y_gt;
        private FeatureHeader featureHeader;

        public Node(final Matrix X,
                    final Matrix y_gt,
                    final FeatureHeader featureHeader)
        {
            this.X = X;
            this.y_gt = y_gt;
            this.featureHeader = featureHeader;
        }

        public final Matrix getX() { return this.X; }
        public final Matrix getY() { return this.y_gt; }
        public final FeatureHeader getFeatureHeader() { return this.featureHeader; }

        // a method to get the majority class (i.e. the most popular class) from ground truth.
        public int getMajorityClass(final Matrix X,
                                    final Matrix y_gt)
        {
            Pair<Matrix, Matrix> uniqueYGtAndCounts = y_gt.unique();
            Matrix uniqueYGtVals = uniqueYGtAndCounts.first();
            Matrix counts = uniqueYGtAndCounts.second();

            // find the argmax of the counts
            int rowIdxOfMaxCount = -1;
            double maxCount = Double.NEGATIVE_INFINITY;

            for(int rowIdx = 0; rowIdx < counts.getShape().numRows(); ++rowIdx)
            {
                if(counts.get(rowIdx, 0) > maxCount)
                {
                    rowIdxOfMaxCount = rowIdx;
                    maxCount = counts.get(rowIdx, 0);
                }
            }

            return (int)uniqueYGtVals.get(rowIdxOfMaxCount, 0);
        }

        // an abstract method to predict the class for this example
        public abstract int predict(final Matrix x);

        // an abstract method to get the datasets that each child node should be built from
        public abstract List<Pair<Matrix, Matrix> > getChildData() throws Exception;

    }

    // leaf node type
    public static class LeafNode
        extends Node
    {

        // a leaf node has the class label inside it
        private int predictedClass;

        public LeafNode(final Matrix X,
                        final Matrix y_gt,
                        final FeatureHeader featureHeader)
        {
            super(X, y_gt, featureHeader);
            this.predictedClass = this.getMajorityClass(X, y_gt);
        }

        @Override
        public int predict(final Matrix x)
        {
            // predict the class (an integer)
            return this.predictedClass;
        }

        // leaf nodes have no children
        @Override
        public List<Pair<Matrix, Matrix> > getChildData() throws Exception { return null; }

    }

    // interior node type
    public static class InteriorNode
        extends Node
    {

        // the column index of the feature that this interior node has chosen
        private int             featureIdx;

        // the type (continuous or discrete) of the feature this interior node has chosen
        private FeatureType     featureType;

        // when we're processing a discrete feature, it is possible that even though that discrete feature
        // can take on any value in its domain (for example, like 5 values), the data we have may not contain
        // all of those values in it. Therefore, whenever we want to predict a test point, it is possible
        // that the test point has a discrete value that we haven't seen before. When we encounter such scenarios
        // we should predict the majority class (aka assign an "out-of-bounds" leaf node)
        private int             majorityClass;

        // the values of the feature that identify each child
        // if the feature this node has chosen is discrete, then |splitValues| = |children|
        // if the feature this node has chosen is continuous, then |splitValues| = 1 and |children| = 2
        private List<Double>    splitValues; 
        private List<Node>      children;

        // what features are the children of this node allowed to use?
        // this is different if the feature this node has chosen is discrete or continuous
        private Set<Integer>    childColIdxs;

        public InteriorNode(final Matrix X,
                            final Matrix y_gt,
                            final FeatureHeader featureHeader,
                            final Set<Integer> availableColIdxs)
        {
            super(X, y_gt, featureHeader);
            this.splitValues = new ArrayList<Double>();
            this.children = new ArrayList<Node>();
            this.majorityClass = this.getMajorityClass(X, y_gt);

            // make a deepcopy of the set that is given to us....we need to potentially remove stuff from this
            // so don't use a shallow copy and risk messing up parent nodes (with a shared shallow copy)!
            this.childColIdxs = new HashSet<Integer>(availableColIdxs);

            // quite a lot happens in this method.
            // this method will figure out which feature (amongst all the ones that we are allowed to see)
            // has the "best" quality (as measured by info gain). It will also populate the field 'this.splitValues'
            // with the correct values for that feature.
            // (side note: this is why this method is being called *after* this.splitValues is initialized)
            this.featureIdx = this.pickBestFeature(X, y_gt, availableColIdxs);
            this.featureType = this.getFeatureHeader().getFeature(this.getFeatureIdx()).getFeatureType();

            // once we know what feature this node has, we need to remove that feature from our children
            // if that feature is discrete.
            // we made a deepcopy of the set so we're all good to in-place remove here.
            if(this.getFeatureType().equals(FeatureType.DISCRETE))
            {
                this.getChildColIdxs().remove(this.getFeatureIdx());
            }
        }

        //------------------------ some getters and setters (cause this is java) ------------------------
        public int getFeatureIdx() { return this.featureIdx; }
        public final FeatureType getFeatureType() { return this.featureType; }

        private List<Double> getSplitValues() { return this.splitValues; }
        private List<Node> getChildren() { return this.children; }

        public Set<Integer> getChildColIdxs() { return this.childColIdxs; }
        public int getMajorityClass() { return this.majorityClass; }
        //-----------------------------------------------------------------------------------------------

        // make sure we add children in the correct order when we use this!
        public void addChild(final Node n) { this.getChildren().add(n); }

        // helper method add 
        // compute the entropy of the label column y
        // this tells us how mixed the classes are at the current node
        private double entropy(final Matrix y){
            int n = y.getShape().numRows();

            // no examples means no uncertainty
            if(n == 0){
                return 0.0;
            }

            Pair<Matrix, Matrix> uniqueYGtAndCounts = y.unique();
            Matrix counts = uniqueYGtAndCounts.second();

            double h = 0.0;

            // entropy = - sum over classes of p(class) * log2(p(class))
            for(int row = 0; row < counts.getShape().numRows(); ++row){
                double p = counts.get(row, 0) / n;

                // only include classes that actually appear in this subset
                if(p > 0.0){
                    h -= p * (Math.log(p) / Math.log(2.0));
                }
            }
            return h;
        }

        // build a new matrix that contains only the rows whose indices are listed in rowIdxs
        // use this to fmaintain the X and y datasets for each child after a split
        private Matrix sliceRows(final Matrix src, final List<Integer> rowIdxs){
            Matrix out = Matrix.zeros(rowIdxs.size(), src.getShape().numCols());

            // copy each selected row from src into the output matrix
            for(int r = 0; r < rowIdxs.size(); ++r){
                int srcRow = rowIdxs.get(r);
                // copy every column in that row
                for(int c = 0; c < src.getShape().numCols(); ++c){
                    out.set(r, c, src.get(srcRow, c));
                }
            }

            return out;
}

        // TODO: complete me!
        /*general idea 
        // choose the best feature from the legal features
        // best means the feature with the smallest conditional entropy
        // this is the same as the largest information gain
         */
        private int pickBestFeature(final Matrix X,
                                    final Matrix y_gt,
                                    final Set<Integer> availableColIdxs)
        {
            double bestConditionalEntropy = Double.POSITIVE_INFINITY;
            int bestFeatureIdx = -1;
            Matrix bestSplitMatrix = null;

            // try every legal feature and keep the one with the best split
            for(Integer colIdx : availableColIdxs){
                try{
                    Pair<Double, Matrix> result = this.getConditionalEntropy(X, y_gt, colIdx);
                    double conditionalEntropy = result.first();
                    Matrix splitMatrix = result.second();

                    // smaller conditional entropy means larger information gain
                    if(conditionalEntropy < bestConditionalEntropy){
                        bestConditionalEntropy = conditionalEntropy;
                        bestFeatureIdx = colIdx;
                        bestSplitMatrix = splitMatrix;
                    }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
        // store the split values of the best feature in this node
        // if the feature is discrete, these are the distinct values
        // if the feature is continuous, this will be one threshold
        this.getSplitValues().clear();

        if(bestSplitMatrix != null){
            for(int r = 0; r < bestSplitMatrix.getShape().numRows(); ++r){
                this.getSplitValues().add(bestSplitMatrix.get(r, 0));
            }   
        }

            return bestFeatureIdx;
        }

        // TODO: complete me!
        private Pair<Double, Matrix> getConditionalEntropy(final Matrix X,
                                                           final Matrix y_gt,
                                                           final int colIdx) throws Exception
        {
            Feature feature = this.getFeatureHeader().getFeature(colIdx);
            int n = X.getShape().numRows();

            // case 1: discrete feature
            // create one branch for each distinct value seen in this column
            if(feature.getFeatureType().equals(FeatureType.DISCRETE)){
                List<Double> seenValues = new ArrayList<Double>();

                // collect all distinct values that appear in this feature column
                for(int row = 0; row < X.getShape().numRows(); ++row){
                    double val = X.get(row, colIdx);

                    if(!seenValues.contains(val)){
                        seenValues.add(val);
                        }
                }
                // sort the values so the split order stays consistent
                Collections.sort(seenValues);

                double conditionalEntropy = 0.0;

                // for each discrete value v, compute Pr[f = v] * H(Y | f = v)
                for(double val : seenValues){
                    List<Integer> matchingRows = new ArrayList<Integer>();

                    // collect the training examples whose feature value equals v
                    for(int row = 0; row < X.getShape().numRows(); ++row){
                        if(X.get(row, colIdx) == val){
                            matchingRows.add(row);
                        }
                    }
                // the child label set for this branch
                Matrix childY = this.sliceRows(y_gt, matchingRows);
                // probability that a row goes to this branch
                double prob = ((double)matchingRows.size()) / n;

                conditionalEntropy += prob * this.entropy(childY);
            }

            // store the split values
            // each row in splitMatrix represents one branch value
            Matrix splitMatrix = Matrix.zeros(seenValues.size(), 1);
            for(int i = 0; i < seenValues.size(); ++i){
                splitMatrix.set(i, 0, seenValues.get(i));
            }
            return new Pair<Double, Matrix>(conditionalEntropy, splitMatrix);
        }
        // case 2: continuous feature
        // try candidate thresholds and keep the one with minimum weighted entropy
        List<Double> uniqueVals = new ArrayList<Double>();

        //same logic with case 1:
        // collect all different values in this column
        for(int row = 0; row < X.getShape().numRows(); ++row){
            double val = X.get(row, colIdx);

            if(!uniqueVals.contains(val)){
                uniqueVals.add(val);
        }
    }
        // sort them so we test thresholds in order
        Collections.sort(uniqueVals);

        double bestEntropy = Double.POSITIVE_INFINITY;
        double bestThreshold = uniqueVals.get(0);

        // left child gets <= threshold
        // right child gets > threshold
        for(int i = 0; i < uniqueVals.size() - 1; ++i){
            double threshold = uniqueVals.get(i);

            List<Integer> leftRows = new ArrayList<Integer>();
            List<Integer> rightRows = new ArrayList<Integer>();

            // split the training rows according to the current threshold
            for(int row = 0; row < X.getShape().numRows(); ++row){
                if(X.get(row, colIdx) <= threshold){
                    leftRows.add(row);
                }else{
                    rightRows.add(row);
            }
        }
        //same as caase 1: compute the probability of going left or right
        //build the label sets for the left and right child
        // weighted conditional entropy of this threshold
        double leftProb = ((double)leftRows.size()) / n;
        double rightProb = ((double)rightRows.size()) / n;

        Matrix leftY = this.sliceRows(y_gt, leftRows);
        Matrix rightY = this.sliceRows(y_gt, rightRows);

        double curEntropy = leftProb * this.entropy(leftY) + rightProb * this.entropy(rightY);

        // keep the best split 
        if(curEntropy < bestEntropy){
            bestEntropy = curEntropy;
            bestThreshold = threshold;
        }
    }

    // if all training example has the same feature value,
    // then this continuous feature cannot separate the data (I think.. Need Double check )
    if(uniqueVals.size() == 1){
        bestEntropy = this.entropy(y_gt);
        bestThreshold = uniqueVals.get(0);
    }

    Matrix splitMatrix = Matrix.zeros(1, 1);
    splitMatrix.set(0, 0, bestThreshold);

    return new Pair<Double, Matrix>(bestEntropy, splitMatrix);
}

        // TODO: complete me!
        @Override
        public int predict(final Matrix x)
        {
            // discrete feature:
            // find the child whose split value matches x[featureIdx]
            if(this.getFeatureType().equals(FeatureType.DISCRETE)){
                double val = x.get(0, this.getFeatureIdx());
                // find the child whose split value matches val
                for(int i = 0; i < this.getSplitValues().size(); ++i){
                    if(this.getSplitValues().get(i) == val){
                        return this.getChildren().get(i).predict(x);
                    }
                }

                // if x has a discrete value we never saw during training,
                // use the majority class stored at this node
                return this.getMajorityClass();
            }

            // continuous feature:
            //  compare x against the threshold and go left or right
            double threshold = this.getSplitValues().get(0);
            double val = x.get(0, this.getFeatureIdx());

            if(val <= threshold){
                // left child stores rows with value <= threshold
                return this.getChildren().get(0).predict(x);
            }
            else{
                // right child stores rows with value > threshold
                return this.getChildren().get(1).predict(x);
            }
        }

        // TODO: complete me!
        @Override
        public List<Pair<Matrix, Matrix> > getChildData() throws Exception
        {
            List<Pair<Matrix, Matrix> > childData = new ArrayList<Pair<Matrix, Matrix> >();

            // where the bug ouur.. fix need
            // build the training set for each child of this node
            if(this.getFeatureType().equals(FeatureType.DISCRETE)){
                // one child dataset for each observed discrete feature value
                for(double splitVal : this.getSplitValues()){
                    List<Integer> rows = new ArrayList<Integer>();

                    // collect all rows whose chosen feature equals splitVal
                    for(int row = 0; row < this.getX().getShape().numRows(); ++row){
                        if(this.getX().get(row, this.getFeatureIdx()) == splitVal){
                            rows.add(row);
                        }
                    }
                    // build the child X and child y matrices for this branch
                    Matrix childX = this.sliceRows(this.getX(), rows);
                    Matrix childY = this.sliceRows(this.getY(), rows);

                    // store the dataset pair for this child
                    childData.add(new Pair<Matrix, Matrix>(childX, childY));
                }
            }else{
                // continuous feature always creates exactly two child datasets
                double threshold = this.getSplitValues().get(0);

                List<Integer> leftRows = new ArrayList<Integer>();
                List<Integer> rightRows = new ArrayList<Integer>();

                // rows with feature <= threshold go left, the rest go right
                for(int row = 0; row < this.getX().getShape().numRows(); ++row){
                    if(this.getX().get(row, this.getFeatureIdx()) <= threshold){
                        leftRows.add(row);
                    }
                    else{
                        rightRows.add(row);
                    }
                }

                Matrix leftX = this.sliceRows(this.getX(), leftRows);
                Matrix leftY = this.sliceRows(this.getY(), leftRows);
                Matrix rightX = this.sliceRows(this.getX(), rightRows);
                Matrix rightY = this.sliceRows(this.getY(), rightRows);

                childData.add(new Pair<Matrix, Matrix>(leftX, leftY));
                childData.add(new Pair<Matrix, Matrix>(rightX, rightY));
            }

            return childData;
            }

    }




    private Node root;

    public DecisionTreeModel(final FeatureHeader featureHeader)
    {
        super(featureHeader);
        this.root = null;
    }

    public Node getRoot() { return this.root; }
    private void setRoot(Node n) { this.root = n; }

    // TODO: complete me!
    private Node dfsBuild(Matrix X, Matrix y_gt, Set<Integer> availableColIdxs) throws Exception
    {
        Pair<Matrix, Matrix> uniqueYGtAndCounts = y_gt.unique();
        Matrix uniqueLabels = uniqueYGtAndCounts.first();

        // base case 1:
        // if all labels at this node are the same, this node is pure
        // so we stop splitting and make a leaf
        if(uniqueLabels.getShape().numRows() == 1){
            return new LeafNode(X, y_gt, this.getFeatureHeader());
        }

        /// base case 2:
        // if there are no legal features left, we cannot split anymore
        // so we make a leaf that predicts the majority class
        if(availableColIdxs.isEmpty()){
            return new LeafNode(X, y_gt, this.getFeatureHeader());
        }

        // build an interior node, which will choose its best feature recursively 
        InteriorNode node = new InteriorNode(X, y_gt, this.getFeatureHeader(), availableColIdxs);
        List<Pair<Matrix, Matrix> > childData = node.getChildData();

        // if the chosen feature did not produce a usable split, convert this node to a leaf
        if(childData == null || childData.size() <= 1){
            // stop here and make a leaf instead
            return new LeafNode(X, y_gt, this.getFeatureHeader());
        }

        // recursively build one child subtree for each child dataset returned by the split
        for(Pair<Matrix, Matrix> childPair : childData){
            Matrix childX = childPair.first();
            Matrix childY = childPair.second();

            // guard against bad splits that produce an empty child dataset
            // if that happens, do not keep splitting this node
            if(childX.getShape().numRows() == 0){
                return new LeafNode(X, y_gt, this.getFeatureHeader());
            }

            Node childNode = this.dfsBuild(childX, childY, node.getChildColIdxs());
            node.addChild(childNode);
        }
        return node;
    }

    // did this for you, feel free to change the printouts if you want
    @Override
    public void train(final Matrix trainFeatures,
                      final Matrix trainGroundTruth)
    {
        System.out.println("DecisionTree.fit: X.shape=" + trainFeatures.getShape() +
            " y_gt.shape=" + trainGroundTruth.getShape());
        try
        {
            Set<Integer> allColIdxs = new HashSet<Integer>();
            for(int colIdx = 0; colIdx < trainFeatures.getShape().numCols(); ++colIdx)
            {
                allColIdxs.add(colIdx);
            }
            this.setRoot(this.dfsBuild(trainFeatures, trainGroundTruth, allColIdxs));
        } catch(Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    // did this for you, feel free to change the printouts if you want
    @Override
    public int classify(final Matrix featureVec)
    {
        // class 0 means Human (i.e. not a zombie), class 1 means zombie
        System.out.println("DecisionTree.predict: x=" + featureVec);
        return this.getRoot().predict(featureVec);
    }
}
//test 
//javac -cp "./lib/*;." @doppeltree.srcs
//java -cp "./lib/*;./src" edu.bu.labs.doppeltree.Main labs.doppeltree.models.DecisionTreeModel
