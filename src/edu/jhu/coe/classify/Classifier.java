package edu.jhu.coe.classify;

/**
 * Classifiers assign labels to instances.
 *
 * @author Dan Klein
 */
public interface Classifier<I,L> {
  L getLabel(I instance);
}
