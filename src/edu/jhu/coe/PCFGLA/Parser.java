package edu.jhu.coe.PCFGLA;

import java.util.List;

import edu.jhu.coe.syntax.Tree;

public interface Parser {
  public Tree<String> getBestParse(List<String> sentence);
}

