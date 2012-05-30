package edu.jhu.coe.syntax;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.lang.String;


import edu.jhu.coe.syntax.SubtreeAnalyzer;
import edu.jhu.coe.syntax.SubtreeAnalyzer.Options;
import edu.jhu.coe.syntax.Trees;
import edu.jhu.coe.util.AbstractFrequencyMap;
import edu.jhu.coe.util.IntegerFrequencyMap;
import edu.jhu.coe.util.IntegerHashtable;
import edu.jhu.coe.PCFGLA.Corpus;
import edu.jhu.coe.PCFGLA.Corpus.TreeBankType;
import edu.jhu.coe.PCFGLA.OptionParser;
import edu.jhu.coe.PCFGLA.Option;
import edu.jhu.coe.util.SparseIntVector;
import edu.jhu.coe.util.UniversalTagMapper;

/**
 * Perform a subtree analysis using a rule criterion. Currently
 * this only counts subtrees, and returns the <code>k</code> most
 * common subtrees using at most <code>r</code> rule expansions
 * ({@see AnalyzeSubtrees}).
 * @author Frank Ferraro
 */

public class SubtreeAnalyzerByRule extends SubtreeAnalyzer{

    private static SubtreeAnalyzerByRule SABR;
    private boolean viewProgress = true;
    private boolean performAnalysisOnLeftOver = true;

    private boolean keepAllDepth1 = false;

    private IntegerFrequencyMap<Tree<Integer>> allDepthOneFragments;

    public SubtreeAnalyzerByRule(edu.jhu.coe.syntax.SubtreeAnalyzer.Options opts){
	super(opts);
	//fragmentFrequency = new IntegerFrequencyMap<Long>(opts.cutoff);
	fragmentFrequency.setSizeThreshold(opts.cutoff);
	if(opts.printincrementally) printIncrementally=true;
	if(opts.alldepthone!=0) {
	    keepAllDepth1 = true;
	    allDepthOneFragments = new IntegerFrequencyMap<Tree<Integer>>();
	}
	performAnalysisOnLeftOver = opts.performLeftOverAnalysis;
	if(performAnalysisOnLeftOver)
	    leftOverFrequency = new LinkedList<IntegerFrequencyMap<Integer>>();
    }

    public AbstractFrequencyMap<Tree<Integer>,Integer> AnalyzeSubtrees(int R){
	return AnalyzeSubtrees(R,false);
    }

    public Collection<Tree<String>> AnalyzeSubtreesAsConstraints(int R){
	//System.err.println("WHAT IS R="+R);
	AbstractFrequencyMap<Tree<Integer>,Integer> result = AnalyzeSubtrees(R,false,true);
	Collection<Tree<String>> retList = new ArrayList<Tree<String>>();
	for(Tree<Integer> tree : result.keySet()){
	    //System.out.println("and we have the tree="+tree);
	    //off-by-one counting in getDepth() method...
	    if(tree.getDepth()>2){
		//System.out.println("keep!");
		retList.add(Trees.integerTreeToStringTree(tree));
	    }
	}
	return retList;
    }

    public AbstractFrequencyMap<Tree<Integer>,Integer> AnalyzeSubtrees(int R, boolean addToFragmentLookup){
	return AnalyzeSubtrees(R,addToFragmentLookup,false);
    }

