package edu.jhu.coe.math;

/**
 * @author Dan Klein
 */
public interface GradientMinimizer {
  double[] minimize(DifferentiableFunction function, double[] initial, double tolerance);
  double[] minimize(DifferentiableFunction function, double[] initial, double tolerance, boolean project);
}
