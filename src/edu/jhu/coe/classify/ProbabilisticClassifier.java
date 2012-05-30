package edu.jhu.coe.classify;

import edu.jhu.coe.util.Counter;

/**
 * Probabilistic classifiers assign distributions over labels to instances.
 *
 * @author Dan Klein
 */
public interface ProbabilisticClassifier<I,L> extends Classifier<I,L> {
  Counter<L> getProbabilities(I instance);
}
