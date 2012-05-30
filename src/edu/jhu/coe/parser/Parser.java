package edu.jhu.coe.parser;

import java.util.List;

import edu.jhu.coe.syntax.Tree;

public interface Parser
{
	Tree<String> getBestParse(List<String> sentence);
}
