package edu.jhu.coe.optimize;

import edu.jhu.coe.math.Function;

public interface FunctionMinimizer {
	public double[] minimize(Function fn, double[] initialX, double tolerance);
}
