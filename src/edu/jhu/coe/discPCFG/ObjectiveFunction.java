/**
 * 
 */
package edu.jhu.coe.discPCFG;

import edu.jhu.coe.math.DifferentiableRegularizableFunction;

/**
 * @author petrov
 *
 */
public interface ObjectiveFunction extends DifferentiableRegularizableFunction {
	<F,L> double[] getLogProbabilities(EncodedDatum datum, double[] weights, Encoding<F, L> encoding, IndexLinearizer indexLinearizer);
	//Pair<Double, double[]> calculate();
	public void shutdown();
	  

}