    //the one to ACTUALLY use
    public AbstractFrequencyMap<Tree<Integer>,Integer> AnalyzeSubtrees(int R, boolean addToFragmentLookup, boolean asConstraintSet){
	//number of rules used = 1
	if(viewProgress && !asConstraintSet) System.err.println("Using 1 rule");
        for (Tree<Integer> tree : getCorpus()){
	    //construct the initial fragment
	    Tree<Integer> tempRootFrag = (new Fragment(tree)).getTree();
	    tempRootFrag.calculateNumberOfExpansions();
	    fragmentFrequency.increment(tempRootFrag);
	    Set<Tree<Integer>> tempQueue1 = fragmentContribution.containsKey(tempRootFrag)?
		fragmentContribution.get(tempRootFrag):(new LinkedHashSet<Tree<Integer>>());
	    tempQueue1.add(tree);
	    fragmentContribution.put(tempRootFrag, tempQueue1);

	    Queue<Tree<Integer>> childQ = new ConcurrentLinkedQueue<Tree<Integer>>();
	    for(Tree<Integer> child : tree.getChildren())
		childQ.add(child);

	    while(!childQ.isEmpty()){
		Tree<Integer> currTree = childQ.poll();
		Tree<Integer> tempFrag = (new Fragment(currTree)).getTree();
		tempFrag.calculateNumberOfExpansions();
		fragmentFrequency.increment(tempFrag);
		Set<Tree<Integer>> tempQueue = fragmentContribution.containsKey(tempFrag)?
		    fragmentContribution.get(tempFrag):(new LinkedHashSet<Tree<Integer>>());
		tempQueue.add(currTree);
		fragmentContribution.put(tempFrag, tempQueue);
		for(Tree<Integer> child : currTree.getChildren()){
		    if(!child.isLeaf())
			childQ.add(child);
		}
	    }
        }
	if(keepAllDepth1){
	    for(Tree<Integer> t : fragmentFrequency.keySet()){
		allDepthOneFragments.incrementBy(t, fragmentFrequency.get(t));
	    }
	    System.out.println(allDepthOneFragments.size());
	}
	if(performAnalysisOnLeftOver){
	    HashMap<Tree<Integer>, Integer> leftOver = fragmentFrequency.reduce(fragmentContribution,true);
	    IntegerFrequencyMap<Integer> ifm = new IntegerFrequencyMap<Integer>();
	    performLeftOverAnalysis(ifm,leftOver);
	    leftOverFrequency.add(ifm);
	    leftOver.clear();
	    leftOver=null;
	} else{
	    fragmentFrequency.reduce(fragmentContribution);
	    if(keepAllDepth1) 
		System.out.println(allDepthOneFragments.size());
	}
	fragmentFrequency.clearLeftOver();
	
	if(printIncrementally){
	    System.out.println("\n\n### OUTPUT FROM AT MOST 1 RULE\n");
	    printFragmentFrequency();
	}
	if(performAnalysisOnLeftOver){
	    leftOverFrequency.get(0).printMainOnly();
	}
	//other cases
	for(int r=2; r<=R; r++){
	    int fragCount=0;
	    if(viewProgress && !asConstraintSet) System.err.println("Expanding at most " + r + " rules");
	    Set<Tree<Integer>> priorFragmentList = new HashSet<Tree<Integer>>(fragmentFrequency.keySet());
	    Map<Tree<Integer>, Set<Tree<Integer>>> alreadyCounted = new HashMap<Tree<Integer>, Set<Tree<Integer>>>();
	    for(Tree<Integer> X : priorFragmentList){
		fragCount++;
		if(fragCount % 1000 == 0 && !asConstraintSet) System.err.print("...");
		//add a check to see if X has used at most r-2 rules
		if(X.getNumberOfExpansions() < r-1)
		    continue;
		Set<Tree<Integer>> set = fragmentContribution.get(X);
		for(Tree<Integer> T : set){
		    List<Tree<Integer>> frontier = Fragment.getFrontierFragments(X,T);
		    //we can't expand down any further
		    if(frontier.isEmpty())
			break;
		    //for every element in the frontier	    
		    for(Tree<Integer> FChild : frontier){
			//avoid double-counting
			Set<Tree<Integer>> alreadyCountedForFC = alreadyCounted.containsKey(FChild)?alreadyCounted.get(FChild):(new HashSet<Tree<Integer>>());
			if(!alreadyCountedForFC.contains(T)){
			    fragmentFrequency.increment(FChild);
			    alreadyCountedForFC.add(T);
			    alreadyCounted.put(FChild,alreadyCountedForFC);
			}
			Set<Tree<Integer>> tempQueue = 
			    fragmentContribution.containsKey(FChild)?
			    fragmentContribution.get(FChild):
			    (new LinkedHashSet<Tree<Integer>>());
			tempQueue.add(T);
			fragmentContribution.put(FChild, tempQueue);
		    }
		    frontier = null;
		}		
	    }

	    if(performAnalysisOnLeftOver){
		IntegerFrequencyMap<Integer> ifm = new IntegerFrequencyMap<Integer>();
		HashMap<Tree<Integer>,Integer> leftOver = fragmentFrequency.reduce(fragmentContribution,true);
		performLeftOverAnalysis(ifm,leftOver);
		leftOverFrequency.add(ifm);
		leftOver=null;
	    } else{
		fragmentFrequency.reduce();
	    }
	    fragmentFrequency.clearLeftOver();

	    
	    //merge the depth 1 with the current list, if we're on the last iteration
	    if(keepAllDepth1 && r==R){
		fragmentFrequency.merge(allDepthOneFragments);
	    }

	    if(!asConstraintSet) System.err.println();
	    
	    if(printIncrementally){
		System.out.println("\n\n### OUTPUT FROM AT MOST " + r + " RULES\n");
		printFragmentFrequency();
	    }
	    if(performAnalysisOnLeftOver){
		leftOverFrequency.get(r-1).printMainOnly();
	    }
	}
	if(printIncrementally)
	    System.out.println("\n========================\n");
       
	if(!printIncrementally && !addToFragmentLookup && !asConstraintSet){
	    System.out.println("\n\n### FINAL OUTPUT\n");
	    printFragmentFrequency();
	}
	if(addToFragmentLookup){
	    fragmentLookup = new IntegerHashtable<Tree<Integer>>();
	    inverseFragmentLookup = new ArrayList<Tree<Integer>>();
	    int i=0;
	    for(Integer key : fragmentFrequency.getInverseMap().keySet()){
		HashSet<Tree<Integer>> set = fragmentFrequency.getInverseMap().get(key);
		for(Tree<Integer> T : set){
		    inverseFragmentLookup.add(new Integer(i), T);
		    fragmentLookup.put(T, new Integer(i));
		    i++;
		}
	    }	
	    
	}

	return fragmentFrequency;
    }

