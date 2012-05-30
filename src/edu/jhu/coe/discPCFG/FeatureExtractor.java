package edu.jhu.coe.discPCFG;

import edu.jhu.coe.util.Counter;

/**
 * Feature extractors process input instances into feature counters.
 *
 * @author Dan Klein
 */
public interface FeatureExtractor<I,O> {
  Counter<O> extractFeatures(I instance);
}
