package edu.jhu.coe.classify;

import edu.jhu.coe.util.Counter;


/**
 * A FeatureVector is a collection of features.  This collection may or may not be a
 * list, depending on the implementation.
 */
public interface FeatureVector <F> {
  Counter<F> getFeatures();
}