    public void performLeftOverAnalysis(IntegerFrequencyMap<Integer> ifm, HashMap<Tree<Integer>, Integer> map){
	for(Map.Entry<Tree<Integer>,Integer> entry : map.entrySet()){
	    //if(entry.getKey().getNumberOfExpansions()==1)
	    //System.out.println("** removing " + getHumanTree(entry.getKey()) + " :: " + entry.getKey().getNumberOfExpansions() + " ;; " + entry.getValue());
	    ifm.increment(entry.getKey().getNumberOfExpansions());
	}
    }
    

    public static void main(String[] args) throws IOException {
        OptionParser optParser = new OptionParser(Options.class);
        opts = (Options) optParser.parse(args, true);
        System.out.println(optParser.getPassedInOptions());

	SABR = new SubtreeAnalyzerByRule(opts);

	if(opts.extractcounts==1){
	    if(opts.treebank != TreeBankType.WSJ)
		SABR.setCorpus(opts.corpus, opts.treebank);
	    else
		SABR.setCorpus(opts.corpus, opts.first, opts.last);
	    SABR.AnalyzeSubtrees(opts.rulecriterion, opts.testcorpus!=null);
	} else{
	    //we must read in previous counts from somewhere
	    if(opts.gztofragment==null &&
	       opts.texttofragment == null){
		//error, must read in from somewhere
		System.err.println("Please provide either a GZIP or text fragment file, in appropriate format (but not both)");
		System.exit(-1);
	    } else if(opts.gztofragment !=null &&
		      opts.texttofragment != null){
		//error, must read in from somewhere
		System.err.println("Please provide either a GZIP or text fragment file, in appropriate format (but not both)");
		System.exit(-1);
	    } else if(opts.gztofragment!=null){
		//read from gz
		//ZZZ to do!!!
		//SABR.readFragmentFromGZ(opts.gztofragment);
	    } else{
		//read from text file
		SABR.readFragmentFromText(opts.texttofragment);
	    }
	}
	//run on a "test" corpus
	if(opts.testcorpus!=null){
	    //load test corpus
	    List<Tree<Integer>> otherCorpus = SABR.loadCorpusInto(opts.testcorpus,opts.testfirst, opts.testlast);
	    List<SparseIntVector> lst = SABR.extractFeaturesOn(otherCorpus);
	    if(opts.outputtextfeatures==null){
		SABR.printExtractedFeatures(lst);
	    } else{
		SABR.printExtractedFeatures(lst, opts.outputtextfeatures);
	    }
	}
    }
}