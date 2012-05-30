package edu.jhu.coe.dep;

import java.util.Collection;

import edu.jhu.coe.syntax.Tree;

public interface TrainedDependencyScorer extends DependencyScorer {

	public void train(Collection<Tree<String>> trees);

}
