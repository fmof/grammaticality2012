package edu.jhu.coe.math;

public interface DifferentiableRegularizableFunction extends
		DifferentiableFunction {

	  double[] unregularizedDerivativeAt(double[] x);

}
