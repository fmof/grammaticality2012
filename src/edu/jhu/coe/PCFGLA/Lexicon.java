/**
 * 
 */
package edu.jhu.coe.PCFGLA;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.jhu.coe.PCFGLA.smoothing.Smoother;
import edu.jhu.coe.syntax.BerkeleyCompatibleFragment;
import edu.jhu.coe.syntax.StateSet;
import edu.jhu.coe.syntax.Tree;

import fig.basic.Pair;

/**
 * @author petrov
 *
 */
public interface Lexicon {
    public void optimize();
    public double[] score(String word, short tag, int loc, boolean noSmoothing, boolean isSignature);
    public double[] score(StateSet stateSet, short tag, boolean noSmoothing, boolean isSignature);
    public double[] scoreWord(StateSet stateSet, int tag);
    public double[] scoreSignature(StateSet stateSet, int tag);
    public String getSignature(String word, int loc);
    public void logarithmMode();
    public boolean isLogarithmMode();
    public void trainTree(Tree<StateSet> trainTree, double randomness, Lexicon oldLexicon, boolean secondHalf, boolean noSmoothing); 
    public void setSmoother(Smoother smoother);
    public Lexicon splitAllStates(short[] numNonInternalBeforeLatestCouple, int[] offset, int[] counts, boolean moreSubstatesThanCounts, int mode);
    public Lexicon propagateHypothesizedCouplings(LatentStatistics LS, Set</*Map<Pair<Integer,Integer>, */BerkeleyCompatibleFragment> easyPreterminalCouplingMap, short[] numSigsPerTag, double gamma);
    public void mergeStates(boolean[][][] mergeThesePairs, double[][] mergeWeights);
    public Smoother getSmoother();
    public double[] getSmoothingParams();
    public Lexicon projectLexicon(double[] condProbs, int[][] mapping, int[][] toSubstateMapping);
    public Lexicon copyLexicon();
    public void removeUnlikelyTags(double threshold, double exponent);
    public double getPruningThreshold();
    public void writeMJStyle(BufferedWriter writer) throws Exception;
    public void explicitlyComputeScores(int finalLevel);
}
