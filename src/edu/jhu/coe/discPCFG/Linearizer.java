/**
 * 
 */
package edu.jhu.coe.discPCFG;

import java.util.List;

import edu.jhu.coe.PCFGLA.BinaryRule;
import edu.jhu.coe.PCFGLA.Grammar;
import edu.jhu.coe.PCFGLA.Rule;
import edu.jhu.coe.PCFGLA.SimpleLexicon;
import edu.jhu.coe.PCFGLA.SpanPredictor;
import edu.jhu.coe.PCFGLA.UnaryRule;
import edu.jhu.coe.syntax.StateSet;

/**
 * @author petrov
 *
 */
public interface Linearizer {
	public double[] getLinearizedGrammar();
  public double[] getLinearizedLexicon();
  public double[] getLinearizedSpanPredictor();
	public double[] getLinearizedWeights();
	public void delinearizeWeights(double[] logWeights);
  public Grammar getGrammar();
  public SimpleLexicon getLexicon();
  public SpanPredictor getSpanPredictor();
  public void increment(double[] counts, StateSet stateSet, int tag, double[] weights, boolean isGold);
  public void increment(double[] counts, UnaryRule rule, double[] weights, boolean isGold);
  public void increment(double[] counts, BinaryRule rule, double[] weights, boolean isGold);
  public void increment(double[] counts, List<StateSet> sentence, double[][][] weights, boolean isGold);
  public int dimension();
  public int getNGrammarWeights();
  public int getNLexiconWeights();
  public int getNSpanWeights();
}
