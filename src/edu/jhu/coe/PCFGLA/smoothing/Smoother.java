/**
 * 
 */
package edu.jhu.coe.PCFGLA.smoothing;

import java.io.Serializable;

import edu.jhu.coe.PCFGLA.BinaryCounterTable;
import edu.jhu.coe.PCFGLA.UnaryCounterTable;

/**
 * @author leon
 *
 */
public interface Smoother {
	public void smooth(UnaryCounterTable unaryCounter, BinaryCounterTable binaryCounter);
	public void smooth(short tag, double[] ruleScores);
	public void updateWeights(int[][] toSubstateMapping);
	public Smoother copy();
}
