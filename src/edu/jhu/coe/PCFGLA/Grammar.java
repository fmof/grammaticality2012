package edu.jhu.coe.PCFGLA;

import edu.jhu.coe.PCFGLA.smoothing.*;
import edu.jhu.coe.math.SloppyMath;
import edu.jhu.coe.syntax.BerkeleyCompatibleFragment;
import edu.jhu.coe.syntax.ProductionTuple;
import edu.jhu.coe.syntax.StateSet;
import edu.jhu.coe.syntax.TagSigOrWord;
import edu.jhu.coe.syntax.Tree;
import edu.jhu.coe.util.*;
import edu.jhu.coe.util.PriorityQueue;

import fig.basic.Pair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;

/**
 * Simple implementation of a PCFG grammar, offering the ability to look up
 * rules by their child symbols. Rule probability estimates are just relative
 * frequency estimates off of training trees.
 */
public class Grammar implements java.io.Serializable {
	
    /**
     * @author leon
     *
     */
    public static enum RandomInitializationType {
	INITIALIZE_WITH_SMALL_RANDOMIZATION,
	    INITIALIZE_LIKE_MMT //initialize like in the Matzuyaki, Miyao, and Tsujii paper 
	    }
	
    public static class RuleNotFoundException extends Exception {
	private static final long serialVersionUID = 2L;
    }
	
    public int finalLevel;

    public boolean[] isGrammarTag;
    public boolean useEntropicPrior = false;
	
    private List<BinaryRule>[] binaryRulesWithParent;
    private List<BinaryRule>[] binaryRulesWithLC;
    private List<BinaryRule>[] binaryRulesWithRC;
    private BinaryRule[][] splitRulesWithLC;
    private BinaryRule[][] splitRulesWithRC;
    private BinaryRule[][] splitRulesWithP;
    public List<UnaryRule>[] unaryRulesWithParent;
    public List<UnaryRule>[] unaryRulesWithC;
    private List<UnaryRule>[] sumProductClosedUnaryRulesWithParent;
	
    /** the number of states */
    public short numStates;
	
    /** the number of substates per state */
    public short[] numSubStates;

	
    public Map<BinaryRule, BinaryRule> binaryRuleMap;
    BinaryRule bSearchRule;
    public Map<UnaryRule, UnaryRule> unaryRuleMap;
    UnaryRule uSearchRule;
	
    UnaryCounterTable prevUnaryRuleCounter = null;
    BinaryCounterTable prevBinaryRuleCounter = null;
    UnaryCounterTable unaryRuleCounter = null;
    BinaryCounterTable binaryRuleCounter = null;
	
    CounterMap<Integer, Integer> symbolCounter = new CounterMap<Integer, Integer>();
    CounterMap<Integer, Integer> unnormalizedSymbolCounter = new CounterMap<Integer, Integer>();
	
    private static final long serialVersionUID = 1L;
	
    protected Numberer tagNumberer;
	
    public List<UnaryRule>[] closedSumRulesWithParent = null;
    public List<UnaryRule>[] closedSumRulesWithChild = null;
	
    public List<UnaryRule>[] closedViterbiRulesWithParent = null;
    public List<UnaryRule>[] closedViterbiRulesWithChild = null;
	
    public UnaryRule[][] closedSumRulesWithP = null;
    public UnaryRule[][] closedSumRulesWithC = null;
	
    public UnaryRule[][] closedViterbiRulesWithP = null;
    public UnaryRule[][] closedViterbiRulesWithC = null;
	
    private Map bestSumRulesUnderMax = null;
    private Map bestViterbiRulesUnderMax = null;
    public double threshold;
	
    public Smoother smoother = null;
	
    /** A policy giving what state to go to next, starting from a
     * given state, going to a given state.
     * This array is indexed by the start state, the end state,
     * the start substate, and the end substate.
     */
    private int [][] closedViterbiPaths = null;
    private int [][] closedSumPaths=null;
	
    public boolean findClosedPaths;
	
    /** If we are in logarithm mode, then this grammar's scores are all
     * given as logarithms.  The default is to have a score plus a scale factor.
     */
    boolean logarithmMode;
	
    public Tree<Short>[] splitTrees;
		
    public void clearUnaryIntermediates(){
	ArrayUtil.fill(closedSumPaths,0);
	ArrayUtil.fill(closedViterbiPaths, 0);
    }
	
	
    public void addBinary(BinaryRule br) {
	//System.out.println("BG adding rule " + br);
	binaryRulesWithParent[br.parentState].add(br);
	binaryRulesWithLC[br.leftChildState].add(br);
	binaryRulesWithRC[br.rightChildState].add(br);
	//allRules.add(br);
	binaryRuleMap.put(br, br);
    }
	
    public void addUnary(UnaryRule ur) {
	// System.out.println(" UG adding rule " + ur);
	//closeRulesUnderMax(ur);
	if (!unaryRulesWithParent[ur.parentState].contains(ur)) {
	    unaryRulesWithParent[ur.parentState].add(ur);
	    unaryRulesWithC[ur.childState].add(ur);
	    //allRules.add(ur);
	    unaryRuleMap.put(ur, ur);
	}
    }


    @SuppressWarnings("unchecked")
	public List<BinaryRule> getBinaryRulesByParent(int state) {
	if (state >= binaryRulesWithParent.length) {
	    return Collections.EMPTY_LIST;
	}
	return binaryRulesWithParent[state];
    }
	
    @SuppressWarnings("unchecked")
	public List<UnaryRule> getUnaryRulesByParent(int state) {
	if (state >= unaryRulesWithParent.length) {
	    return Collections.EMPTY_LIST;
	}
	return unaryRulesWithParent[state];
    }
  
    @SuppressWarnings("unchecked")
	public List<UnaryRule>[] getSumProductClosedUnaryRulesByParent() {
	return sumProductClosedUnaryRulesWithParent;
    }
  
    @SuppressWarnings("unchecked")
	public List<BinaryRule> getBinaryRulesByLeftChild(int state) {
	//		System.out.println("getBinaryRulesByLeftChild not supported anymore.");
	//		return null;
	if (state >= binaryRulesWithLC.length) {
	    return Collections.EMPTY_LIST;
	}
	return binaryRulesWithLC[state];
    }
	
    @SuppressWarnings("unchecked")
	public List<BinaryRule> getBinaryRulesByRightChild(int state) {
	//		System.out.println("getBinaryRulesByRightChild not supported anymore.");
	//		return null;
	if (state >= binaryRulesWithRC.length) {
	    return Collections.EMPTY_LIST;
	}
	return binaryRulesWithRC[state];
    }
	
    @SuppressWarnings("unchecked")
	public List<UnaryRule> getUnaryRulesByChild(int state) {
	//		System.out.println("getUnaryRulesByChild not supported anymore.");
	//		return null;
	if (state >= unaryRulesWithC.length) {
	    return Collections.EMPTY_LIST;
	}
	return unaryRulesWithC[state];
    }
	
    public String toString_old() {
	/*StringBuilder sb = new StringBuilder();
	  List<String> ruleStrings = new ArrayList<String>();
	  for (int state = 0; state < numStates; state++) {
	  List<BinaryRule> leftRules = getBinaryRulesByLeftChild(state);
	  for (BinaryRule r : leftRules) {
	  ruleStrings.add(r.toString());
	  }
	  }
	  for (int state = 0; state < numStates; state++) {
	  UnaryRule[] unaries = getClosedViterbiUnaryRulesByChild(state);
	  for (int r = 0; r < unaries.length; r++) {
	  UnaryRule ur = unaries[r];
	  ruleStrings.add(ur.toString());
	  }
	  }
	  for (String ruleString : CollectionUtils.sort(ruleStrings)) {
	  sb.append(ruleString);
	  sb.append("\n");
	  }*/
	return null;//sb.toString();
    }

    public void writeData(Writer w) throws IOException {
  	finalLevel = (short)(Math.log(numSubStates[1])/Math.log(2));
  	PrintWriter out = new PrintWriter(w);
	for (int state = 0; state < numStates; state++) {
	    BinaryRule[] parentRules = this.splitRulesWithP(state);
	    for (int i = 0; i < parentRules.length; i++) {
		BinaryRule r = parentRules[i];
		out.print(r.toString());
	    }
	}
	for (int state = 0; state < numStates; state++) {
	    UnaryRule[] unaries = this.getClosedViterbiUnaryRulesByParent(state);
	    for (int r = 0; r < unaries.length; r++) {
		UnaryRule ur = unaries[r];
		out.print(ur.toString());
	    }
	}
	out.flush();
    }

    
    public void writeMJStyle(BufferedWriter writer) throws Exception{
	StringBuilder sb = new StringBuilder();
	List<String> ruleStrings = new ArrayList<String>();
	
	for(UnaryRule ur : unaryRuleMap.keySet()){
	    ruleStrings.add(ur.toStringMJ());
	}
	for(BinaryRule br : binaryRuleMap.keySet()){
	    ruleStrings.add(br.toStringMJ());
	}
	for (String ruleString : CollectionUtils.sort(ruleStrings)) {
	    sb.append(ruleString);
	}
	writer.write(sb.toString()+"\n");
    }


    public void writeFragments(BufferedWriter writer) throws Exception{
	for (UnaryRule oldRule : unaryRuleMap.keySet()) {
	    writeFragmentsFromRule(writer,oldRule);
	}
	for (BinaryRule oldRule : binaryRuleMap.keySet()) {
	    writeFragmentsFromRule(writer,oldRule);
	}

    }

    private void writeFragmentsFromRule(BufferedWriter writer, UnaryRule rule) throws Exception{
	double[][] oldScores =  rule.getScores2();
	short parentState = rule.getParentState();
	short childState = rule.getChildState();

	for (short cS = 0; cS < oldScores.length; cS++) {
	    if (oldScores[cS]==null)
		continue;	 
	    for (short pS = 0; pS < oldScores[cS].length; pS++) {
		double score = oldScores[cS][pS];
		if(score <= threshold) continue;
		if(InternalNodeSet.isSubstateInternal(parentState,pS)) {
		    continue;
		}		    
		BerkeleyCompatibleFragment bcf = new BerkeleyCompatibleFragment();
		ProductionTuple pt = new ProductionTuple(true);
		pt.addNT(0,parentState,pS, 0);
		pt.addNT(1,childState, cS, 1);
		String goalStr = tagNumberer.object(parentState)+"_"+pS;
		Tree<String> st = new Tree<String>(goalStr);
		Tree<TagSigOrWord> stT = constructInitialBCF(pt.toTree(),st);
		writer.write(score + " " + st+"\n");
		//System.out.println("printing out fragment "+st);
	    }	    
	}
    }

    private void writeFragmentsFromRule(BufferedWriter writer, BinaryRule rule) throws Exception{
	double[][][] oldScores =  rule.getScores2();
	short parentState = rule.getParentState();
	short leftChildState = rule.getLeftChildState();
	short rightChildState = rule.getRightChildState();

	for (short lcS = 0; lcS < oldScores.length; lcS++) {
	    for (short rcS = 0; rcS < oldScores[lcS].length; rcS++) {
		if (oldScores[lcS][rcS]==null)
		    continue;
		 
		for (short pS = 0; pS < oldScores[lcS][rcS].length; pS++) {
		    double score = oldScores[lcS][rcS][pS];
		    if(score <= threshold) continue;
		    ProductionTuple pt = new ProductionTuple(false);
		    pt.addNT(0,parentState,pS, 0);
		    pt.addNT(1,leftChildState, lcS, 1);
		    pt.addNT(2,rightChildState, rcS, 2);
		    if(InternalNodeSet.isSubstateInternal(parentState,pS)) {
			continue;
		    }		    
		    String goalStr = tagNumberer.object(parentState)+"_"+pS;
		    Tree<String> st = new Tree<String>(goalStr);
		    Tree<TagSigOrWord> stT = constructInitialBCF(pt.toTree(),st);
		    writer.write(score + " " + st+"\n");
		}	    
	    }
	}
    }
	
    public String toString() {
	//splitRules();
	StringBuilder sb = new StringBuilder();
	List<String> ruleStrings = new ArrayList<String>();
	for (int state = 0; state < numStates; state++) {
	    BinaryRule[] parentRules = splitRulesWithP(state);
	    for (int i = 0; i < parentRules.length; i++) {
		BinaryRule r = parentRules[i];
		ruleStrings.add(r.toString());
	    }
	}
	for (int state = 0; state < numStates; state++) {
	    UnaryRule[] unaries = getClosedSumUnaryRulesByParent(state);
	    for (int r = 0; r < unaries.length; r++) {
		UnaryRule ur = unaries[r];
	
		ruleStrings.add(ur.toString());
	    }
	}
	for (String ruleString : CollectionUtils.sort(ruleStrings)) {
	    sb.append(ruleString);
	}
	return sb.toString();
    }

    public int getNumberOfRules() {
	int nRules = 0;
	for (int state = 0; state < numStates; state++) {
	    BinaryRule[] parentRules = this.splitRulesWithP(state);
	    for (int i = 0; i < parentRules.length; i++) {
		BinaryRule bRule = parentRules[i];
		double[][][] scores = bRule.getScores2();
		for (int j=0; j<scores.length; j++){
		    for (int k=0; k<scores[j].length; k++){
			if (scores[j][k]!=null){	
			    nRules += scores[j][k].length;
			}
		    }
		}
	    }
	    UnaryRule[] unaries = this.getClosedSumUnaryRulesByParent(state);
	    for (int r = 0; r < unaries.length; r++) {
		UnaryRule uRule = unaries[r];
		//			List<UnaryRule> unaries = this.getUnaryRulesByParent(state);
		//			for (UnaryRule uRule : unaries){
		if (uRule.childState==uRule.parentState) continue;
		double[][] scores = uRule.getScores2();
		for (int j=0; j<scores.length; j++){
		    if (scores[j]!=null){	
			nRules += scores[j].length;
		    }
		}
	    }
	}
	return nRules;
    }


    public void printUnaryRules(){
	//System.out.println("BY PARENT");
	for (int state1 = 0; state1 < numStates; state1++) {
	    List<UnaryRule> unaries = this.getUnaryRulesByParent(state1);
	    for (UnaryRule uRule : unaries){
		UnaryRule uRule2 = (UnaryRule)unaryRuleMap.get(uRule);
		if (!uRule.getScores2().equals(uRule2.getScores2()))
		    System.out.print("BY PARENT:\n" +uRule + "" + uRule2+ "\n");
	    }
	}
	//System.out.println("VITERBI CLOSED");
	for (int state1 = 0; state1 < numStates; state1++) {
	    UnaryRule[] unaries = this.getClosedViterbiUnaryRulesByParent(state1);
	    for (int r = 0; r < unaries.length; r++) {
		UnaryRule uRule = unaries[r];
		//System.out.print(uRule);
		UnaryRule uRule2 = (UnaryRule)unaryRuleMap.get(uRule);
		if (unariesAreNotEqual(uRule,uRule2))
		    System.out.print("VITERBI CLOSED:\n" + uRule + "" + uRule2+ "\n");
	    }
	}
		
	/*System.out.println("FROM RULE MAP");
	  for (UnaryRule uRule : unaryRuleMap.keySet()){
	  System.out.print(uRule);
	  }*/
		
	//System.out.println("AND NOW THE BINARIES");
	//System.out.println("BY PARENT");
	for (int state1 = 0; state1 < numStates; state1++) {
	    BinaryRule[] parentRules = this.splitRulesWithP(state1);
	    for (int i = 0; i < parentRules.length; i++) {
		BinaryRule bRule = parentRules[i];
		BinaryRule bRule2 = (BinaryRule)binaryRuleMap.get(bRule);
		if (!bRule.getScores2().equals(bRule2.getScores2()))
		    System.out.print("BINARY: "+bRule + "" + bRule2 + "\n");
	    }
	}
	/*
	  System.out.println("FROM RULE MAP");
	  for (BinaryRule bRule : binaryRuleMap.keySet()){
	  System.out.print(bRule);
	  }*/
		
		
    }
	
    public boolean unariesAreNotEqual(UnaryRule u1, UnaryRule u2){
	// two cases:
	// 1. u2 is null and u1 is a selfRule
	if (u2==null){
	    return false;
	    /*double[][] s1 = u1.getScores2();
	      for (int i=0; i<s1.length; i++){
	      if (s1[i][i] != 1.0) return true;
	      }*/
	}
	else { // compare all entries
	    double[][] s1 = u1.getScores2();
	    double[][] s2 = u2.getScores2();
	    for (int i=0; i<s1.length; i++){
		if (s1[i] == null || s2[i] == null) continue;
		for (int j=0; j<s1[i].length; j++){
		    if (s1[i][j] != s2[i][j]) return true;
		}
	    }
	}
	return false;
    }
	
	

    @SuppressWarnings("unchecked")
	public void init() {
	binaryRuleMap = new HashMap<BinaryRule, BinaryRule>();
	unaryRuleMap = new HashMap<UnaryRule, UnaryRule>();
	//allRules = new ArrayList<Rule>();
	bestSumRulesUnderMax = new HashMap();
	bestViterbiRulesUnderMax = new HashMap();
	binaryRulesWithParent = new List[numStates];
	binaryRulesWithLC = new List[numStates];
	binaryRulesWithRC = new List[numStates];
	unaryRulesWithParent = new List[numStates];
	unaryRulesWithC = new List[numStates];
	closedSumRulesWithParent = new List[numStates];
	closedSumRulesWithChild = new List[numStates];
	closedViterbiRulesWithParent = new List[numStates];
	closedViterbiRulesWithChild = new List[numStates];
	isGrammarTag = new boolean[numStates];
		
	//if (findClosedPaths) {
	closedViterbiPaths = new int[numStates][numStates];
	//}
	closedSumPaths = new int[numStates][numStates];
	
	//System.out.println("in init, numStates is : " + numStates);
	for (short s = 0; s < numStates; s++) {
	    binaryRulesWithParent[s] = new ArrayList<BinaryRule>();
	    binaryRulesWithLC[s] = new ArrayList<BinaryRule>();
	    binaryRulesWithRC[s] = new ArrayList<BinaryRule>();
	    unaryRulesWithParent[s] = new ArrayList<UnaryRule>();
	    unaryRulesWithC[s] = new ArrayList<UnaryRule>();
	    closedSumRulesWithParent[s] = new ArrayList<UnaryRule>();
	    closedSumRulesWithChild[s] = new ArrayList<UnaryRule>();
	    closedViterbiRulesWithParent[s] = new ArrayList<UnaryRule>();
	    closedViterbiRulesWithChild[s] = new ArrayList<UnaryRule>();
			
	    double[][] scores = new double[numSubStates[s]][numSubStates[s]];
	    for (int i=0; i<scores.length; i++) {
		scores[i][i] = 1;
	    }
	    UnaryRule selfR = new UnaryRule(s, s, scores);
	    //relaxSumRule(selfR);
	    relaxViterbiRule(selfR);
	}
    }
		
    /**
     * Rather than calling some all-in-one constructor that takes a list of trees
     * as training data, you call Grammar() to create an empty grammar, call
     * tallyTree() repeatedly to include all the training data, then call
     * optimize() to take it into account.
     * 
     * @param oldGrammar
     *          This is the previous grammar. We use this to copy the split trees
     *          that record how each state is split recursively. These parameters
     *          are intialized if oldGrammar is null.
     */
    @SuppressWarnings("unchecked")
	public Grammar(short[] nSubStates, boolean findClosedPaths,
		       Smoother smoother, Grammar oldGrammar, double thresh) {
	this.tagNumberer = Numberer.getGlobalNumberer("tags");
	this.findClosedPaths = findClosedPaths;
	this.smoother = smoother;
	this.threshold = thresh;
	unaryRuleCounter = new UnaryCounterTable(nSubStates);
	binaryRuleCounter = new BinaryCounterTable(nSubStates);
	symbolCounter = new CounterMap<Integer, Integer>();
	numStates = (short)tagNumberer.total();
	numSubStates = nSubStates;
	bSearchRule = new BinaryRule((short)0,(short)0,(short)0);
	uSearchRule = new UnaryRule((short)0,(short)0);
	logarithmMode = false;
	if (oldGrammar!=null) {
	    splitTrees = oldGrammar.splitTrees;
	} else {
	    splitTrees = new Tree[numStates];
	    boolean hasAnySplits = false;
	    for (int tag=0; !hasAnySplits && tag<numStates; tag++) {
		hasAnySplits = hasAnySplits || numSubStates[tag]>1;
	    }
	    for (int tag=0; tag<numStates; tag++) {
		ArrayList<Tree<Short>> children = new ArrayList<Tree<Short>>(numSubStates[tag]);
		if (hasAnySplits) {
		    for (short substate=0; substate<numSubStates[tag]; substate++) {
			children.add(substate,new Tree<Short>(substate));
		    }
		}
		splitTrees[tag] = new Tree<Short>((short)0,children);
	    }
	}
	init();
    }

    public void setSmoother(Smoother smoother){
	this.smoother = smoother;
    }
	
    public static double generateMMTRandomNumber(Random r) {
	double f = r.nextDouble();
	f = f*2 - 1;
	f = f*Math.log(3);
	return Math.exp(f);
    }
	
    public void optimize(double randomness) {
	//System.out.print("Optimizing Grammar...");
	init();
	//checkNumberOfSubstates();
	if (randomness > 0.0) {
	    Random random = GrammarTrainer.RANDOM;
	    // add randomness
	    for (UnaryRule unaryRule : unaryRuleCounter.keySet()) {
		//System.out.println("optimizing " + unaryRule);
		double[][] unaryCounts = unaryRuleCounter.getCount(unaryRule);
		for (int i = 0; i < unaryCounts.length; i++) {
		    if (unaryCounts[i]==null)
			unaryCounts[i] = new double[numSubStates[unaryRule.getParentState()]];
		    for (int j = 0; j < unaryCounts[i].length; j++) {
			double r = random.nextDouble() * randomness;
			unaryCounts[i][j] += r;
		    }
		}
		unaryRuleCounter.setCount(unaryRule, unaryCounts);
	    }
	    for (BinaryRule binaryRule : binaryRuleCounter.keySet()) {
		double[][][] binaryCounts = binaryRuleCounter.getCount(binaryRule);
		for (int i = 0; i < binaryCounts.length; i++) {
		    for (int j = 0; j < binaryCounts[i].length; j++) {
			if (binaryCounts[i][j]==null)
			    binaryCounts[i][j] = new double[numSubStates[binaryRule.getParentState()]];
			for (int k = 0; k < binaryCounts[i][j].length; k++) {
			    double r = random.nextDouble() * randomness;
			    binaryCounts[i][j][k] += r;
			}
		    }
		}
		binaryRuleCounter.setCount(binaryRule, binaryCounts);
	    }
	}

	normalize();
	unnormalizedSymbolCounter = symbolCounter;
	smooth(false); // this also adds the rules to the proper arrays
    }
	
    public void removeUnlikelyRules(double thresh, double power){
	//System.out.print("Removing everything below "+thresh+" and rasiing rules to the " +power+"th power... ");
	if (isLogarithmMode()) power = Math.log(power);
	int total=0, removed = 0;
	for (int state = 0; state < numStates; state++) {
	    for (int r=0; r<splitRulesWithP[state].length; r++){
		BinaryRule rule = splitRulesWithP[state][r];
		double[][][] scores=rule.getScores2();
		for (int lC=0; lC<scores.length; lC++){
		    for (int rC=0; rC<scores[lC].length; rC++){
			if (scores[lC][rC]==null) continue;
			boolean isNull=true;
			for (int p=0; p<scores[lC][rC].length; p++){
			    total++;
			    if (scores[lC][rC][p]<thresh) {
				scores[lC][rC][p] = 0;
				removed++;
			    }
			    else {
				if (power!=1) scores[lC][rC][p] = Math.pow(scores[lC][rC][p],power);
				isNull=false;
			    }
			}
			if (isNull) scores[lC][rC]=null;
		    }
		}
		rule.setScores2(scores);
		splitRulesWithP[state][r] = rule;
		scores=null;
	    }
	    for (UnaryRule rule : unaryRulesWithParent[state]){
		double[][] scores=rule.getScores2();
		for (int c=0; c<scores.length; c++){
		    if (scores[c]==null) continue;
		    boolean isNull=true;
		    for (int p=0; p<scores[c].length; p++){
			total++;
			if (scores[c][p]<=thresh) {
			    removed++;
			    scores[c][p] = 0;
			}
			else {
			    if (power!=1) scores[c][p] = Math.pow(scores[c][p],power);
			    isNull=false;
			}
		    }
		    if (isNull) scores[c]=null;
		}
		rule.setScores2(scores);
		scores=null;
	    }
	}
	//System.out.print("done.\nRemoved "+removed+" out of "+total+" rules.\n");
    }

    public void smooth(boolean noNormalize){
	//System.out.println("What is unaryRuleCounter? " + unaryRuleCounter);
	//System.out.println("What is binaryRuleCounter? "+ binaryRuleCounter);
	smoother.smooth(unaryRuleCounter,binaryRuleCounter);
	if (!noNormalize) normalize();
		
	// compress and add the rules
	for (UnaryRule unaryRule : unaryRuleCounter.keySet()) {
	    double[][] unaryCounts = unaryRuleCounter.getCount(unaryRule);
	    for (int i = 0; i < unaryCounts.length; i++) {
		if (unaryCounts[i]==null)
		    continue;
		/** allZero records if all probabilities are 0.  If so,
		 * we want to null out the matrix element.
		 */
		double allZero = 0;
		int j=0;
		while (allZero == 0 && j < unaryCounts[i].length){
		    allZero += unaryCounts[i][j++];
		}
		if (allZero==0) {
		    unaryCounts[i] = null;
		}
	    }
	    unaryRule.setScores2(unaryCounts);
	    addUnary(unaryRule);
	}
	computePairsOfUnaries();
	for (BinaryRule binaryRule : binaryRuleCounter.keySet()) {
	    double[][][] binaryCounts = binaryRuleCounter.getCount(binaryRule);
	    for (int i = 0; i < binaryCounts.length; i++) {
		for (int j = 0; j < binaryCounts[i].length; j++) {
		    if (binaryCounts[i][j]==null)
			continue;
		    /** allZero records if all probabilities are 0.  If so,
		     * we want to null out the matrix element.
		     */
		    double allZero = 0;
		    int k=0;
		    while (allZero == 0 && k < binaryCounts[i][j].length){
			allZero += binaryCounts[i][j][k++];
		    }
		    if (allZero==0) {
			binaryCounts[i][j] = null;
		    }
		}
	    }
	    binaryRule.setScores2(binaryCounts);
	    addBinary(binaryRule);
	}
	// Reset all counters:
	prevUnaryRuleCounter = unaryRuleCounter; prevBinaryRuleCounter = binaryRuleCounter;
	unaryRuleCounter = new UnaryCounterTable(numSubStates);
	binaryRuleCounter = new BinaryCounterTable(numSubStates);
	//System.out.println("THE SYMBOL COUNTER: \n" + unnormalizedSymbolCounter);
	symbolCounter = new CounterMap<Integer, Integer>();
	//checkNumberOfSubstates();
    
	// Romain: added the computation for the sum-product closure
	//TODO: fix the code and add this back in
	//sumProductClosedUnaryRulesWithParent = sumProductUnaryClosure(unaryRulesWithParent);

    }
	
    public void clearCounts(){
	unaryRuleCounter = new UnaryCounterTable(numSubStates);
	binaryRuleCounter = new BinaryCounterTable(numSubStates);
	symbolCounter = new CounterMap<Integer, Integer>();
		
    }
	
    /**
     * Normalize the unary & binary probabilities so that they sum to 1 for each parent.
     * The binaryRuleCounter and unaryRuleCounter are assumed to contain probabilities,
     * NOT log probabilities!
     */
    public void normalize() {
	// tally the parent counts
	tallyParentCounts();
	// turn the rule scores into fractions
	for (UnaryRule unaryRule : unaryRuleCounter.keySet()) {
	    double[][] unaryCounts = unaryRuleCounter.getCount(unaryRule);
	    int parentState = unaryRule.getParentState();
	    int nParentSubStates = numSubStates[parentState];
	    int nChildStates = numSubStates[unaryRule.childState];
	    double[] parentCount = new double[nParentSubStates];
	    for (int i = 0; i < nParentSubStates; i++) {
		parentCount[i] = symbolCounter.getCount(parentState, i);
	    }
	    boolean allZero = true;
	    for (int j = 0; j < nChildStates; j++) {
		if (unaryCounts[j]==null) continue;
		for (int i = 0; i < nParentSubStates; i++) {
		    if (parentCount[i]!=0){
			double nVal = (unaryCounts[j][i] / parentCount[i]);
			if (nVal<threshold||SloppyMath.isVeryDangerous(nVal)) nVal = 0;
			unaryCounts[j][i] = nVal;
		    }
		    allZero = allZero && (unaryCounts[j][i]==0);
		}
	    }
	    if (allZero){
		System.out.println("Maybe an underflow? Rule: "+unaryRule+"\n"+ArrayUtil.toString(unaryCounts));
	    }
	    unaryRuleCounter.setCount(unaryRule,unaryCounts);
	}
	for (BinaryRule binaryRule : binaryRuleCounter.keySet()) {
	    double[][][] binaryCounts = binaryRuleCounter.getCount(binaryRule);
	    int parentState = binaryRule.parentState;
	    int nParentSubStates = numSubStates[parentState];
	    double[] parentCount = new double[nParentSubStates];
	    for (int i = 0; i < nParentSubStates; i++) {
		parentCount[i] = symbolCounter.getCount(parentState, i);
	    }
	    for (int j = 0; j < binaryCounts.length; j++) {
		for (int k = 0; k < binaryCounts[j].length; k++) {
		    if (binaryCounts[j][k]==null) continue;
		    for (int i = 0; i < nParentSubStates; i++) {
			if (parentCount[i]!=0){
			    double nVal = (binaryCounts[j][k][i] / parentCount[i]);
			    if (nVal<threshold||SloppyMath.isVeryDangerous(nVal)) nVal = 0;
			    binaryCounts[j][k][i] = nVal;
			}
		    }
		}
	    }
	    binaryRuleCounter.setCount(binaryRule,binaryCounts);
	}
    }
	
	
    /*
     * Check number of substates
     */
    public void checkNumberOfSubstates() {
	for (UnaryRule unaryRule : unaryRuleCounter.keySet()) {
	    double[][] unaryCounts = unaryRuleCounter.getCount(unaryRule);
	    int nParentSubStates = numSubStates[unaryRule.parentState];
	    int nChildSubStates = numSubStates[unaryRule.childState];
	    if (unaryCounts.length!=nChildSubStates){
		System.out.println("Unary Rule "+unaryRule+" should have "+nChildSubStates+" childsubstates.");
	    }
	    if (unaryCounts[0]!=null && unaryCounts[0].length!=nParentSubStates){
		System.out.println("Unary Rule "+unaryRule+" should have "+nParentSubStates+" parentsubstates.");
	    }
	}
	for (BinaryRule binaryRule : binaryRuleCounter.keySet()) {
	    double[][][] binaryCounts = binaryRuleCounter.getCount(binaryRule);
	    int nParentSubStates = numSubStates[binaryRule.parentState];
	    int nLeftChildSubStates = numSubStates[binaryRule.leftChildState];
	    int nRightChildSubStates = numSubStates[binaryRule.rightChildState];
	    if (binaryCounts.length!=nLeftChildSubStates){
		System.out.println("Unary Rule "+binaryRule+" should have "+nLeftChildSubStates+" left childsubstates.");
	    }
	    if (binaryCounts[0].length!=nRightChildSubStates){
		System.out.println("Unary Rule "+binaryRule+" should have "+nRightChildSubStates+" right childsubstates.");
	    }
	    if (binaryCounts[0][0] != null && binaryCounts[0][0].length!=nParentSubStates){
		System.out.println("Unary Rule "+binaryRule+" should have "+nParentSubStates+" parentsubstates.");
	    }
	}
	System.out.println("Done with checks.");
    }

	
    /** Sum the parent symbol counter, symbolCounter.  This is needed when
     * the rule counters are altered, such as when adding randomness
     * in optimize().
     * <p>
     * This assumes that the unaryRuleCounter and binaryRuleCounter contain probabilities,
     * NOT log probabilities! 
     */
    private void tallyParentCounts() {
	symbolCounter = new CounterMap<Integer, Integer>();
	for (UnaryRule unaryRule : unaryRuleCounter.keySet()) {
	    double[][] unaryCounts = unaryRuleCounter.getCount(unaryRule);
	    int parentState = unaryRule.getParentState();
	    isGrammarTag[parentState] = true;
	    if (unaryRule.childState == parentState) continue;
	    int nParentSubStates = numSubStates[parentState];
	    double[] sum = new double[nParentSubStates];
	    for (int j = 0; j < unaryCounts.length; j++) {
		if (unaryCounts[j]==null) continue;
		for (int i = 0; i < nParentSubStates; i++) {
		    double val = unaryCounts[j][i];
		    //if (val>=threshold)	
		    sum[i] += val;
		}
	    }
	    for (int i = 0; i < nParentSubStates; i++) {
		symbolCounter.incrementCount(parentState, i, sum[i]);
	    }

	}
	for (BinaryRule binaryRule : binaryRuleCounter.keySet()) {
	    double[][][] binaryCounts = binaryRuleCounter.getCount(binaryRule);
	    int parentState = binaryRule.parentState;
	    isGrammarTag[parentState] = true;
	    int nParentSubStates = numSubStates[parentState];
	    double[] sum = new double[nParentSubStates];
	    for (int j = 0; j < binaryCounts.length; j++) {
		for (int k = 0; k < binaryCounts[j].length; k++) {
		    if (binaryCounts[j][k]==null) continue;
		    for (int i = 0; i < nParentSubStates; i++) {
			double val = binaryCounts[j][k][i]; 
			//if (val>=threshold) 
			sum[i] += val; 
		    }
		}
	    }
	    for (int i = 0; i < nParentSubStates; i++) {
		symbolCounter.incrementCount(parentState, i, sum[i]);
	    }
	}
    }
	
    public void tallyStateSetTree(Tree<StateSet> tree, Grammar old_grammar) {
	// Check that the top node is not split (it has only one substate)
	if (tree.isLeaf())
	    return;
	if (tree.isPreTerminal())
	    return;
	StateSet node = tree.getLabel();
	if (node.numSubStates() != 1) {
	    System.err.println("The top symbol is split!");
	    System.out.println(tree);
	    System.exit(1);
	}
	// The inside score of its only substate is the (log) probability of the
	// tree
	double tree_score = node.getIScore(0);
	int tree_scale = node.getIScale();
	if (tree_score==0){
	    System.out.println("Something is wrong with this tree. I will skip it.");
	    return;
	}
	tallyStateSetTree(tree, tree_score, tree_scale, old_grammar);
    }
	
    public void tallyStateSetTree(Tree<StateSet> tree, double tree_score, double tree_scale, Grammar old_grammar) {
	if (tree.isLeaf())
	    return;
	if (tree.isPreTerminal())
	    return;
	List<Tree<StateSet>> children = tree.getChildren();
	StateSet parent = tree.getLabel();
	short parentState = parent.getState();
	int nParentSubStates = numSubStates[parentState];
	switch (children.size()) {
	case 0:
	    // This is a leaf (a preterminal node, if we count the words themselves),
	    // nothing to do
	    break;
	case 1:
	    StateSet child = children.get(0).getLabel();
	    short childState = child.getState();
	    int nChildSubStates = numSubStates[childState];
	    UnaryRule urule = new UnaryRule(parentState, childState);
	    double[][] oldUScores = old_grammar.getUnaryScore(urule); // rule score
	    double[][] ucounts = unaryRuleCounter.getCount(urule);
	    if (ucounts==null) ucounts = new double[nChildSubStates][];
	    double scalingFactor = ScalingTools.calcScaleFactor(parent.getOScale()+child.getIScale()-tree_scale);
	    //			if (scalingFactor==0){
	    //				System.out.println("p: "+parent.getOScale()+" c: "+child.getIScale()+" t:"+tree_scale);
	    //			}
	    for (short i = 0; i < nChildSubStates; i++) {
		if (oldUScores[i]==null) continue;
		double cIS = child.getIScore(i);
		if (cIS==0) continue;
		if (ucounts[i]==null) ucounts[i] = new double[nParentSubStates];
		for (short j = 0; j < nParentSubStates; j++) {
		    double pOS = parent.getOScore(j); // Parent outside score
		    if (pOS==0) continue;
		    double rS = oldUScores[i][j];
		    if (rS==0) continue;
		    if (tree_score==0)
			tree_score = 1;
		    double logRuleCount = (rS * cIS / tree_score) * scalingFactor * pOS;
		    ucounts[i][j] += logRuleCount;
		}
	    }
	    //urule.setScores2(ucounts);
	    unaryRuleCounter.setCount(urule, ucounts);
	    break;
	case 2:
	    StateSet leftChild = children.get(0).getLabel();
	    short lChildState = leftChild.getState();
	    StateSet rightChild = children.get(1).getLabel();
	    short rChildState = rightChild.getState();
	    int nLeftChildSubStates = numSubStates[lChildState];
	    int nRightChildSubStates = numSubStates[rChildState];
	    //new double[nLeftChildSubStates][nRightChildSubStates][];
	    BinaryRule brule = new BinaryRule(parentState, lChildState, rChildState);
	    double[][][] oldBScores = old_grammar.getBinaryScore(brule); // rule score
	    if (oldBScores==null){
		//rule was not in the grammar
		//parent.setIScores(iScores2);
		//break;
		oldBScores=new double[nLeftChildSubStates][nRightChildSubStates][nParentSubStates];
		ArrayUtil.fill(oldBScores,1.0);
	    }
	    double[][][] bcounts = binaryRuleCounter.getCount(brule);
	    if (bcounts==null) bcounts = new double[nLeftChildSubStates][nRightChildSubStates][];
	    scalingFactor = ScalingTools.calcScaleFactor(parent.getOScale()+leftChild.getIScale()+rightChild.getIScale()-tree_scale);
	    //			if (scalingFactor==0){
	    //				System.out.println("p: "+parent.getOScale()+" l: "+leftChild.getIScale()+" r:"+rightChild.getIScale()+" t:"+tree_scale);
	    //			}
	    for (short i = 0; i < nLeftChildSubStates; i++) {
		double lcIS = leftChild.getIScore(i);
		if (lcIS==0) continue;
		for (short j = 0; j < nRightChildSubStates; j++) {
		    if (oldBScores[i][j]==null) continue;
		    double rcIS = rightChild.getIScore(j);
		    if (rcIS==0) continue;
		    // allocate parent array
		    if (bcounts[i][j]==null) bcounts[i][j] = new double[nParentSubStates];
		    for (short k = 0; k < nParentSubStates; k++) {
			double pOS = parent.getOScore(k); // Parent outside score
			if (pOS==0) continue;
			double rS = oldBScores[i][j][k];
			if (rS==0) continue;
			if (tree_score==0)
			    tree_score = 1;
			double logRuleCount = (rS * lcIS / tree_score) * rcIS * scalingFactor * pOS;
			/*if (logRuleCount == 0) {
			  System.out.println("rS "+rS+", lcIS "+lcIS+", rcIS "+rcIS+", tree_score "+tree_score+
			  ", scalingFactor "+scalingFactor+", pOS "+pOS);
			  System.out.println("Possibly underflow?");
			  //	logRuleCount = Double.MIN_VALUE;
			  }*/
			bcounts[i][j][k] += logRuleCount;
		    }
		}
	    }
	    binaryRuleCounter.setCount(brule, bcounts);
	    break;
	default:
	    throw new Error("Malformed tree: more than two children");
	}
		
	for (Tree<StateSet> child : children) {
	    tallyStateSetTree(child, tree_score, tree_scale, old_grammar);
	}
    }
	
    public void tallyUninitializedStateSetTree(Tree<StateSet> tree) {
	if (tree.isLeaf())
	    return;
	// the lexicon handles preterminal nodes
	if (tree.isPreTerminal())
	    return;
	List<Tree<StateSet>> children = tree.getChildren();
	StateSet parent = tree.getLabel();
	short parentState = parent.getState();
	int nParentSubStates = parent.numSubStates(); //numSubStates[parentState];
	switch (children.size()) {
	case 0:
	    // This is a leaf (a preterminal node, if we count the words
	    // themselves), nothing to do
	    break;
	case 1:
	    StateSet child = children.get(0).getLabel();
	    short childState = child.getState();
	    int nChildSubStates = child.numSubStates(); //numSubStates[childState];
	    double[][] counts = new double[nChildSubStates][nParentSubStates];
	    UnaryRule urule = new UnaryRule(parentState, childState, counts);
	    unaryRuleCounter.incrementCount(urule, 1.0);
	    break;
	case 2:
	    StateSet leftChild = children.get(0).getLabel();
	    short lChildState = leftChild.getState();
	    StateSet rightChild = children.get(1).getLabel();
	    short rChildState = rightChild.getState();
	    int nLeftChildSubStates = leftChild.numSubStates(); //numSubStates[lChildState];
	    int nRightChildSubStates = rightChild.numSubStates();// numSubStates[rChildState];
	    double[][][] bcounts = new double[nLeftChildSubStates][nRightChildSubStates][nParentSubStates];
	    BinaryRule brule = new BinaryRule(parentState, lChildState, rChildState, bcounts);
	    binaryRuleCounter.incrementCount(brule, 1.0);
	    break;
	default:
	    throw new Error("Malformed tree: more than two children");
	}
		
	for (Tree<StateSet> child : children) {
	    tallyUninitializedStateSetTree(child);
	}
    }
	
    /*public void tallyChart(Pair<double[][][][], double[][][][]> chart, double tree_score,	Grammar old_grammar) {
      double[][][][] iScore = chart.getFirst();
      double[][][][] oScore = chart.getSecond();
      if (tree.isLeaf())
      return;
      if (tree.isPreTerminal())
      return;
      List<Tree<StateSet>> children = tree.getChildren();
      StateSet parent = tree.getLabel();
      short parentState = parent.getState();
      int nParentSubStates = numSubStates[parentState];
      switch (children.size()) {
      case 0:
      // This is a leaf (a preterminal node, if we count the words themselves),
      // nothing to do
      break;
      case 1:
      StateSet child = children.get(0).getLabel();
      short childState = child.getState();
      int nChildSubStates = numSubStates[childState];
      UnaryRule urule = new UnaryRule(parentState, childState);
      double[][] oldUScores = old_grammar.getUnaryScore(urule); // rule score
      double[][] ucounts = unaryRuleCounter.getCount(urule);
      if (ucounts==null) ucounts = new double[nChildSubStates][];
      double scalingFactor = Math.pow(GrammarTrainer.SCALE,
      parent.getOScale()+child.getIScale()-tree_scale);
      if (scalingFactor==0){
      System.out.println("p: "+parent.getOScale()+" c: "+child.getIScale()+" t:"+tree_scale);
      }
      for (short i = 0; i < nChildSubStates; i++) {
      if (oldUScores[i]==null) continue;
      double cIS = child.getIScore(i);
      if (cIS==0) continue;
      if (ucounts[i]==null) ucounts[i] = new double[nParentSubStates];
      for (short j = 0; j < nParentSubStates; j++) {
      double pOS = parent.getOScore(j); // Parent outside score
      if (pOS==0) continue;
      double rS = oldUScores[i][j];
      if (rS==0) continue;
      if (tree_score==0)
      tree_score = 1;
      double logRuleCount = (rS * cIS / tree_score) * scalingFactor * pOS;
      ucounts[i][j] += logRuleCount;
      }
      }
      //urule.setScores2(ucounts);
      unaryRuleCounter.setCount(urule, ucounts);
      break;
      case 2:
      StateSet leftChild = children.get(0).getLabel();
      short lChildState = leftChild.getState();
      StateSet rightChild = children.get(1).getLabel();
      short rChildState = rightChild.getState();
      int nLeftChildSubStates = numSubStates[lChildState];
      int nRightChildSubStates = numSubStates[rChildState];
      //new double[nLeftChildSubStates][nRightChildSubStates][];
      BinaryRule brule = new BinaryRule(parentState, lChildState, rChildState);
      double[][][] oldBScores = old_grammar.getBinaryScore(brule); // rule score
      if (oldBScores==null){
      //rule was not in the grammar
      //parent.setIScores(iScores2);
      //break;
      oldBScores=new double[nLeftChildSubStates][nRightChildSubStates][nParentSubStates];
      ArrayUtil.fill(oldBScores,1.0);
      }
      double[][][] bcounts = binaryRuleCounter.getCount(brule);
      if (bcounts==null) bcounts = new double[nLeftChildSubStates][nRightChildSubStates][];
      scalingFactor = Math.pow(GrammarTrainer.SCALE,
      parent.getOScale()+leftChild.getIScale()+rightChild.getIScale()-tree_scale);
      if (scalingFactor==0){
      System.out.println("p: "+parent.getOScale()+" l: "+leftChild.getIScale()+" r:"+rightChild.getIScale()+" t:"+tree_scale);
      }
      for (short i = 0; i < nLeftChildSubStates; i++) {
      double lcIS = leftChild.getIScore(i);
      if (lcIS==0) continue;
      for (short j = 0; j < nRightChildSubStates; j++) {
      if (oldBScores[i][j]==null) continue;
      double rcIS = rightChild.getIScore(j);
      if (rcIS==0) continue;
      // allocate parent array
      if (bcounts[i][j]==null) bcounts[i][j] = new double[nParentSubStates];
      for (short k = 0; k < nParentSubStates; k++) {
      double pOS = parent.getOScore(k); // Parent outside score
      if (pOS==0) continue;
      double rS = oldBScores[i][j][k];
      if (rS==0) continue;
      if (tree_score==0)
      tree_score = 1;
      double logRuleCount = (rS * lcIS / tree_score) * rcIS * scalingFactor * pOS;
						
      bcounts[i][j][k] += logRuleCount;
      }
      }
      }
      binaryRuleCounter.setCount(brule, bcounts);
      break;
      default:
      throw new Error("Malformed tree: more than two children");
      }
		
      for (Tree<StateSet> child : children) {
      tallyStateSetTree(child, tree_score, tree_scale, old_grammar);
      }
      }
    */
    /*
     * private UnaryRule makeUnaryRule(Tree<String> tree) { int parent =
     * tagNumberer.number(tree.getLabel()); int child =
     * tagNumberer.number(tree.getChildren().get(0).getLabel()); return new
     * UnaryRule(parent, child); }
     * 
     * private BinaryRule makeBinaryRule(Tree<String> tree) { int parent =
     * tagNumberer.number(tree.getLabel()); int lChild =
     * tagNumberer.number(tree.getChildren().get(0).getLabel()); int rChild =
     * tagNumberer.number(tree.getChildren().get(1).getLabel()); return new
     * BinaryRule(parent, lChild, rChild); }
     */
    public void makeCRArrays() {
	// int numStates = closedRulesWithParent.length;
	closedSumRulesWithP = new UnaryRule[numStates][];
	closedSumRulesWithC = new UnaryRule[numStates][];
	closedViterbiRulesWithP = new UnaryRule[numStates][];
	closedViterbiRulesWithC = new UnaryRule[numStates][];
		
	for (int i = 0; i < numStates; i++) {
	    closedSumRulesWithP[i] = (UnaryRule[]) closedSumRulesWithParent[i].toArray(new UnaryRule[0]);
	    closedSumRulesWithC[i] = (UnaryRule[]) closedSumRulesWithChild[i].toArray(new UnaryRule[0]);
	    closedViterbiRulesWithP[i] = (UnaryRule[]) closedViterbiRulesWithParent[i].toArray(new UnaryRule[0]);
	    closedViterbiRulesWithC[i] = (UnaryRule[]) closedViterbiRulesWithChild[i].toArray(new UnaryRule[0]);
	}
    }
    public UnaryRule[] getClosedSumUnaryRulesByParent(int state) {
	if (closedSumRulesWithP == null) {
	    makeCRArrays();
	}
	if (state >= closedSumRulesWithP.length) {
	    return new UnaryRule[0];
	}
	return closedSumRulesWithP[state];
    }
	
    public UnaryRule[] getClosedSumUnaryRulesByChild(int state) {
	if (closedSumRulesWithC == null) {
	    makeCRArrays();
	}
	if (state >= closedSumRulesWithC.length) {
	    return new UnaryRule[0];
	}
	return closedSumRulesWithC[state];
    }
	
    public UnaryRule[] getClosedViterbiUnaryRulesByParent(int state) {
	if (closedViterbiRulesWithP == null) {
	    makeCRArrays();
	}
	if (state >= closedViterbiRulesWithP.length) {
	    return new UnaryRule[0];
	}
	return closedViterbiRulesWithP[state];
    }
	
    public UnaryRule[] getClosedViterbiUnaryRulesByChild(int state) {
	if (closedViterbiRulesWithC == null) {
	    makeCRArrays();
	}
	if (state >= closedViterbiRulesWithC.length) {
	    return new UnaryRule[0];
	}
	return closedViterbiRulesWithC[state];
    }
	
    @SuppressWarnings("unchecked")
	public void purgeRules() {
	Map bR = new HashMap();
	Map bR2 = new HashMap();
	for (Iterator i = bestSumRulesUnderMax.keySet().iterator(); i.hasNext();) {
	    UnaryRule ur = (UnaryRule) i.next();
	    if ((ur.parentState != ur.childState)) {
		bR.put(ur, ur);
		bR2.put(ur, ur);
	    }
	}
	bestSumRulesUnderMax = bR;
	bestViterbiRulesUnderMax = bR2;
    }
	
    @SuppressWarnings("unchecked")
	public List<short[]> getBestViterbiPath(short pState, short np, short cState, short cp) {
	ArrayList<short[]> path = new ArrayList<short[]>();
	short[] state = new short[2];
	state[0] = pState;
	state[1] = np;
	// if we haven't built the data structure of closed paths, then
	// return the simplest possible path
	if (!findClosedPaths) {
	    path.add(state);
	    state = new short[2];
	    state[0] = cState;
	    state[1] = cp;
	    path.add(state);
	    return path;
	} else {
	    //read the best paths off of the closedViterbiPaths list
	    if (pState==cState && np==cp) {
		path.add(state);
		path.add(state);
		return path;
	    }
	    while (state[0]!=cState || state[1]!=cp) {
		path.add(state);
		state[0] = (short)closedViterbiPaths[state[0]][state[1]];
	    }
	    // add the destination state as well
	    path.add(state);
	    return path;
	}
    }
	
    @SuppressWarnings("unchecked")
    /** WARNING: This is not guaranteed to work with the internal state representation.
	(12 March 2012)
	* But it also doesn't seem to be called...
    */
	private void closeRulesUnderMax(UnaryRule ur) {
	short pState = ur.parentState;
	int nPSubStates = numSubStates[pState];
	short cState = ur.childState;
	double[][] uScores = ur.getScores2();
	// do all sum rules
	for (int i = 0; i < closedSumRulesWithChild[pState].size(); i++) {
	    UnaryRule pr = (UnaryRule) closedSumRulesWithChild[pState].get(i);
	    for (int j = 0; j < closedSumRulesWithParent[cState].size(); j++) {
		short parentState = pr.parentState;
		int nParentSubStates = numSubStates[parentState];
		UnaryRule cr = (UnaryRule) closedSumRulesWithParent[cState].get(j);
		UnaryRule resultR = new UnaryRule(parentState, cr.getChildState());
		double[][] scores = new double[numSubStates[cr.getChildState()]][nParentSubStates];
		for (int np = 0; np < scores[0].length; np++) {
		    for (int cp = 0; cp < scores.length; cp++) {
			// sum over intermediate substates
			double sum = 0;
			for (int unp = 0; unp < nPSubStates; unp++) {
			    for (int ucp = 0; ucp < uScores.length; ucp++) {
				sum += pr.getScore(np, unp) * cr.getScore(ucp, cp) * ur.getScore(unp, ucp);
			    }
			}
			scores[cp][np] = sum;
		    }
		}
		resultR.setScores2(scores);
		//add rule to bestSumRulesUnderMax if it's better
		relaxSumRule(resultR,pState,cState);
	    }
	}
	// do viterbi rules also
	for (short i = 0; i < closedViterbiRulesWithChild[pState].size(); i++) {
	    UnaryRule pr = (UnaryRule) closedViterbiRulesWithChild[pState].get(i);
	    for (short j = 0; j < closedViterbiRulesWithParent[cState].size(); j++) {
		UnaryRule cr = (UnaryRule) closedViterbiRulesWithParent[cState].get(j);
		short parentState = pr.parentState;
		int nParentSubStates = numSubStates[parentState];
		UnaryRule resultR = new UnaryRule(parentState, cr.getChildState());
		double[][] scores = new double[numSubStates[cr.getChildState()]][nParentSubStates];
		short[][] intermediateSubState1 = new short[nParentSubStates][numSubStates[cr.getChildState()]];
		short[][] intermediateSubState2 = new short[nParentSubStates][numSubStates[cr.getChildState()]];
		for (int np = 0; np < scores[0].length; np++) {
		    for (int cp = 0; cp < scores.length; cp++) {
			// sum over intermediate substates
			double max = 0;
			for (short unp = 0; unp < nPSubStates; unp++) {
			    for (short ucp = 0; ucp < uScores.length; ucp++) {
				double score = pr.getScore(np, unp) * cr.getScore(ucp, cp) * ur.getScore(unp, ucp);
				if (score > max) {
				    max = score;
				    intermediateSubState1[np][cp] = unp; 
				    intermediateSubState2[np][cp] = ucp; 
				}
			    }
			}
			scores[cp][np] = max;
		    }
		}
		resultR.setScores2(scores);
		//add rule to bestSumRulesUnderMax if it's better
		relaxViterbiRule(resultR,pState,intermediateSubState1,cState,intermediateSubState2);
	    }
	}
    }
	
    public int getUnaryIntermediate(short start, short end){
	return closedSumPaths[start][end];
    }
	
	
    @SuppressWarnings("unchecked")
	private boolean relaxSumRule(UnaryRule ur, int intState1, int intState2) {
	//TODO: keep track of path
	UnaryRule bestR = (UnaryRule) bestSumRulesUnderMax.get(ur);
	if (bestR == null) {
	    bestSumRulesUnderMax.put(ur, ur);
	    closedSumRulesWithParent[ur.parentState].add(ur);
	    closedSumRulesWithChild[ur.childState].add(ur);
	    return true;
	} else {
	    boolean change = false;
	    double[][] uscores=ur.getScores2();
	    double[][] scores=bestR.getScores2();
	    for (int i=0; i<uscores[0].length; i++) {
		for (int j=0; j<uscores.length; j++) {
		    if (scores[j][i] < uscores[j][i]) {
			scores[j][i] = uscores[j][i];
			change=true;
		    }
		}
	    }
	    ur.setScores2(uscores);
	    bestR.setScores2(scores);
	    return change;
	}
    }
	
    public void computePairsOfUnaries(){
	//closedSumRulesWithParent = closedViterbiRulesWithParent = unaryRulesWithParent;
	for (short parentState=0; parentState<numStates; parentState++){
	    for (short childState=0; childState<numStates; childState++){
		if (parentState==childState) continue;
		int nParentSubStates = numSubStates[parentState];
		int nChildSubStates = numSubStates[childState];
		UnaryRule resultRsum = new UnaryRule(parentState, childState);
		UnaryRule resultRmax = new UnaryRule(parentState, childState);
		double[][] scoresSum = new double[nChildSubStates][nParentSubStates];
		double[][] scoresMax = new double[nChildSubStates][nParentSubStates];
		double maxSumScore = -1;
		short bestSumIntermed = -1;
		short bestMaxIntermed = -2;
		for (int i = 0; i < unaryRulesWithParent[parentState].size(); i++) {
		    UnaryRule pr = (UnaryRule) unaryRulesWithParent[parentState].get(i);
		    short state = pr.getChildState();
		    if (state==childState){
			double total = 0;
			double[][] scores = pr.getScores2();
			for (int cp = 0; cp < nChildSubStates; cp++) {
			    if (scores[cp]==null) continue;
			    for (int np = 0; np < nParentSubStates; np++) {
				// sum over intermediate substates
				if(InternalNodeSet.isSubstateInternal(parentState,np)){
				    scoresMax[cp][np] = scores[cp][np];
				    total += scores[cp][np];
				    //do I need the following? I'm thinking I might, if only
				    //for those rules that are still here because of internal productions
				    //bestMaxIntermed = -1;
				    continue;
				} 
				double sum = scores[cp][np];
				scoresSum[cp][np] += sum;
				total += sum;
				if (sum > scoresMax[cp][np]) {
				    scoresMax[cp][np] = sum;
				    bestMaxIntermed = -1;
				}
			    }
			}
			if (total>maxSumScore){
			    bestSumIntermed=-1;
			    maxSumScore=total;
			}
		    }
		    else{
			for (int j = 0; j < unaryRulesWithC[childState].size(); j++) {
			    UnaryRule cr = (UnaryRule) unaryRulesWithC[childState].get(j);
			    if (state!=cr.getParentState()) continue;
			    int nMySubStates = numSubStates[state];
			    double total = 0;
			    for (int np = 0; np < nParentSubStates; np++) {
				for (int cp = 0; cp < nChildSubStates; cp++) {
				    if(InternalNodeSet.isSubstateInternal(parentState,np)){
					double tscore = pr.getScore(np,cp);
					scoresMax[cp][np] = tscore;
					total += tscore;
					//do I need the following? I'm thinking I might, if only
					//for those rules that are still here because of internal productions
					//bestMaxIntermed = -1;
					continue;
				    } 
				    // sum over intermediate substates
				    double sum = 0;
				    double max = 0;
				    for (int unp = 0; unp < nMySubStates; unp++) {
					double val  = pr.getScore(np, unp) * cr.getScore(unp, cp);
					sum += val;
					max = Math.max(max, val);
				    }
				    scoresSum[cp][np] += sum;
				    total += sum;
				    if (max > scoresMax[cp][np]) {
					scoresMax[cp][np] = max;
					bestMaxIntermed = state;
				    }
				}
			    }
			    if (total>maxSumScore){
				maxSumScore=total;
				bestSumIntermed=state;
			    }
			}
		    }
		}
		if (maxSumScore>-1){
		    resultRsum.setScores2(scoresSum);
		    addUnary(resultRsum);
		    closedSumRulesWithParent[parentState].add(resultRsum);
		    closedSumRulesWithChild[childState].add(resultRsum);
		    closedSumPaths[parentState][childState]=bestSumIntermed;
		}
		if (bestMaxIntermed>-2){
		    resultRmax.setScores2(scoresMax);
		    closedViterbiRulesWithParent[parentState].add(resultRmax);
		    closedViterbiRulesWithChild[childState].add(resultRmax);
		    closedViterbiPaths[parentState][childState]=bestMaxIntermed;
		}
	    }
	}

    }
    /**
     * Update the best unary chain probabilities and paths with this new rule.<br/>
     *
     * WARNING: This is not guaranteed to work with the internal state representation.
     * 
     * @param ur
     * @param subStates1
     * @param subStates2
     * @return
     */
    @SuppressWarnings("unchecked")
	private void relaxViterbiRule(UnaryRule ur, short intState1,
				      short[][] intSubStates1, short intState2, short[][] intSubStates2) {
	throw new Error("Viterbi closure is broken!");
	/*		UnaryRule bestR = (UnaryRule) bestViterbiRulesUnderMax.get(ur);
			boolean isNewRule = (bestR==null);
			if (isNewRule) {
			bestViterbiRulesUnderMax.put(ur, ur);
			closedViterbiRulesWithParent[ur.parentState].add(ur);
			closedViterbiRulesWithChild[ur.childState].add(ur);
			bestR = ur;
			}
			for (int i=0; i<ur.scores[0].length; i++) {
			for (int j=0; j<ur.scores.length; j++) {
			if (isNewRule || bestR.scores[j][i] < ur.scores[j][i]) {
			bestR.scores[j][i] = ur.scores[j][i];
			// update best path information
			if (findClosedPaths) {
			short[] intermediate = null;
			if (ur.parentState==intState1 && intSubStates1[i][j]==i) {
			intermediate = new short[2];
			intermediate[0] = intState2;
			intermediate[1] = intSubStates2[i][j];
			} else {
			//intermediate = closedViterbiPaths[ur.parentState][intState1][i][intSubStates1[i][j]];
			}
			if (closedViterbiPaths[ur.parentState][ur.childState]==null) {
			closedViterbiPaths[ur.parentState][ur.childState] = new short[numSubStates[ur.parentState]][numSubStates[ur.childState]][];
			}
			closedViterbiPaths[ur.parentState][ur.childState][i][j] = intermediate;
			}
			}
			}
			}
	*/	}
	
    /** 
     * Initialize the best unary chain probabilities and paths with this rule.
     * Given the changes I've made, I could probably make this a better implementation.
     * 
     * @param rule
     */
    @SuppressWarnings("unchecked")
	private void relaxViterbiRule(UnaryRule rule) {
	bestViterbiRulesUnderMax.put(rule, rule);
	closedViterbiRulesWithParent[rule.parentState].add(rule);
	closedViterbiRulesWithChild[rule.childState].add(rule);
	if (findClosedPaths) {
	    double[][] scores=rule.getScores2();
	    for (short i = 0; i < scores.length; i++) {
		for (short j = 0; j < scores[i].length; j++) {
		    short[] pair = new short[2];
		    pair[0] = rule.childState;
		    pair[1] = j;
		}
	    }
	}
    }
	
    /**
     * 'parentRules', 'childRules' and the return value all have the same format as 'unaryRulesWithParent',
     * but can be thought of as square matrices. All this function does is a matrix multiplication, but
     * operating directly on this non-standard matrix representation.
     * 'parentRules' gives the probability of going from A to B, 'childRules' from B to C, and the return
     * value from A to C (summing out B).
     * This function is intended primarily to compute unaryRulesWithParent^n.
     */ 
    private List<UnaryRule>[] matrixMultiply(List<UnaryRule>[] parentRules, List<UnaryRule>[] childRules) {
  	throw new Error("I'm broken by parent first");
  	/*
	  double[][][][] scores = new double[numStates][numStates][][];
	  for ( short A=0; A<numStates; A++ ) {
	  for ( UnaryRule rAB : parentRules[A] ) {
	  short B = rAB.childState;
	  double[][] scoresAB = rAB.getScores();
	  for ( UnaryRule rBC : childRules[B] ) {
          short C = rBC.childState;
          if ( scores[A][C] == null ) {
	  scores[A][C] = new double[numSubStates[A]][numSubStates[C]];
	  ArrayUtil.fill(scores[A][C], Double.NEGATIVE_INFINITY);
          }
          double[][] scoresBC = rBC.getScores();
          double[] scoresToAdd = new double[numSubStates[B]+1];
          for ( int a = 0; a < numSubStates[A]; a++ ) {
	  for ( int c = 0; c < numSubStates[C]; c++ ) {
	  // Arrays.fill(scoresToAdd, Double.NEGATIVE_INFINITY);  // No need to here
	  scoresToAdd[scoresToAdd.length-1] = scores[A][C][a][c];  // The current score to which to add the new contributions
	  for ( int b = 0; b < numSubStates[B]; b++ ) {
	  scoresToAdd[b] = scoresAB[a][b] + scoresBC[b][c];
	  }
	  scores[A][C][a][c] = SloppyMath.logAdd(scoresToAdd);
	  }
          }
	  }
	  }
	  }
	  @SuppressWarnings("unchecked")
	  List<UnaryRule>[] result = new List[numStates];
	  for ( short A=0; A<numStates; A++ ) {
	  result[A] = new ArrayList<UnaryRule>();
	  for ( short C=0; C<numStates; C++ ) {
	  if ( scores[A][C] != null ) {
          result[A].add(new UnaryRule(A,C,scores[A][C]));
	  }
	  }
	  }
	  return result;
	*/
    }
  
    /**
     * rules1 += rules2  (adds rules2 into rules1, destroying rules1)
     * No sharing of score arrays occurs because of this operation since rules2 data is either added
     * in or copied. 
     * @param rules1
     * @param rules2
     */
    private void matrixAdd(List<UnaryRule>[] rules1, List<UnaryRule>[] rules2) {    
  	throw new Error("I'm broken by parent first");
  	/*
	  for ( short A=0; A<numStates; A++ ) {
	  for ( UnaryRule r2 : rules2[A] ) {
	  short child2 = r2.getChildState();
	  double[][] scores2 = r2.getScores();
	  boolean matchFound = false;
	  for ( UnaryRule r1 : rules1[A] ) {
          short child1 = r1.getChildState();
          if ( child1 == child2 ) {
	  double[][] scores1 = r1.getScores();
	  for ( int a = 0; a < numSubStates[A]; a++ ) {
	  for ( int c = 0; c < numSubStates[child1]; c++ ) {
	  scores1[a][c] = SloppyMath.logAdd(scores1[a][c], scores2[a][c]);
	  }
	  }
	  matchFound = true;
	  break;
          }
	  }
	  if (!matchFound) {
          // Make a (deep) copy of rule r2
          UnaryRule ruleCopy = new UnaryRule(r2);
          double[][] scoresCopy = new double[numSubStates[A]][numSubStates[child2]];
          for ( int a = 0; a < numSubStates[A]; a++ ) {
	  for ( int c = 0; c < numSubStates[child2]; c++ ) {
	  scoresCopy[a][c] = scores2[a][c];
	  }
          }
          ruleCopy.setScores(scoresCopy);
          rules1[A].add(ruleCopy);
	  }
	  }
	  }
	*/
    }

    private List<UnaryRule>[] matrixUnity() {
  	throw new Error("I'm broken by parent first");
	//    List<UnaryRule>[] result = new List[numStates];
	//    for ( short A=0; A<numStates; A++ ) {
	//      result[A] = new ArrayList<UnaryRule>();
	//      double[][] scores = new double[numSubStates[A]][numSubStates[A]];
	//      ArrayUtil.fill(scores, Double.NEGATIVE_INFINITY);
	//      for ( int a = 0; a < numSubStates[A]; a++ ) {
	//        scores[a][a] = 0;
	//      }
	//      UnaryRule rule = new UnaryRule(A, A, scores);
	//      result[A].add(rule);
	//    }
	//    return result;
    }
  
    /**
     * @param P
     * @return I + P + P^2 + P^3 + ... (approximation by truncation after some power)
     */
    private List<UnaryRule>[] sumProductUnaryClosure(List<UnaryRule>[] P) {
  	throw new Error("I'm broken by parent first");
  	/*
	  List<UnaryRule>[] R = matrixUnity();
	  matrixAdd(R, P);         // R = I + P + P^2 + P^3 + ...
	  List<UnaryRule>[] Q = P; // Q = P^k
	  int maxPower = 3;
	  for ( int i = 1; i < maxPower; i++ ) {
	  Q = matrixMultiply(Q, P);
	  matrixAdd(R, Q);
	  }
	  return R;
	*/
    }

    /**
     * Assumption: A in possibleSt ==> V[A] != null. This property is true of the result as well.
     * The converse is not true because of a workaround for part of speech tags that we must handle
     * here.
     * @param V (considered a row vector, indexed by (state, substate))
     * @param M (a matrix represented in List<UnaryRule>[] (by parent) format)
     * @param possibleSt (a list of possible states to consider)
     * @return U=V*M (row vector)
     */
    public double[][] matrixVectorPreMultiply(double[][] V, List<UnaryRule>[] M, List<Integer> possibleSt) {
  	throw new Error("I'm broken by parent first");
  	/*
	  double[][] U = new double[numStates][];
	  for (int pState : possibleSt){
	  U[pState] = new double[numSubStates[pState]];
	  Arrays.fill(U[pState], Double.NEGATIVE_INFINITY);
	  UnaryRule[] unaries = M[pState].toArray(new UnaryRule[0]);
	  for ( UnaryRule ur : unaries ) {
	  int cState = ur.childState;
	  if ( V[cState] == null ) {
          continue;
	  }
	  double[][] scores = ur.getScores();  // numSubStates[pState] * numSubStates[cState]
	  int nParentStates = numSubStates[pState];
	  int nChildStates = numSubStates[cState];
	  double[] termsToAdd = new double[nChildStates+1]; // Could be inside the for(np) loop
	  for (int np = 0; np < nParentStates; np++) {
          Arrays.fill(termsToAdd, Double.NEGATIVE_INFINITY);
          double currentVal = U[pState][np];
          termsToAdd[termsToAdd.length-1] = currentVal;
          for (int cp = 0; cp < nChildStates; cp++) {
	  double iS = V[cState][cp];
	  if (iS == Double.NEGATIVE_INFINITY) {
	  continue;
	  }
	  double pS = scores[np][cp];
	  termsToAdd[cp] = iS + pS;
          }
          
          double newVal = SloppyMath.logAdd(termsToAdd);
          if (newVal > currentVal) {
	  U[pState][np] = newVal;
          }
	  }
	  }
	  }
	  return U;
	*/
    }
  
    /**
     * Assumption: A in possibleSt ==> V[A] != null. This property is true of the result as well.
     * The converse is not true because of a workaround for part of speech tags that we must handle
     * here.
     * @param M (a matrix represented in List<UnaryRule>[] (by parent) format)
     * @param V (considered a column vector, indexed by (state, substate))
     * @param possibleSt (a list of possible states to consider)
     * @return U=M*V (column vector)
     */
    public double[][] matrixVectorPostMultiply(List<UnaryRule>[] M, double[][] V, List<Integer> possibleSt) {
  	throw new Error("I'm broken by parent first");
  	/*
	  double[][] U = new double[numStates][];
	  for (int cState : possibleSt){
	  U[cState] = new double[numSubStates[cState]];
	  Arrays.fill(U[cState], Double.NEGATIVE_INFINITY);
	  }
	  for (int pState : possibleSt){
	  UnaryRule[] unaries = M[pState].toArray(new UnaryRule[0]);
	  for ( UnaryRule ur : unaries ) {
	  int cState = ur.childState;
	  if ( U[cState] == null ) {
          continue;
	  }
	  double[][] scores = ur.getScores();  // numSubStates[pState] * numSubStates[cState]
	  int nParentStates = numSubStates[pState];
	  int nChildStates = numSubStates[cState];
	  double[] termsToAdd = new double[nParentStates+1]; // Could be inside the for(np) loop
	  for (int cp = 0; cp < nChildStates; cp++) {
          Arrays.fill(termsToAdd, Double.NEGATIVE_INFINITY);
          double currentVal = U[cState][cp];
          termsToAdd[termsToAdd.length-1] = currentVal;
          for (int np = 0; np < nParentStates; np++) {
	  double oS = V[pState][np];
	  if (oS == Double.NEGATIVE_INFINITY) {
	  continue;
	  }
	  double pS = scores[np][cp];
	  termsToAdd[cp] = oS + pS;
          }
          
          double newVal = SloppyMath.logAdd(termsToAdd);
          if (newVal > currentVal) {
	  U[cState][cp] = newVal;
          }
	  }
	  }
	  }
	  return U;
	*/
    }
  
    /**
     * Populates the "splitRules" accessor lists using the existing rule lists. If
     * the state is synthetic, these lists contain all rules for the state. If the
     * state is NOT synthetic, these lists contain only the rules in which both
     * children are not synthetic.
     * <p>
     * <i>This method must be called before the grammar is used, either after
     * training or deserializing grammar.</i>
     */
    @SuppressWarnings("unchecked")
	public void splitRules() {
	//		splitRulesWithLC = new BinaryRule[numStates][];
	//		splitRulesWithRC = new BinaryRule[numStates][];
	//makeRulesAccessibleByChild();
		
	if (binaryRulesWithParent==null) return;
	splitRulesWithP = new BinaryRule[numStates][];
	splitRulesWithLC = new BinaryRule[numStates][];
	splitRulesWithRC = new BinaryRule[numStates][];
		
	for (int state = 0; state < numStates; state++) {
	    splitRulesWithLC[state] = toBRArray(binaryRulesWithLC[state]);
	    splitRulesWithRC[state] = toBRArray(binaryRulesWithRC[state]);
	    splitRulesWithP[state] = toBRArray(binaryRulesWithParent[state]);
	}
	// we don't need the original lists anymore
	binaryRulesWithParent = null;
	binaryRulesWithLC = null;
	binaryRulesWithRC = null;
	makeCRArrays();
    }
	
    public BinaryRule[] splitRulesWithLC(int state) {
	//		System.out.println("splitRulesWithLC not supported anymore.");
	//		return null;
	if (state >= splitRulesWithLC.length) {
	    return new BinaryRule[0];
	}
	return splitRulesWithLC[state];
    }
	
    public BinaryRule[] splitRulesWithRC(int state) {
	//		System.out.println("splitRulesWithLC not supported anymore.");
	//		return null;
	if (state >= splitRulesWithRC.length) {
	    return new BinaryRule[0];
	}
	return splitRulesWithRC[state];
    }
	
    public BinaryRule[] splitRulesWithP(int state) {
	if (splitRulesWithP==null) splitRules();
	if (state >= splitRulesWithP.length) {
	    return new BinaryRule[0];
	}
	return splitRulesWithP[state];
    }
	
    private BinaryRule[] toBRArray(List<BinaryRule> list) {
	// Collections.sort(list, Rule.scoreComparator()); // didn't seem to help
	BinaryRule[] array = new BinaryRule[list.size()];
	for (int i = 0; i < array.length; i++) {
	    array[i] = list.get(i);
	}
	return array;
    }

    public double[][] getUnaryScore(short pState, short cState) {
	UnaryRule r = getUnaryRule(pState, cState);
	if (r != null)
	    return r.getScores2();
	if (GrammarTrainer.VERBOSE) System.out.println("The requested UNARY rule ("+uSearchRule+") is not in the grammar!");
	double[][] uscores = new double[numSubStates[cState]][numSubStates[pState]];
	ArrayUtil.fill(uscores,1.0);
	return uscores;
    }

    /**
     * @param pState
     * @param cState
     * @return
     */
    public UnaryRule getUnaryRule(short pState, short cState) {
	UnaryRule uRule = new UnaryRule (pState, cState);
	UnaryRule r = unaryRuleMap.get(uRule);
	return r;
    }

	
    public double[][] getUnaryScore(UnaryRule rule) {
	UnaryRule r = unaryRuleMap.get(rule);
	if (r != null)
	    return r.getScores2();
	if (GrammarTrainer.VERBOSE) System.out.println("The requested UnaryRule \"rule\" UNARY rule ("+rule+") is not in the grammar!");
	double[][] uscores = new double[numSubStates[rule.getChildState()]][numSubStates[rule.getParentState()]];
	ArrayUtil.fill(uscores,1.0);
	return uscores;
    }
	
    public double[][][] getBinaryScore(short pState, short lState, short rState) {
	BinaryRule r = getBinaryRule(pState, lState, rState);
	if (r != null)
	    return r.getScores2();
	if (GrammarTrainer.VERBOSE) System.out.println("The requested binary rule ("+bSearchRule+") is not in the grammar!");
	double[][][] bscores = new double[numSubStates[lState]][numSubStates[rState]][numSubStates[pState]];
	ArrayUtil.fill(bscores,1.0);
	return bscores;
    }

    /**
     * @param pState
     * @param lState
     * @param rState
     * @return
     */
    public BinaryRule getBinaryRule(short pState, short lState, short rState) {
	BinaryRule bRule = new BinaryRule(pState, lState, rState);
	BinaryRule r = binaryRuleMap.get(bRule);
	return r;
    }

    public double[][][] getBinaryScore(BinaryRule rule) {
	BinaryRule r = binaryRuleMap.get(rule);
	if (r != null)
	    return r.getScores2();
	else {
	    if (GrammarTrainer.VERBOSE) System.out.println("The requested rule ("+rule+") is not in the grammar!");
	    double[][][] bscores = new double[numSubStates[rule.getLeftChildState()]][numSubStates[rule.getRightChildState()]][numSubStates[rule.getParentState()]];
	    ArrayUtil.fill(bscores,1.0);
	    return bscores;
	}
    }

	
    public void printUnnormalizedSymbolCounter(Numberer tagNumberer) {
	Set<Integer> set = unnormalizedSymbolCounter.keySet();
	PriorityQueue<String> pq = new PriorityQueue<String>(set.size());
	for (Integer i : set) {
	    pq.add((String) tagNumberer.object(i), unnormalizedSymbolCounter.getCount(i, 0));
	    // System.out.println(i+". "+(String)tagNumberer.object(i)+"\t
	    // "+unnormalizedSymbolCounter.getCount(i,0));
	}
	int i = 0;
	while (pq.hasNext()) {
	    i++;
	    double p = (double) pq.getPriority();
	    System.out.println(i + ". " + pq.next() + "\t " + p);
	}
    }
	
    public void printSymbolCounter(Numberer tagNumberer) {
	Set<Integer> set = symbolCounter.keySet();
	PriorityQueue<String> pq = new PriorityQueue<String>(set.size());
	for (Integer i : set) {
	    pq.add((String) tagNumberer.object(i), symbolCounter.getCount(i, 0));
	    // System.out.println(i+". "+(String)tagNumberer.object(i)+"\t
	    // "+symbolCounter.getCount(i,0));
	}
	int i = 0;
	while (pq.hasNext()) {
	    i++;
	    int p = (int) pq.getPriority();
	    System.out.println(i + ". " + pq.next() + "\t " + p);
	}
    }
	
    public int getSymbolCount(Integer i) {
	return (int) symbolCounter.getCount(i, 0);
    }
	
    private void makeRulesAccessibleByChild(){
	// first the binaries
	if (true) return;
	for (int state=0; state<numStates; state++){
	    if (!isGrammarTag[state]) continue;
	    if (binaryRulesWithParent==null) continue;
	    for (BinaryRule rule : binaryRulesWithParent[state]){
		binaryRulesWithLC[rule.leftChildState].add(rule);
		binaryRulesWithRC[rule.rightChildState].add(rule);
	    }
	    //			for (UnaryRule rule : unaryRulesWithParent[state]){
	    //				unaryRulesWithC[rule.childState].add(rule);
	    //			}
	}
		
    }

	
	
    /**
     * Split all substates into two new ones. This produces a new Grammar with
     * updated rules.
     * 
     * @param randomness
     *          percent randomness applied in splitting rules
     * @param mode
     *				0: normalized (at least almost)
     *        1: not normalized (when splitting a log-linear grammar)
     *        2: just noise (for log-linear grammars with cascading regularization)          
     * @return
     */
    public Grammar splitAllStates(short[] numNonInternalBeforeLatestCouple, int[] givenOffset, double randomness, int[] counts, boolean moreSubstatesThanCounts, int mode) {
	if (logarithmMode) {
	    throw new Error("Do not split states when Grammar is in logarithm mode");
	}
	short[] newNumSubStates = new short[numSubStates.length];
	int[] offset = new int[numSubStates.length];
	for (short i = 0; i < numSubStates.length; i++) {
	    //twice the number of non-internal nodes PLUS number of internals
	    newNumSubStates[i] = (short)(2*numNonInternalBeforeLatestCouple[i] + (short)(InternalNodeSet.getNumberOfInternalsForTag(i)));
	    System.out.println("for tag " + i +", we had " + numNonInternalBeforeLatestCouple[i] + " and " + InternalNodeSet.getNumberOfInternalsForTag(i) + " and now we have " + newNumSubStates[i] + " (except for ROOT)");
	    offset[i] = numNonInternalBeforeLatestCouple[i];
	    givenOffset[i] = offset[i];
	}
	//move internal nodes
	boolean doNotNormalize = (mode==1);
	newNumSubStates[0] = 1; // never split ROOT
	// create the new grammar
	Grammar grammar = new Grammar(newNumSubStates, findClosedPaths, smoother, this, threshold);
	Random random = GrammarTrainer.RANDOM;

	for (BinaryRule oldRule : binaryRuleMap.keySet()) {
	    BinaryRule newRule = oldRule.splitRule(numSubStates, newNumSubStates, offset, random, randomness, doNotNormalize, mode);
	    grammar.addBinary(newRule);
	}

	for (UnaryRule oldRule : unaryRuleMap.keySet()){
	    UnaryRule newRule = oldRule.splitRule(numSubStates, newNumSubStates, offset, random, randomness, doNotNormalize, mode); 
	    
	    grammar.addUnary(newRule);
	}
	//now go through internal node set and change the mapping
	givenOffset = offset;
	
	grammar.isGrammarTag = this.isGrammarTag;
	grammar.extendSplitTrees(splitTrees, offset, numSubStates);
	grammar.computePairsOfUnaries();
	return grammar;
    }


    /**
     *
     * @param constraintSet
     * @param parsedTrainingStats (LatentStatistics)
     */
    public Grammar hypothesizeCouplings(ConstraintSet constraintSet, LatentStatistics LS, Set</*Map<Pair<Integer,Integer>, */BerkeleyCompatibleFragment> easyPreterminalCouplingMap, short[] numSigsPerTag, double gamma, double randomness, int mode){//, int[] counts, boolean moreSubstatesThanCounts) {
        if (logarithmMode) {
            throw new Error("Do not couple rules when Grammar is in logarithm mode");
        }

	//store the number of new latently-annotated (internal) nodes to add for each symbol
	short[] newSignaturesToAdd = new short[numSubStates.length];
	Random random = GrammarTrainer.RANDOM;	
	boolean doNotNormalize = (mode==1);

	//the list and maps in which to accumulate all the new fragments
	List<BerkeleyCompatibleFragment> bcfList = new LinkedList<BerkeleyCompatibleFragment>();
	Map<UnaryRule,Set<Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>>> unaryBCFMap = new HashMap<UnaryRule,Set<Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>>>();
	Map<BinaryRule,Set<Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>>> binaryBCFMap = new HashMap<BinaryRule,Set<Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>>>();
	
	//the number of new signatures for every tag (coarse label)
	for (short i = 0; i < numSubStates.length; i++) {
	    numSigsPerTag[i] = numSubStates[i];	    
	    System.out.println("starting off tag="+i+" with "+numSubStates[i]+" substates");
	}

	//System.out.println("the old grammar!:");
	//System.out.println(this);

	//an object to help with the remapping
	//maps (tag,sig_old) to sig_new (implicitly, (tag,sig_new))
	//maybe I could do this as an array...?
	Map<Pair<Integer,Integer>,Set<Integer>> mapper = new HashMap<Pair<Integer,Integer>,Set<Integer>>();

	Map<Vector<Integer>,Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>> startingFragmentMap = new HashMap<Vector<Integer>,Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>>();

	boolean useSubstates = LS.doWeUseSubstates();
	
	Set<String> previousCouplings = InternalNodeSet.getPriorCouplings();
       	//System.out.println("Prior to coupling, InternalNodeSet is = \n\t"+InternalNodeSet.toString2());
	//System.out.print("And justAdded is...\n\t");
	//System.out.println("and previousCouplings is \n"+ previousCouplings);
	InternalNodeSet.printJustAdded();
	for (UnaryRule oldRule : unaryRuleMap.keySet()) {
	    hypothesizeCouplings(oldRule,unaryBCFMap, constraintSet, startingFragmentMap, previousCouplings, newSignaturesToAdd, numSigsPerTag, mapper, LS, 0., random, randomness, doNotNormalize, mode, useSubstates);	    
	}
	for (BinaryRule oldRule : binaryRuleMap.keySet()) {
	    hypothesizeCouplings(oldRule,binaryBCFMap, constraintSet, startingFragmentMap, previousCouplings, newSignaturesToAdd, numSigsPerTag, mapper, LS, 0., random, randomness, doNotNormalize, mode, useSubstates);	    
	}
	InternalNodeSet.setPriorCouplings(previousCouplings);

	Set<ProductionTuple> allHypoedFrags = new HashSet<ProductionTuple>();
	for(Map.Entry<UnaryRule,Set<Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>>> entry : unaryBCFMap.entrySet()){
	    //System.out.println("what are the scores of this rule? "+ entry.getKey());
	    for(Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment> bcfPair : entry.getValue()){
		//System.out.println("what is bcfPair from unary? " + bcfPair);
		for(ProductionTuple pt : bcfPair.getFirst().getTuples())
		    allHypoedFrags.add(pt);
		if(bcfPair.getSecond()==null) continue;
		for(ProductionTuple pt : bcfPair.getSecond().getTuples())
		    allHypoedFrags.add(pt);
		//check if the second one is just a preterminal
		List<ProductionTuple> bcfbottomtuples = bcfPair.getSecond().getTuples();
		if(bcfPair.getSecond().isPreterminal()){
		//if(bcfPair.getSecond().getRoot()!=null && bcfPair.getSecond().getRoot().isPreterminal()){
		    ProductionTuple pt = bcfbottomtuples.get(0);
		    easyPreterminalCouplingMap.add(bcfPair.getSecond());
		    //easyPreterminalCouplingMap.put(new Pair<Integer,Integer>(pt.getTag(0),pt.getSig(0)), bcfPair.getSecond());
		}
	    }
	}
	for(Map.Entry<BinaryRule,Set<Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>>> entry : binaryBCFMap.entrySet()){
	    for(Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment> bcfPair : entry.getValue()){
		//System.out.println("what is bcfPair from binary? " + bcfPair);
		for(ProductionTuple pt : bcfPair.getFirst().getTuples())
		    allHypoedFrags.add(pt);
		if(bcfPair.getSecond()==null) continue;
		for(ProductionTuple pt : bcfPair.getSecond().getTuples())
		    allHypoedFrags.add(pt);
		//check if the second one is just a preterminal
		List<ProductionTuple> bcfbottomtuples = bcfPair.getSecond().getTuples();
		if(bcfPair.getSecond().isPreterminal()){
		//if(bcfPair.getSecond().getRoot()!=null && bcfPair.getSecond().getRoot().isPreterminal()){
		    ProductionTuple pt = bcfbottomtuples.get(0);
		    easyPreterminalCouplingMap.add(bcfPair.getSecond());
		    //easyPreterminalCouplingMap.put(new Pair<Integer,Integer>(pt.getTag(0),pt.getSig(0)), bcfPair.getSecond());
		}
	    }
	}

	//System.out.println("ALL HYPOed FRAGs is \n:" + allHypoedFrags);
	//System.out.println("MAPPER IS : " + mapper);
	//System.out.println("NEW FRAGMENTS TO ADD: ");
	//System.out.println(unaryBCFMap);
	//System.out.println(binaryBCFMap);
	
	short[] newNumSubStates = new short[numSubStates.length];	
	double[][] parentTally = new double[numSubStates.length][];
	//System.out.println(tagNumberer);
	for(int i=0;i<numSigsPerTag.length;i++){
	    newNumSubStates[i]=numSigsPerTag[i];
	    System.err.println("newNumSubStates["+i+"]="+newNumSubStates[i]);
	    System.out.println("newNumSubStates["+i+"]="+newNumSubStates[i]);
	    parentTally[i] = new double[numSigsPerTag[i]];
	}
	System.err.println("============");
	

	//System.out.println("before creating new grammar, check: tagNumberer.size() = " + Numberer.getGlobalNumberer("tags").total() + " and newNumSubStates.length=" + newNumSubStates.length);
	//System.out.println("and why not, let's print the damn thing:\n"+Numberer.getGlobalNumberer("tags"));
	// create the new grammar
	Grammar grammar = new Grammar(newNumSubStates, findClosedPaths, smoother, this, threshold);

	FragmentProbabilityComputer fragmentPC = new FragmentProbabilityComputer(LS, unnormalizedSymbolCounter, gamma);
	List<UnaryRule> urList = new LinkedList<UnaryRule>();
	List<BinaryRule> brList = new LinkedList<BinaryRule>();

	for (UnaryRule oldRule : unaryRuleMap.keySet()) {
	    UnaryRule newRule;
	    short pstate = oldRule.getParentState();
	    short cstate = oldRule.getChildState();
	    newRule = new UnaryRule(pstate,cstate,newNumSubStates[pstate],newNumSubStates[cstate]);
	    newRule.initializeScoresAfterCoupling(oldRule.getScores2(), parentTally, fragmentPC,startingFragmentMap, unaryBCFMap.get(oldRule), allHypoedFrags);
	    urList.add(newRule);
	    //System.out.println("adding new unary rule:\n"+oldRule+",\n"+newRule);
	}

	System.out.println("===================\n");

	InternalNodeSet.printJustAdded();

	for (BinaryRule oldRule : binaryRuleMap.keySet()) {
	    BinaryRule newRule;
	    //System.out.println("using oldRule="+oldRule+" as a guide...");
	    short pstate = oldRule.getParentState();
	    short lcstate = oldRule.getLeftChildState();
	    short rcstate = oldRule.getRightChildState();
	    newRule = new BinaryRule(pstate,lcstate,rcstate,newNumSubStates[pstate],newNumSubStates[lcstate],newNumSubStates[rcstate]);
	    newRule.initializeScoresAfterCoupling(oldRule.getScores2(), parentTally, fragmentPC,startingFragmentMap, binaryBCFMap.get(oldRule), allHypoedFrags);
	    brList.add(newRule);
	    //System.out.println("adding new binary rule:\n"+newRule);
	}

	for(int i=0;i<parentTally.length;i++){
	  for(int j=0;j<parentTally[i].length;j++)
	  System.out.println("parentTally["+i+"]["+j+"]="+parentTally[i][j]);
	  }

	for(UnaryRule unaryRule : urList){
	    int parentState = unaryRule.getParentState();
	    int nParentSubStates = numSigsPerTag[parentState];
	    int nChildStates = numSigsPerTag[unaryRule.childState];
     	    boolean allZero = true;
	    double[][] oldscores = unaryRule.getScores2();
	    //System.out.println("for the rule "+ unaryRule);
	    //System.out.println("scores were " + ArrayUtil.toString(oldscores));
	    for (int j = 0; j < nChildStates; j++) {
		if (oldscores[j]==null) continue;
		for (int i = 0; i < nParentSubStates; i++) {
		    if (parentTally[parentState][i]!=0){
			//System.out.println("unary rule, parentState = " + parentState + " and i=" + i + ", oldscore is : " + oldscores[j][i]);
			double nVal = (oldscores[j][i] / parentTally[parentState][i]);
			if (SloppyMath.isVeryDangerous(nVal)) nVal = 0;
			oldscores[j][i] = nVal;
		    }
		    allZero = allZero && (oldscores[j][i]==0);
		}
	    }
	    if (allZero){
		System.out.println("Maybe an underflow? Unary Rule: "+unaryRule+"\n"+ArrayUtil.toString(oldscores));
	    }
	    //System.out.println("\tand now they're "+ArrayUtil.toString(oldscores));
	    unaryRule.setScores2(oldscores);
	    grammar.addUnary(unaryRule);
	}
	urList=null;
	for(BinaryRule binaryRule : brList){
	    int parentState = binaryRule.getParentState();
	    int nParentSubStates = numSigsPerTag[parentState];
	    int nLChildStates = numSigsPerTag[binaryRule.leftChildState];
	    int nRChildStates = numSigsPerTag[binaryRule.rightChildState];
     	    boolean allZero = true;
	    double[][][] oldscores = binaryRule.getScores2();
	    for (int j = 0; j < nLChildStates; j++) {
		if (oldscores[j]==null) continue;
		for(int k = 0; k < nRChildStates; k++){
		    if(oldscores[j][k] == null) continue;
		    for (int i = 0; i < nParentSubStates; i++) {
			if (parentTally[parentState][i]!=0){
			    double nVal = (oldscores[j][k][i] / parentTally[parentState][i]);
			    if (SloppyMath.isVeryDangerous(nVal)) nVal = 0;
			    oldscores[j][k][i] = nVal;
			}
			allZero = allZero && (oldscores[j][k][i]==0);
		    }
		}
	    }
	    if (allZero){
		System.out.println("Maybe an underflow? Binary Rule: "+binaryRule+"\n"+ArrayUtil.toString(oldscores));
	    }
	    binaryRule.setScores2(oldscores);
	    grammar.addBinary(binaryRule);
	}
	brList=null;
	grammar.isGrammarTag = this.isGrammarTag;
	grammar.computePairsOfUnaries();

	mapper=null;
	bcfList=null;
	unaryBCFMap=null;
	binaryBCFMap=null;
	allHypoedFrags = null;
	
	System.gc();
	return grammar;
    }

    public void printUnaryRuleCount(){
	System.out.println("unary rule count");
	System.out.println("unaryRuleCounter: \n" + unaryRuleCounter);
    }


    private void constructInitialBCF(BerkeleyCompatibleFragment bcf, LinkedList<Pair<Integer,Integer>> Q){
	//System.out.println("passed in BCF="+bcf);
	while(!Q.isEmpty()){
	    //System.out.println("what is Q? " + Q);
	    ProductionTuple retPT = InternalNodeSet.pollForRule(Q.poll());
	    //if we don't find anything, then we're at the frontier
	    //System.out.println("what is retPT="+retPT);
	    if(retPT==null){
		//System.err.println("Whoops! We don't seem to have any rule that starts with "+state+" and substate="+substate);
		continue;
	    }
	    //iterate through production tuple
	    //set IDs and put children on Q if internal
	    if(!retPT.isPreterminal()){
		for(int tupleidx=1;tupleidx<retPT.getNumberOfNodes();tupleidx++){
		    Q.add(new Pair<Integer,Integer>(retPT.getTag(tupleidx), retPT.getSig(tupleidx)));
		}
	    } else{
			    
	    }
	    //add to bcf
	    retPT.overrideInternal(0,true);
	    bcf.addTuple(retPT);
	}			
	
	//System.out.println("WHAT IS BCF?? "+ bcf);
    }


    private Tree<TagSigOrWord> constructInitialBCF(Tree<TagSigOrWord> tree){
	constructInitialBCF(tree,null);
	return tree;
    }

    private Tree<TagSigOrWord> constructInitialBCF(Tree<TagSigOrWord> tree, Tree<String> strTree){
	//do some null tests
	//not sure I need this, but why not? 
	if(tree==null) return null;
	ArrayList<Tree<String>> newSChildren = new ArrayList<Tree<String>>();
	ArrayList<Tree<TagSigOrWord>> newChildren = new ArrayList<Tree<TagSigOrWord>>();
	Tree<String> nst=null;
	
	for(Tree<TagSigOrWord> child : tree.getChildren()){
	    //get the rule (from InternalNodeSet.pollForRule(...)) from this child
	    TagSigOrWord tsow = child.getLabel();
	    if(tsow.hasWord()){ //is preterminal
		newChildren.add(child);
		if(strTree!=null)
		    newSChildren.add(new Tree<String>(tsow.getWord()));
	    } else{
		if(strTree!=null){
		    int tag=tsow.getTag(); int sig = tsow.getSig();
		    String goalStr = tagNumberer.object(tag)+"_"+sig;
		    if(InternalNodeSet.isSubstateInternal(tag,sig))
			goalStr += "*";			
		    nst=new Tree<String>(goalStr);
		}
		ProductionTuple retPT = InternalNodeSet.pollForRule(tsow.getPair());
		//recur on that pt.toTree()
		if(retPT!=null){
		    newChildren.add(constructInitialBCF(retPT.toTree(), nst));
		    if(strTree!=null){
			newSChildren.add(nst);
		    }
		}
		else{
		    newChildren.add(child);
		    if(strTree!=null){
			newSChildren.add(nst);
		    }
		}
	    }
	}
	if(strTree!=null) strTree.setChildren(newSChildren);
	tree.setChildren(newChildren);
	return tree;
    }

    
    private void orderTreeNodes(Tree<TagSigOrWord> tree, int num){
	LinkedList<Tree<TagSigOrWord>> Q = new LinkedList<Tree<TagSigOrWord>>();
	Q.add(tree);
	while(!Q.isEmpty()){
	    Tree<TagSigOrWord> tmp = Q.poll();
	    tmp.setNodeNumber(num);
	    num++;
	    for(Tree<TagSigOrWord> c : tmp.getChildren())
		Q.add(c);
	}
    }

    /**
     * @param rule
     * @param ptList
     * @param newSignaturesToAdd
     * @param scoreThreshold
     * @param random
     * @param randomness
     * @param doNotNormalize
     * @param mode
     */
    private void hypothesizeCouplings(UnaryRule rule, Map<UnaryRule,Set<Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>>> bcfList, ConstraintSet constraintSet, Map<Vector<Integer>,Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>> startingFragmentMap, Set<String>/*Map<Pair<Integer,Integer>,Set<List<Pair<Integer,Integer>>>>*/ previousCouplings, short[] newSignaturesToAdd, short[] numSigsPerTag, Map<Pair<Integer,Integer>,Set<Integer>> mapper, LatentStatistics LS, double scoreThreshold, Random random, double randomness, boolean doNotNormalize, int mode, boolean useSubstates) {
	double[][] oldScores =  rule.getScores2();
	short parentState = rule.getParentState();
	short childState = rule.getChildState();

	Set<Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>> settemp = bcfList.get(rule);
	if(settemp==null) settemp = new HashSet<Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>>();

	//for all current substates
	for (short cS = 0; cS < oldScores.length; cS++) {
	    if (oldScores[cS]==null)
		continue;
		 
	    for (short pS = 0; pS < oldScores[cS].length; pS++) {
		double score = oldScores[cS][pS];
		if(score <= threshold) continue;
		//we need to construct a BCF in order to find out what this rule can couple with...
		BerkeleyCompatibleFragment bcf = new BerkeleyCompatibleFragment();
		//VERY IMPORTANT!!!
		//make sure the IDs are in toposort order, starting from 0
		ProductionTuple pt = new ProductionTuple(true);
		pt.addNT(0,parentState,pS, 0);
		pt.addNT(1,childState, cS, 1);
		bcf.addTuple(pt);
		//skip if this particular rule is internal
		if(InternalNodeSet.isSubstateInternal(parentState,pS)) {
		    settemp.add(new Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>(bcf,null));	       	
		    continue;
		}
		if(InternalNodeSet.isSubstateInternal(childState,cS)){
		    //add childState, cS to queue
		    Tree<TagSigOrWord> stT = constructInitialBCF(pt.toTree());
		    orderTreeNodes(stT, 0);
		    //System.out.println("Cool! we have a tree! "+ stT);
		    bcf = BerkeleyCompatibleFragment.createFromPairTree(stT);
		}
		//System.out.println("WHAT Is the new bcf? "+ bcf);

		//maybe move this down a bit???
		//and add it to the reverse map
		Vector<Integer> bcfVectorID = new Vector<Integer>(4);
		bcfVectorID.add(new Integer(parentState)); bcfVectorID.add(new Integer(pS));
		bcfVectorID.add(new Integer(childState)); bcfVectorID.add(new Integer(cS));
		startingFragmentMap.put(bcfVectorID, new Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>(bcf,null));
		//System.out.println("startingFragmentMap.put("+bcfVectorID+", "+new Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>(bcf,null)+")");
		
		settemp.add(new Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>(bcf,null));

		//System.out.println("for bcf="+bcf+", never getting past...");
		if(LS.getUnigramFragmentCount(bcf)==0) continue;
		//System.out.println("\there!");
		
		//make sure that bcf IDs are all unique
		int maxID_bcf = bcf.getMaximumID();

		//then check if this fragment (bcf) is in the upper portion for constraint set
		Tree<String> reconstructedTree = bcf.toCoarseTreeStringWRTTagger(tagNumberer,useSubstates);
		//System.out.println("reconstructed tree is " + reconstructedTree);
		Set<Pair<Tree<String>,Integer>> topCouplingSet = constraintSet.getTopCouplingSet(reconstructedTree);
		
		//if(bcf.getNumberOfProductions()>1)
		//System.out.println("Top coupling set of " + reconstructedTree + " ( " + score + ") is:" + topCouplingSet);
		for(Pair<Tree<String>,Integer> bottom : topCouplingSet){
		    //System.out.println("\t" + bottom);
		    //we need to check if this bottom fragment is available!!
		    int[] tagAndSig = bcf.getTagAndSigFromID(bottom.getSecond().intValue());
		    if(tagAndSig==null){
      			continue;
		    }
		    //get all possible BCFs from the bottom fragment
		    BerkeleyCompatibleFragment bottomBCF = constructAvailableBCFsFromFragment(bottom.getFirst().shallowCloneAndOrder(bottom.getSecond().intValue()), tagAndSig[0], tagAndSig[1],bottom.getSecond().intValue(), maxID_bcf+1, false, useSubstates);
		    //at this point in the grammar evolution, none of the bottom fragments are available for coupling
		    if(bottomBCF==null){// || possibleBottomBCFs!=null && possibleBottomBCFs.size()==0){   
			continue;
		    }
		    //System.out.println("from constructAvailable bottomBCF="+bottomBCF);
		    BerkeleyCompatibleFragment topBottom = BerkeleyCompatibleFragment.compose(bcf,bottomBCF);
		    Tree<String> tbString = topBottom.toTreeStringWRTTaggerNoLatentInternal(tagNumberer);
		    //System.out.println("ANd what is potential? " + tbString);
		    if(previousCouplings.contains(tbString.toString())){
			//System.out.println("catching "+tbString.toString()+" before hypothesizing a duplicate, with top="+bcf+", and bottom="+bottomBCF);
			continue;
		    }

		    //- rename all internal nodes of top
		    Pair<BerkeleyCompatibleFragment,Integer> renamedTopPair = renameInternal(bcf,bottom.getSecond().intValue(), numSigsPerTag,mapper,-1);
		    Pair<BerkeleyCompatibleFragment,Integer> renamedBottomPair = renameInternal(bottomBCF,bottom.getSecond().intValue(), numSigsPerTag,mapper,renamedTopPair.getSecond().intValue());
		    //put together the renamedTop and renamedBottom
		    settemp.add(new Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>(renamedTopPair.getFirst(),renamedBottomPair.getFirst()));

		    bcfVectorID = new Vector<Integer>(4);
		    BerkeleyCompatibleFragment tempTopbcf = renamedTopPair.getFirst();
		    bcfVectorID.add(new Integer(parentState)); bcfVectorID.add(tempTopbcf.getRoot().getSig(0));
		    bcfVectorID.add(new Integer(childState)); bcfVectorID.add(tempTopbcf.getRoot().getSig(1));


		    startingFragmentMap.put(bcfVectorID, new Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>(renamedTopPair.getFirst(),renamedBottomPair.getFirst()));
		    //System.out.println("previousCouplings.add("+tbString+") for the top="+renamedTopPair.getFirst()+" and bottom="+renamedBottomPair.getFirst());
		    previousCouplings.add(tbString.toString());
		}
	    }	    
	}
	bcfList.put(rule,settemp);
    }


    /**
     * @param rule
     * @param ptList
     * @param newSignaturesToAdd
     * @param scoreThreshold
     * @param random
     * @param randomness
     * @param doNotNormalize
     * @param mode
     */
    private void hypothesizeCouplings(BinaryRule rule, Map<BinaryRule,Set<Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>>> bcfList, ConstraintSet constraintSet, Map<Vector<Integer>,Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>> startingFragmentMap, Set<String>/*Map<Pair<Integer,Integer>,Set<List<Pair<Integer,Integer>>>>*/ previousCouplings, short[] newSignaturesToAdd, short[] numSigsPerTag, Map<Pair<Integer,Integer>,Set<Integer>> mapper, LatentStatistics LS, double scoreThreshold, Random random, double randomness, boolean doNotNormalize, int mode, boolean useSubstates) {
	double[][][] oldScores =  rule.getScores2();
	short parentState = rule.getParentState();
	short leftChildState = rule.getLeftChildState();
	short rightChildState = rule.getRightChildState();

	Set<Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>> settemp = bcfList.get(rule);
	if(settemp==null) settemp = new HashSet<Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>>();

	//for all current substates
	for (short lcS = 0; lcS < oldScores.length; lcS++) {
	    for (short rcS = 0; rcS < oldScores[lcS].length; rcS++) {
		if (oldScores[lcS][rcS]==null)
		    continue;
		 
		for (short pS = 0; pS < oldScores[lcS][rcS].length; pS++) {
		    double score = oldScores[lcS][rcS][pS];
		    if(score <= threshold) continue;
		    //we need to construct a BCF in order to find out what this rule can couple with...
		    BerkeleyCompatibleFragment bcf = new BerkeleyCompatibleFragment();
		    //VERY IMPORTANT!!!
		    //make sure the IDs are in toposort order, starting from 0
		    ProductionTuple pt = new ProductionTuple(false);
		    pt.addNT(0,parentState,pS, 0);
		    pt.addNT(1,leftChildState, lcS, 1);
		    pt.addNT(2,rightChildState, rcS, 2);
		    bcf.addTuple(pt);
		    //System.out.println("debug for sleepy people: "+ bcf);
		    //Ideally, we'd skip if this particular rule is internal
		    //But there's the case that the only thing that's keeping this
		    //rule ``template'' around is an internal production
		    if(InternalNodeSet.isSubstateInternal(parentState,pS)) {
			settemp.add(new Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>(bcf,null));
			continue;
		    }		    
		    //System.out.println("made it past 0");
		    if(InternalNodeSet.isSubstateInternal(leftChildState,lcS) ||
		       InternalNodeSet.isSubstateInternal(rightChildState,rcS)){
			Tree<TagSigOrWord> stT = constructInitialBCF(pt.toTree());
			orderTreeNodes(stT, 0);
			bcf = BerkeleyCompatibleFragment.createFromPairTree(stT);
		    }
		    //System.out.println("WHAT Is the new bcf? "+ bcf);

		    //and add it to the reverse map
		    Vector<Integer> bcfVectorID = new Vector<Integer>(6);
		    bcfVectorID.add(new Integer(parentState)); bcfVectorID.add(new Integer(pS));
		    bcfVectorID.add(new Integer(leftChildState)); bcfVectorID.add(new Integer(lcS));
		    bcfVectorID.add(new Integer(rightChildState)); bcfVectorID.add(new Integer(rcS));
		    startingFragmentMap.put(bcfVectorID, new Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>(bcf,null));

		    //System.out.println("startingFragmentMap.put("+bcfVectorID+", "+new Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>(bcf,null)+")");

		    settemp.add(new Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>(bcf,null));
		    //System.out.println("bcf="+bcf+" before conditional");
		    if(LS.getUnigramFragmentCount(bcf)==0){
			//if(bcf.getNumberOfProductions()>1)
			//    System.out.println("sads :(");
			continue;
		    }
		    //System.out.println("bcf="+bcf+" after conditional");
		    
		    //make sure that bcf IDs are all unique
		    int maxID_bcf = bcf.getMaximumID();

		    //then check if this fragment (bcf) is in the upper portion for constraint set
		    Tree<String> reconstructedTree = bcf.toCoarseTreeStringWRTTagger(tagNumberer,useSubstates);

		    Set<Pair<Tree<String>,Integer>> topCouplingSet = constraintSet.getTopCouplingSet(reconstructedTree);
		    //System.out.println("Top coupling set of tree="+reconstructedTree+" is:"+topCouplingSet);
		    for(Pair<Tree<String>,Integer> bottom : topCouplingSet){
			//System.out.println("\t" + bottom);
			//we need to check if this bottom fragment is available!!
			int[] tagAndSig = bcf.getTagAndSigFromID(bottom.getSecond().intValue());
			if(tagAndSig==null){
			    continue;
			}
			//get all possible BCFs from the bottom fragment
			BerkeleyCompatibleFragment bottomBCF = constructAvailableBCFsFromFragment(bottom.getFirst().shallowCloneAndOrder(bottom.getSecond().intValue()), tagAndSig[0], tagAndSig[1],bottom.getSecond().intValue(),  maxID_bcf+1, false, useSubstates);
			//at this point in the grammar evolution, none of the bottom fragments are available for coupling
			if(bottomBCF==null){// || possibleBottomBCFs!=null && possibleBottomBCFs.size()==0){	        
			    continue;
			}
			//System.out.println("from constructAvailable bottomBCF="+bottomBCF);

			BerkeleyCompatibleFragment topBottom = BerkeleyCompatibleFragment.compose(bcf,bottomBCF);
			Tree<String> tbString = topBottom.toTreeStringWRTTaggerNoLatentInternal(tagNumberer);
			//System.out.println("ANd what is potential? " + tbString);
			if(previousCouplings.contains(tbString.toString())){
			    //System.out.println("catching "+tbString.toString()+" before hypothesizing a duplicate, with top="+bcf+", and bottom="+bottomBCF);
			    continue;
			}
			//- rename all internal nodes of top
			Pair<BerkeleyCompatibleFragment,Integer> renamedTopPair = renameInternal(bcf,bottom.getSecond().intValue(), numSigsPerTag,mapper,-1);
			//System.out.println("renamed top is " + renamedTopPair);
			Pair<BerkeleyCompatibleFragment,Integer> renamedBottomPair = renameInternal(bottomBCF,bottom.getSecond().intValue(), numSigsPerTag,mapper,renamedTopPair.getSecond().intValue());
			//System.out.println("renamed bottom is " + renamedBottomPair);
			//put together the renamedTop and renamedBottom
			settemp.add(new Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>(renamedTopPair.getFirst(),renamedBottomPair.getFirst()));

			//and add it to the reverse map
			bcfVectorID = new Vector<Integer>(6);
			BerkeleyCompatibleFragment tempTopbcf = renamedTopPair.getFirst();
			bcfVectorID.add(new Integer(parentState)); bcfVectorID.add(tempTopbcf.getRoot().getSig(0));
			bcfVectorID.add(new Integer(leftChildState)); bcfVectorID.add(tempTopbcf.getRoot().getSig(1)); 
			bcfVectorID.add(new Integer(rightChildState)); bcfVectorID.add(tempTopbcf.getRoot().getSig(2));
			startingFragmentMap.put(bcfVectorID, new Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>(renamedTopPair.getFirst(),renamedBottomPair.getFirst()));
			previousCouplings.add(tbString.toString());
			//System.out.println("previousCouplings.add("+tbString+") for the top="+renamedTopPair.getFirst()+" and bottom="+renamedBottomPair.getFirst());
		    }
		    //System.out.println();
		}	    
	    }
	}
	bcfList.put(rule,settemp);
	//System.out.println("mapper is : "+mapper);
    }


    private BerkeleyCompatibleFragment constructAvailableBCFsFromFragment(Tree<String> fragment, int topLevelTag, int topLevelSig, int topLevelID, int offset, boolean changeThis, boolean useSubstates){
	//System.out.println("constructing for fragment="+fragment+", "+ topLevelTag+", "+ topLevelSig);
	//this probably isn't right...
	if(fragment.isLeaf()) return new BerkeleyCompatibleFragment();

	List<Tree<String>> children = fragment.getChildren();
	//I'd like to check if this is a preterminal...
	if(fragment.isPreTerminal()){
	    //...but that's not good enough: check if this unary rule is a rule in the grammar
	    int childTag = tagNumberer.containsNumber(coarsenAndRemoveSubstate(children.get(0).getLabel(),useSubstates))?0:-1;
	    if(childTag==-1){
		ProductionTuple pt = new ProductionTuple(true);
		pt.addPreterminal(topLevelTag,topLevelSig,topLevelID + (changeThis?offset:0), children.get(0).getLabel());
		return new BerkeleyCompatibleFragment(pt);
	    }
	}
	switch(children.size()){
	case 1:
	    String childsym = children.get(0).getLabel();
	    int childTag = tagNumberer.number(coarsenAndRemoveSubstate(childsym,useSubstates));
	    UnaryRule ur = unaryRuleMap.get(new UnaryRule((short)topLevelTag,(short)childTag));
	    if(ur==null) {return null;}
	    int childSig = getSubstate(childsym);//get this!!
	    //double[][] uscores = ur.getScores2();
	    //System.out.print("let's consider this unary rule... "+ur);
	    boolean childShouldBeInternal = children.get(0).getChildren().size()>0;
	    if(ur.getScore(topLevelSig,childSig)==0) return null;
	    
	    //check if child needs to be internal based on the tree, but
	    //we haven't actually verified it as being internal!
	    //in these cases, there's a mismatch between the fragment and the rule
	    //so exit with null when either the child isn't internal but it should be (shouldn't happen)
	    //or when the child should be internal but it isnt'
	    if(childShouldBeInternal ^ InternalNodeSet.isSubstateInternal(childTag,childSig)){
		return null;
	    }
	    BerkeleyCompatibleFragment childBCF = constructAvailableBCFsFromFragment(children.get(0), childTag, childSig, children.get(0).getNodeNumber(), offset, true, useSubstates);
	    if(childBCF==null){   
		return null;
	    }
	    ProductionTuple pt = new ProductionTuple(true);
	    pt.addNT(0,topLevelTag,topLevelSig,topLevelID  + (changeThis?offset:0));
	    pt.addNT(1,childTag,childSig,children.get(0).getNodeNumber() + offset);
	    BerkeleyCompatibleFragment ncbcf = childBCF.copyWithOffset(offset);
	    ncbcf.addTuple(pt);
	    return ncbcf;
	    //break;
	case 2:
	    int lchildTag = tagNumberer.number(coarsenAndRemoveSubstate(children.get(0).getLabel(),useSubstates));
	    int rchildTag = tagNumberer.number(coarsenAndRemoveSubstate(children.get(1).getLabel(),useSubstates));
	    BinaryRule br = binaryRuleMap.get(new BinaryRule((short)topLevelTag,(short)lchildTag, (short)rchildTag));
	    if(br==null) return null;
	    //System.out.println("grabbed binary rule : "+br);
	    boolean leftShouldBeInternal = children.get(0).getChildren().size()>0;
	    boolean rightShouldBeInternal = children.get(1).getChildren().size()>0;

	    int leftChildSig=getSubstate(children.get(0).getLabel()); int rightChildSig=getSubstate(children.get(1).getLabel());

	    if(br.getScore(topLevelSig,leftChildSig, rightChildSig)==0) return null;
	    //check if left (right) needs to be internal
	    //in either case, there's a mismatch between the fragment and the rule
	    //so exit with null
	    if(leftShouldBeInternal ^ InternalNodeSet.isSubstateInternal(lchildTag,leftChildSig))
		return null;
	    if(rightShouldBeInternal ^ InternalNodeSet.isSubstateInternal(rchildTag,rightChildSig))
		return null;
	    BerkeleyCompatibleFragment leftBCF = constructAvailableBCFsFromFragment(children.get(0), lchildTag, leftChildSig,children.get(0).getNodeNumber(), offset, true, useSubstates);
	    BerkeleyCompatibleFragment rightBCF = constructAvailableBCFsFromFragment(children.get(1), rchildTag, rightChildSig, children.get(1).getNodeNumber(), offset, true, useSubstates);
	    if(leftBCF==null || rightBCF==null) return null;
	    ProductionTuple binpt = new ProductionTuple(false);
	    binpt.addNT(0,topLevelTag,topLevelSig,topLevelID  + (changeThis?offset:0));
	    binpt.addNT(1,lchildTag,leftChildSig,children.get(0).getNodeNumber()+offset);
	    binpt.addNT(2,rchildTag,rightChildSig,children.get(1).getNodeNumber()+offset);
	    BerkeleyCompatibleFragment newbcf = leftBCF.copyWithOffset(offset);
	    for(ProductionTuple rpt : rightBCF.copyWithOffset(offset).getTuples())
		newbcf.addTuple(rpt);
	    newbcf.addTuple(binpt);
	    return newbcf;
	    //break;
	default:
	    throw new Error("malformed tree");
	}
    }

    //old one?
    private List<BerkeleyCompatibleFragment> constructAvailableBCFsFromFragment1(Tree<String> fragment, int topLevelTag, int topLevelSig, int topLevelID, int offset, boolean changeThis, boolean useSubstates){
	//System.out.println("constructing for fragment="+fragment+", "+ topLevelTag+", "+ topLevelSig);
	if(fragment.isLeaf()) return new LinkedList<BerkeleyCompatibleFragment>();
	List<Tree<String>> children = fragment.getChildren();
	//I'd like to check if this is a preterminal...
	if(fragment.isPreTerminal()){
	    //...but that's not good enough: check if this unary rule is a rule in the grammar
	    int childTag = tagNumberer.containsNumber(coarsenAndRemoveSubstate(children.get(0).getLabel(),useSubstates))?0:-1;
	    if(childTag==-1){
		ProductionTuple pt = new ProductionTuple(true);
		pt.addPreterminal(topLevelTag,topLevelSig,topLevelID + (changeThis?offset:0), children.get(0).getLabel());
		LinkedList<BerkeleyCompatibleFragment> list = new LinkedList<BerkeleyCompatibleFragment>();
		list.add(new BerkeleyCompatibleFragment(pt));
		return list;
	    }
	}
	switch(children.size()){
	case 1:
	    int childTag = tagNumberer.number(coarsenAndRemoveSubstate(children.get(0).getLabel(),useSubstates));
	    UnaryRule ur = unaryRuleMap.get(new UnaryRule((short)topLevelTag,(short)childTag));
	    if(ur==null) {return null;}
	    double[][] uscores = ur.getScores2();
	    //System.out.print("let's consider this unary rule... "+ur);
	    boolean childShouldBeInternal = children.get(0).getChildren().size()>0;
	    for(int cs=0;cs < uscores.length; cs++){
		//		System.out.println("why are we looping? "+ cs);
		if(uscores[cs]==null){
		    continue;
		}
		//check if child needs to be internal
		//in either case, there's a mismatch between the fragment and the rule
		//so exit with null
		if((!fragment.isPreTerminal() && childShouldBeInternal) ^ InternalNodeSet.isSubstateInternal(childTag,cs)){ 
		    return null;
		}
		List<BerkeleyCompatibleFragment> childBCF = constructAvailableBCFsFromFragment1(children.get(0), childTag, cs, children.get(0).getNodeNumber(), offset, true, useSubstates);
		if(childBCF==null){   
		    return null;
		}
		//in this case, the is leaf IN THE FRAGMENT
		//All preterminals should be caught above
		ProductionTuple pt = new ProductionTuple(true);
		pt.addNT(0,topLevelTag,topLevelSig,topLevelID  + (changeThis?offset:0));
		pt.addNT(1,childTag,cs,children.get(0).getNodeNumber() + offset);

		if(childBCF.size()==0){
		    LinkedList<BerkeleyCompatibleFragment> list = new LinkedList<BerkeleyCompatibleFragment>();
		    list.add(new BerkeleyCompatibleFragment(pt));
		    return list;
		} else{
		    LinkedList<BerkeleyCompatibleFragment> list = new LinkedList<BerkeleyCompatibleFragment>();
		    for(BerkeleyCompatibleFragment cbcf : childBCF){
			BerkeleyCompatibleFragment newbcf = cbcf.copyWithOffset(offset);
			newbcf.addTuple(pt);
			list.add(newbcf);
		    }
		    return list;
		} 
	    
	    }
	    break;
	case 2:
	    int lchildTag = tagNumberer.number(coarsenAndRemoveSubstate(children.get(0).getLabel(),useSubstates));
	    int rchildTag = tagNumberer.number(coarsenAndRemoveSubstate(children.get(1).getLabel(),useSubstates));
	    BinaryRule br = binaryRuleMap.get(new BinaryRule((short)topLevelTag,(short)lchildTag, (short)rchildTag));
	    if(br==null) return null;
	    //System.out.println("grabbed binary rule : "+br);
	    double[][][] scores = br.getScores2();
	    boolean leftShouldBeInternal = children.get(0).getChildren().size()>0;
	    boolean rightShouldBeInternal = children.get(1).getChildren().size()>0;
	    for(int lcs=0;lcs < scores.length; lcs++){
		for(int rcs=0;rcs < scores[lcs].length; rcs++){
		    //		    System.out.println("why are we looping? : " + topLevelSig + " " + lcs + " " + rcs);
		    if(scores[lcs][rcs]==null) {
			//System.out.println("here for lcs=" + lcs + ", rcs=" + rcs);
			//return null;
			continue;
		    }
		    //check if left (right) needs to be internal
		    //in either case, there's a mismatch between the fragment and the rule
		    //so exit with null
		    if(leftShouldBeInternal ^ InternalNodeSet.isSubstateInternal(lchildTag,lcs))
			return null;
		    if(rightShouldBeInternal ^ InternalNodeSet.isSubstateInternal(rchildTag,rcs))
			return null;
		    List<BerkeleyCompatibleFragment> leftBCF = constructAvailableBCFsFromFragment1(children.get(0), lchildTag, lcs,children.get(0).getNodeNumber(), offset, true, useSubstates);
		    List<BerkeleyCompatibleFragment> rightBCF = constructAvailableBCFsFromFragment1(children.get(1), rchildTag, rcs, children.get(1).getNodeNumber(), offset, true, useSubstates);
		    if(leftBCF==null || rightBCF==null) return null;
		    //in this case, the left and right children are both leaves IN THE FRAGMENT
		    //so simply consider (lcs X rcs)
		    ProductionTuple pt = new ProductionTuple(false);
		    pt.addNT(0,topLevelTag,topLevelSig,topLevelID  + (changeThis?offset:0));
		    pt.addNT(1,lchildTag,lcs,children.get(0).getNodeNumber()+offset);
		    pt.addNT(2,rchildTag,rcs,children.get(1).getNodeNumber()+offset);
		    if(leftBCF.size()==0 && rightBCF.size()==0){
			LinkedList<BerkeleyCompatibleFragment> list = new LinkedList<BerkeleyCompatibleFragment>();
			list.add(new BerkeleyCompatibleFragment(pt));
			return list;
		    } else if(leftBCF.size()==0 && rightBCF.size()!=0){
			LinkedList<BerkeleyCompatibleFragment> list = new LinkedList<BerkeleyCompatibleFragment>();
			for(BerkeleyCompatibleFragment rbcf : rightBCF){
			    BerkeleyCompatibleFragment newbcf = rbcf.copyWithOffset(offset);
			    newbcf.addTuple(pt);
			    list.add(newbcf);
			}
			return list;
		    } else if(leftBCF.size()>0 && rightBCF.size()==0){
			LinkedList<BerkeleyCompatibleFragment> list = new LinkedList<BerkeleyCompatibleFragment>();
			for(BerkeleyCompatibleFragment lbcf : leftBCF){
			    BerkeleyCompatibleFragment newbcf = lbcf.copyWithOffset(offset);
			    newbcf.addTuple(pt);
			    list.add(newbcf);
			}
			return list;
		    } else{
			LinkedList<BerkeleyCompatibleFragment> list = new LinkedList<BerkeleyCompatibleFragment>();
			for(BerkeleyCompatibleFragment lbcf : leftBCF){
			    BerkeleyCompatibleFragment newbcf = lbcf.copyWithOffset(offset);
			    for(BerkeleyCompatibleFragment rbcf : rightBCF){
				for(ProductionTuple rpt : rbcf.copyWithOffset(offset).getTuples())
				    newbcf.addTuple(rpt);
				newbcf.addTuple(pt);
				list.add(newbcf);
			    }
			}
			return list;
		    }
		}
	    }
	    break;
	default:
	    throw new Error("malformed tree");
	}
	return null;
    }

    private int getSubstate(String s){
	int idx=s.lastIndexOf('_');
	int edx=s.lastIndexOf('*');
	return Integer.parseInt(s.substring((idx==-1 || idx>=s.length()-1)?0:idx+1, edx==-1?s.length():edx));
    }

    private String coarsenAndRemoveSubstate(String s, boolean us){
	int idx=s.lastIndexOf('_');
	return us?(s.substring(0,idx==-1?s.length():idx)):s;
    }

    private Pair<BerkeleyCompatibleFragment,Integer> renameInternal(BerkeleyCompatibleFragment bcf, int idOfShared, short[] numSigsPerTag, Map<Pair<Integer,Integer>,Set<Integer>> mapper, int overrideID){
	boolean debug=false;
	if(debug)
	    System.out.println("in renameInternal, bcf="+bcf);
	BerkeleyCompatibleFragment renamedBCF= new BerkeleyCompatibleFragment();
	int idToReturn = -1;
	Map<Pair<Integer,Integer>,Integer> seenNTs = new HashMap<Pair<Integer,Integer>,Integer>();
	for(ProductionTuple pt : bcf.getTuples()){
	    ProductionTuple copyPT = pt.copy();
	    if(pt.isPreterminal()){
		if(InternalNodeSet.isSubstateInternal(pt.getTag(0),pt.getSig(0)) || pt.getID(0)==idOfShared){
		    int newID;
		    if(seenNTs.containsKey(new Pair<Integer,Integer>(pt.getTag(0),pt.getSig(0))))
			newID = seenNTs.get(new Pair<Integer,Integer>(pt.getTag(0),pt.getSig(0)));
		    else{
			newID = (pt.getID(0)==idOfShared)?((overrideID==-1)?(numSigsPerTag[pt.getTag(0)]++):overrideID):(numSigsPerTag[pt.getTag(0)]++);
			seenNTs.put(new Pair<Integer,Integer>(pt.getTag(0),pt.getSig(0)),newID);
		    }
		    copyPT.addPreterminal((short)pt.getTag(0), (short)newID, pt.getWord());
		    Pair<Integer,Integer> pair = new Pair<Integer,Integer>(pt.getTag(0),pt.getSig(0));
		    Set<Integer> set = mapper.get(pair);
		    if(set==null) set= new HashSet<Integer>();
		    set.add(new Integer(newID));
		    if(pt.getID(0)==idOfShared)
			idToReturn = newID;
		    InternalNodeSet.addInternal(pt.getTag(0),newID,pt.getTag(0),pt.getSig(0));
		    if(debug)
			System.out.println("mapping "+ pt.getTag(0) +", "+ pt.getSig(0) + " to " + newID+" (preterminal part)");
		    mapper.put(pair, set);
		}
	    } else{
		for(int i = 0;i<pt.getNumberOfNodes(); i++){
		    //if it's internal, then rename
		    if(InternalNodeSet.isSubstateInternal(pt.getTag(i), pt.getSig(i)) || pt.getID(i)==idOfShared){
			int newID;
			if(seenNTs.containsKey(new Pair<Integer,Integer>(pt.getTag(i),pt.getSig(i))))
			    newID = seenNTs.get(new Pair<Integer,Integer>(pt.getTag(i),pt.getSig(i)));
			else{
			    newID = (pt.getID(i)==idOfShared)?((overrideID==-1)?(numSigsPerTag[pt.getTag(i)]++):overrideID):(numSigsPerTag[pt.getTag(i)]++);			
			    seenNTs.put(new Pair<Integer,Integer>(pt.getTag(i),pt.getSig(i)),newID);
			}
			copyPT.addNT(i, pt.getTag(i), newID, pt.getID(i));
			Pair<Integer,Integer> pair = new Pair<Integer,Integer>(pt.getTag(i),pt.getSig(i));
			Set<Integer> set = mapper.get(pair);
			if(set==null) set= new HashSet<Integer>();
			set.add(new Integer(newID));
			if(pt.getID(i)==idOfShared)
			    idToReturn = newID;
			
			InternalNodeSet.addInternal(pt.getTag(i),newID,pt.getTag(i),pt.getSig(i));
			if(debug)
			    System.out.println("mapping "+ pt.getTag(i) +", "+ pt.getSig(i) + " to " + newID + " (and did ID match?) "+ (pt.getID(i)==idOfShared)); 
			mapper.put(pair, set);
		    }
		    //otherwise, don't change anything
		}
	    }
	    if(InternalNodeSet.isSubstateInternal(pt.getTag(0),pt.getSig(0)) || pt.getID(0)==idOfShared){
		//System.out.println("adding " + new Pair<Integer,Integer>(pt.getTag(0),copyPT.getSig(0))+" and "+copyPT + " to ptMap");
		InternalNodeSet.addToPTMap(new Pair<Integer,Integer>(pt.getTag(0),copyPT.getSig(0)),copyPT);
	    }
	    renamedBCF.addTuple(copyPT);
	}
	if(debug)
	    System.out.println("\tand renamedFragment = "+renamedBCF);
	return new Pair<BerkeleyCompatibleFragment,Integer>(renamedBCF,new Integer(idToReturn));
    }
    
    @SuppressWarnings("unchecked")
	public void extendSplitTrees(Tree<Short>[] trees, int[] offset, short[] oldNumSubStates) {
	this.splitTrees = new Tree[numStates];
	for (int tag=0; tag<splitTrees.length; tag++) {
	    Tree<Short> splitTree = trees[tag].shallowClone();
	    for (Tree<Short> leaf : splitTree.getTerminals()) {
		List<Tree<Short>> children = leaf.getChildren();
		//take into account internal nodes!!!
		if (numSubStates[tag] > oldNumSubStates[tag] && leaf.getLabel() < offset[tag]) {
		    System.out.println("for tag="+tag+", label="+leaf.getLabel());
		    children.add(new Tree<Short>((short)(2*leaf.getLabel())));
		    children.add(new Tree<Short>((short)(2*leaf.getLabel()+1)));
		} else {
		    if(leaf.getLabel() < offset[tag]){
			System.out.println("for tag="+tag+", label="+leaf.getLabel() + " from extendSplitTrees::else clause");
			children.add(new Tree<Short>(leaf.getLabel()));
		    }
		}
	    }
	    //System.out.println("tree for tag="+tag+" is "+splitTree);
	    this.splitTrees[tag] = splitTree;
	}
    }
	
    public int totalSubStates() {
	int count = 0;
	for (int i = 0; i < numStates; i++) {
	    count += numSubStates[i];
	}
	return count;
    }
	
    /**
     * Tally the probability of seeing each substate. This data is needed for
     * tallyMergeScores. mergeWeights is indexed as [state][substate].  This
     * data should be normalized before being used by another function.
     * 
     * @param tree
     * @param mergeWeights The probability of seeing substate given state.
     */
    public void tallyMergeWeights(Tree<StateSet> tree, double mergeWeights[][]) {
	if (tree.isLeaf())
	    return;
	StateSet label = tree.getLabel();
	short state = label.getState();
	double probs[] = new double[label.numSubStates()];
	double total = 0, tmp;
	for (short i=0; i<label.numSubStates(); i++) {
	    tmp = label.getIScore(i) * label.getOScore(i);
	    // TODO: put in the scale parameters???
	    probs[i] = tmp;
	    total += tmp;
	}
	if (total==0) 
	    total = 1;
	for (short i=0; i<label.numSubStates(); i++) {
	    mergeWeights[state][i] += probs[i]/total;
	}
	for (Tree<StateSet> child : tree.getChildren()) {
	    tallyMergeWeights(child,mergeWeights);
	}
    }
	
    /*
     * normalize merge weights. assumes that the mergeWeights are given 
     * as logs. the normalized weights are returned as probabilities. 
     */
    public void normalizeMergeWeights(double[][] mergeWeights){
	for (int state=0; state<mergeWeights.length; state++) {
	    double sum=0;
	    for (int subState=0; subState<numSubStates[state]; subState++) {
		sum += mergeWeights[state][subState];
	    }
	    if (sum==0)
		sum = 1;
	    for (int subState=0; subState<numSubStates[state]; subState++) {
		mergeWeights[state][subState] /= sum;
	    }
	}
    }

	
	
    /**
     * Calculate the log likelihood gain of merging pairs of split states
     * together.  This information is returned in deltas[state][merged substate].
     * It requires mergeWeights to be calculated by tallyMergeWeights.
     * 
     * @param tree
     * @param deltas The log likelihood gained by merging pairs of substates.
     * @param mergeWeights The probability of seeing substate given state.
     */
    public void tallyMergeScores(Tree<StateSet> tree, double[][][] deltas,
				 double[][] mergeWeights) {
	if (tree.isLeaf())
	    return;
	StateSet label = tree.getLabel();
	short state = label.getState();
	double[] separatedScores = new double[label.numSubStates()];
	double[] combinedScores = new double[label.numSubStates()];
	double combinedScore;
	// calculate separated scores
		
	double separatedScoreSum = 0, tmp;
	//don't need to deal with scale factor because we divide below
	for (int i = 0; i < label.numSubStates(); i++) {
	    tmp = label.getIScore(i) * label.getOScore(i);
	    combinedScores[i] = separatedScores[i] = tmp;
	    separatedScoreSum += tmp;
	}
	// calculate merged scores
	for (short i = 0; i < numSubStates[state]; i++) {
	    for (short j=(short)(i+1); j<numSubStates[state]; j++) {
		short[] map = new short[2];
		map[0] = i;
		map[1] = j;
		double[] tmp1 = new double[2], tmp2 = new double[2];
		double mergeWeightSum = 0;
		for (int k=0; k<2; k++) {
		    mergeWeightSum += mergeWeights[state][map[k]];
		}
		if (mergeWeightSum==0)
		    mergeWeightSum = 1;
		for (int k=0; k<2; k++) {
		    tmp1[k] = label.getIScore(map[k])*mergeWeights[state][map[k]]/mergeWeightSum;
		    tmp2[k] = label.getOScore(map[k]);
		}
		combinedScore = (tmp1[0]+tmp1[1]) * (tmp2[0]+tmp2[1]);
		combinedScores[i] = combinedScore;
		combinedScores[j] = 0;
		if (combinedScore!=0 && separatedScoreSum!=0)
		    deltas[state][i][j] += Math.log(separatedScoreSum/ArrayUtil.sum(combinedScores));
		for (int k=0; k<2; k++)
		    combinedScores[map[k]] = separatedScores[map[k]]; 
		if (Double.isNaN(deltas[state][i][j])) {
		    System.out.println(" deltas["+tagNumberer.object(state)+"]["+i+"]["+j+"] = NaN");
		    System.out.println(Arrays.toString(separatedScores) + " "
				       + Arrays.toString(tmp1) + " " + Arrays.toString(tmp2) + " "
				       + combinedScore+" "+Arrays.toString(mergeWeights[state]));
		}
	    }
	}
			
	for (Tree<StateSet> child : tree.getChildren()) {
	    tallyMergeScores(child, deltas, mergeWeights);
	}
    }
	
    /**
     * This merges the substate pairs indicated by mergeThesePairs[state][substate pair].
     * It requires merge weights calculated by tallyMergeWeights. 
     * 
     * @param mergeThesePairs  Which substate pairs to merge.
     * @param mergeWeights     The probability of seeing each substate.
     */
    public Grammar mergeStates(boolean[][][] mergeThesePairs, double[][] mergeWeights) {
	if (logarithmMode) {
	    throw new Error("Do not merge grammars in logarithm mode!");
	}
	short[] newNumSubStates = new short[numSubStates.length];
	short[][] mapping = new short[numSubStates.length][];
	//invariant: if partners[state][substate][0] == substate, it's the 1st one
	short[][][] partners = new short[numSubStates.length][][];
	calculateMergeArrays(mergeThesePairs, newNumSubStates, mapping, partners, numSubStates);
	// create the new grammar
	Grammar grammar = new Grammar(newNumSubStates, findClosedPaths, smoother, this, threshold);
	for (BinaryRule oldRule : binaryRuleMap.keySet()) {
	    short pS = oldRule.getParentState(), lcS = oldRule.getLeftChildState(), rcS = oldRule.getRightChildState();
	    double[][][] oldScores = oldRule.getScores2();
	    //merge binary rule
	    double[][][] newScores = new double[newNumSubStates[lcS]][newNumSubStates[rcS]][newNumSubStates[pS]];
	    for (int i=0; i<numSubStates[pS]; i++) {
		if (partners[pS][i][0]==i) {
		    int parentSplit = partners[pS][i].length;
		    for (int j=0; j<numSubStates[lcS]; j++) {
			if (partners[lcS][j][0]==j) {
			    int leftSplit = partners[lcS][j].length;
			    for (int k=0; k<(numSubStates[rcS]); k++) {
				if (partners[rcS][k][0]==k) {
				    int rightSplit = partners[rcS][k].length;
				    double[][][] scores = new double[leftSplit][rightSplit][parentSplit];
				    for (int js=0; js<leftSplit; js++) {
					for (int ks=0; ks<rightSplit; ks++) {
					    if (oldScores[partners[lcS][j][js]][partners[rcS][k][ks]] == null)
						continue;
					    for (int is=0; is<parentSplit; is++) {
						scores[js][ks][is] = oldScores[partners[lcS][j][js]]
						    [partners[rcS][k][ks]]
						    [partners[pS][i][is]];
					    }
					}
				    }
				    if (rightSplit==2) {
					for (int is=0; is<parentSplit; is++) {
					    for (int js=0; js<leftSplit; js++) {
						scores[js][0][is] = scores[js][1][is] = 
						    scores[js][0][is] + scores[js][1][is];
					    }
					}								
				    }
				    if (leftSplit==2) {
					for (int is=0; is<parentSplit; is++) {
					    for (int ks=0; ks<rightSplit; ks++) {
						scores[0][ks][is] = scores[1][ks][is] = 
						    scores[0][ks][is] + scores[1][ks][is];
					    }
					}								
				    }
				    if (parentSplit==2){
					for (int js=0; js<leftSplit; js++) {
					    for (int ks = 0; ks < rightSplit; ks++) {
						double mergeWeightSum = mergeWeights[pS][partners[pS][i][0]] + 
						    mergeWeights[pS][partners[pS][i][1]];
						if (SloppyMath.isDangerous(mergeWeightSum))
						    mergeWeightSum = 1;
						scores[js][ks][0] = scores[js][ks][1] = 
						    ((scores[js][ks][0]*mergeWeights[pS][partners[pS][i][0]]) + 
						     (scores[js][ks][1]*mergeWeights[pS][partners[pS][i][1]]))/mergeWeightSum;
					    }
					}								
				    }
				    for (int is=0; is < parentSplit; is++) {
					for (int js=0; js < leftSplit; js++) {
					    for (int ks=0; ks < rightSplit; ks++) {
						newScores[mapping[lcS][partners[lcS][j][js]]]
						    [mapping[rcS][partners[rcS][k][ks]]]
						    [mapping[pS][partners[pS][i][is]]] =
						    scores[js][ks][is];
					    }
					}
				    }
				}
			    }
			}
		    }
		}
	    }
	    BinaryRule newRule = new BinaryRule(oldRule);
	    newRule.setScores2(newScores);
	    grammar.addBinary(newRule);
	}
	for (UnaryRule oldRule : unaryRuleMap.keySet()) {
	    short pS = oldRule.getParentState(), cS = oldRule.getChildState();
	    // merge unary rule
	    double[][] newScores = new double[newNumSubStates[cS]][newNumSubStates[pS]];
	    double[][] oldScores = oldRule.getScores2();
	    boolean allZero = true;
	    for (int i=0; i<numSubStates[pS]; i++) {
		if (partners[pS][i][0]==i) {
		    int parentSplit = partners[pS][i].length;
		    for (int j = 0; j < numSubStates[cS]; j++) {
			if (partners[cS][j][0]==j) {
			    int childSplit = partners[cS][j].length;
			    double[][] scores = new double[childSplit][parentSplit];
			    for (int js = 0; js < childSplit; js++) {
				if (oldScores[partners[cS][j][js]] == null)
				    continue;
				for (int is = 0; is < parentSplit; is++) {
				    scores[js][is] = oldScores[partners[cS][j][js]]
					[partners[pS][i][is]];
				}
			    }
			    if (childSplit==2) {
				for (int is = 0; is < parentSplit; is++) {
				    scores[0][is] = scores[1][is] = scores[0][is] + scores[1][is];
				}
			    }
			    if (parentSplit==2) {
				for (int js = 0; js < childSplit; js++) {
				    double mergeWeightSum = mergeWeights[pS][partners[pS][i][0]]
					+ mergeWeights[pS][partners[pS][i][1]];
				    if (SloppyMath.isDangerous(mergeWeightSum))
					mergeWeightSum = 1;
				    scores[js][0] = scores[js][1] = ((scores[js][0] * mergeWeights[pS][partners[pS][i][0]]) + (scores[js][1] * mergeWeights[pS][partners[pS][i][1]])) / mergeWeightSum;
				}
			    }
			    for (int is = 0; is < parentSplit; is++) {
				for (int js = 0; js < childSplit; js++) {
				    newScores[mapping[cS][partners[cS][j][js]]]
					[mapping[pS][partners[pS][i][is]]]
					= scores[js][is];
				    allZero = allZero && (scores[js][is]==0);
				}
			    }
			}
		    }
		}
	    }
	    UnaryRule newRule = new UnaryRule(oldRule);
	    newRule.setScores2(newScores);
	    grammar.addUnary(newRule);
	}
	//System.out.println("What is grammar prior to pruneSplitTree(..)?\n"+grammar);
	grammar.pruneSplitTree(partners, mapping);
	grammar.isGrammarTag = this.isGrammarTag;
	grammar.closedSumRulesWithParent = grammar.closedViterbiRulesWithParent = grammar.unaryRulesWithParent;
	grammar.closedSumRulesWithChild = grammar.closedViterbiRulesWithChild = grammar.unaryRulesWithC;

	return grammar;
    }
	
    /**
     * @param mergeThesePairs
     * @param partners
     */
    private void pruneSplitTree(short[][][] partners, short[][] mapping) {
	for (int tag=0; tag<splitTrees.length; tag++) {
	    Tree<Short> splitTree = splitTrees[tag];
	    int maxDepth = splitTree.getDepth();
	    for (Tree<Short> preTerminal : splitTree.getAtDepth(maxDepth-2)) {
		List<Tree<Short>> children = preTerminal.getChildren();
		ArrayList<Tree<Short>> newChildren = new ArrayList<Tree<Short>>(2);
		for (int i=0; i<children.size(); i++) {
		    Tree<Short> child = children.get(i);
		    int curLoc = child.getLabel();
		    //System.out.println("tag="+tag+", curLoc="+curLoc);
		    if (partners[tag][curLoc][0]==curLoc) {
			newChildren.add(new Tree<Short>(mapping[tag][curLoc]));
		    }
		}
		preTerminal.setChildren(newChildren);
	    }
	}
    }

    public static void checkNormalization(Grammar grammar) {
	double[][] psum = new double[grammar.numSubStates.length][];
	for (int pS=0; pS<grammar.numSubStates.length; pS++) {
	    psum[pS] = new double[grammar.numSubStates[pS]];
	}
	boolean[] sawPS = new boolean[grammar.numSubStates.length];
	for (UnaryRule ur : grammar.unaryRuleMap.values()) {
	    int pS = ur.getParentState();
	    sawPS[pS]=true;
	    int cS = ur.getChildState();
	    double[][] scores = ur.getScores2();
	    for (int ci=0; ci<grammar.numSubStates[cS]; ci++) {
		if (scores[ci]==null)
		    continue;
		for (int pi=0; pi<grammar.numSubStates[pS]; pi++) {
		    psum[pS][pi] += scores[ci][pi];
		}
	    }
	}
	for (BinaryRule br : grammar.binaryRuleMap.values()) {
	    int pS = br.getParentState();
	    sawPS[pS]=true;
	    int lcS = br.getLeftChildState();
	    int rcS = br.getRightChildState();
	    double[][][] scores = br.getScores2();
	    for (int lci=0; lci<grammar.numSubStates[lcS]; lci++) {
		for (int rci=0; rci<grammar.numSubStates[rcS]; rci++) {
		    if (scores[lci][rci]==null)
			continue;
		    for (int pi=0; pi<grammar.numSubStates[pS]; pi++) {
			psum[pS][pi] += scores[lci][rci][pi];
		    }
		}
	    }
	}
	System.out.println();
	System.out.println("Checking for substates whose probs don't sum to 1");
	for (int pS=0; pS<grammar.numSubStates.length; pS++) {
	    if (!sawPS[pS])
		continue;
	    for (int pi=0; pi<grammar.numSubStates[pS]; pi++) {
		if (Math.abs(1-psum[pS][pi])>0.001)
		    System.out.println(" state "+pS+" substate "+pi+" gives bad psum: "+psum[pS][pi]);
	    }
	}
    }

    /**
     * @param mergeThesePairs
     * @param newNumSubStates
     * @param mapping
     * @param partners
     */
    public static void calculateMergeArrays(boolean[][][] mergeThesePairs,
					    short[] newNumSubStates, short[][] mapping, short[][][] partners,
					    short[] numSubStates) {
	for (short state = 0; state < numSubStates.length; state++) {
	    short mergeTarget[] = new short[mergeThesePairs[state].length];
	    Arrays.fill(mergeTarget,(short)-1);
	    short count = 0;
	    mapping[state] = new short[numSubStates[state]];
	    partners[state] = new short[numSubStates[state]][];
	    for (short j=0; j<numSubStates[state]; j++) {
		if (mergeTarget[j]!=-1) {
		    mapping[state][j] = mergeTarget[j];
		}
		else {
		    partners[state][j] = new short[1];
		    partners[state][j][0] = j;
		    //System.out.println("for state="+state+", substate (j) = " +j+", partners = " + partners[state][j][0]);
		    mapping[state][j] = count;
		    count++;
		    // assume we're only merging pairs, so we only see things to merge
		    // with this substate when this substate isn't being merged with anything
		    // earlier
		    for (short k=(short)(j+1); k<numSubStates[state]; k++) {
			if (mergeThesePairs[state][j][k]) {
			    mergeTarget[k] = mapping[state][j];
			    partners[state][j] = new short[2];
			    partners[state][j][0] = j;
			    partners[state][j][1] = k;
			    partners[state][k] = partners[state][j];
			}
		    }
		}
	    }
	    //System.out.println("We're calculating newNumSubStates["+state+"] = " + count);
	    newNumSubStates[state] = count;
	}
	newNumSubStates[0] = 1; // never split or merge ROOT
    }
	
    public void fixMergeWeightsEtc(boolean[][][] mergeThesePairs, double[][] mergeWeights, boolean[][][] complexMergePairs) {
	short[] newNumSubStates = new short[numSubStates.length];
	short[][] mapping = new short[numSubStates.length][];
	//invariant: if partners[state][substate][0] == substate, it's the 1st one
	short[][][] partners = new short[numSubStates.length][][];
	calculateMergeArrays(mergeThesePairs,newNumSubStates,mapping,partners,numSubStates);
	for (int tag=0; tag<numSubStates.length; tag++) {
	    double[] newMergeWeights = new double[newNumSubStates[tag]];
	    //System.out.println("in fixing part, newNumSubStates["+tag+"]="+newNumSubStates[tag]);
	    for (int i=0; i<numSubStates[tag]; i++) {
		newMergeWeights[mapping[tag][i]] += mergeWeights[tag][i];
	    }
	    mergeWeights[tag] = newMergeWeights;
			
	    boolean[][] newComplexMergePairs = new boolean[newNumSubStates[tag]][newNumSubStates[tag]];
	    boolean[][] newMergeThesePairs = new boolean[newNumSubStates[tag]][newNumSubStates[tag]];
	    for (int i=0; i<complexMergePairs[tag].length; i++) {
		for (int j=0; j<complexMergePairs[tag].length; j++) {
		    newComplexMergePairs[mapping[tag][i]][mapping[tag][j]] =
			newComplexMergePairs[mapping[tag][i]][mapping[tag][j]] || complexMergePairs[tag][i][j]; 
		    newMergeThesePairs[mapping[tag][i]][mapping[tag][j]] =
			newMergeThesePairs[mapping[tag][i]][mapping[tag][j]] || mergeThesePairs[tag][i][j]; 
		}
	    }
	    complexMergePairs[tag] = newComplexMergePairs;
	    mergeThesePairs[tag] = newMergeThesePairs;
	}
    }
	
    public void logarithmMode() {
	//System.out.println("The gramar is in logarithmMode!");
	if (logarithmMode)
	    return;
	logarithmMode = true;
	for (UnaryRule r : unaryRuleMap.keySet()) {
	    logarithmModeRule(unaryRuleMap.get(r));
	}
	for (BinaryRule r : binaryRuleMap.keySet()) {
	    logarithmModeRule(binaryRuleMap.get(r));
	}
	//Leon thinks the following sets of rules are already covered above,
	//but he wants to take no chances
	logarithmModeBRuleListArray(binaryRulesWithParent);
	logarithmModeBRuleListArray(binaryRulesWithLC);
	logarithmModeBRuleListArray(binaryRulesWithRC);
	logarithmModeBRuleArrayArray(splitRulesWithLC);
	logarithmModeBRuleArrayArray(splitRulesWithRC);
	logarithmModeBRuleArrayArray(splitRulesWithP);
	logarithmModeURuleListArray(unaryRulesWithParent);
	logarithmModeURuleListArray(unaryRulesWithC);
	logarithmModeURuleListArray(sumProductClosedUnaryRulesWithParent);
	logarithmModeURuleListArray(closedSumRulesWithParent);
	logarithmModeURuleListArray(closedSumRulesWithChild);
	logarithmModeURuleListArray(closedViterbiRulesWithParent);
	logarithmModeURuleListArray(closedViterbiRulesWithChild);
	logarithmModeURuleArrayArray(closedSumRulesWithP);
	logarithmModeURuleArrayArray(closedSumRulesWithC);
	logarithmModeURuleArrayArray(closedViterbiRulesWithP);
	logarithmModeURuleArrayArray(closedViterbiRulesWithC);
    }

    /**
     * 
     */
    private void logarithmModeBRuleListArray(List<BinaryRule>[] a) {
	if (a!=null) {
	    for (List<BinaryRule> l : a) {
		if (l==null) continue;
		for (BinaryRule r : l) {
		    logarithmModeRule(r);
		}
	    }
	}
    }

    /**
     * 
     */
    private void logarithmModeURuleListArray(List<UnaryRule>[] a) {
	if (a!=null) {
	    for (List<UnaryRule> l : a) {
		if (l==null) continue;
		for (UnaryRule r : l) {
		    logarithmModeRule(r);
		}
	    }
	}
    }

    /**
     * 
     */
    private void logarithmModeBRuleArrayArray(BinaryRule[][] a) {
	if (a!=null) {
	    for (BinaryRule[] l : a) {
		if (l==null) continue;
		for (BinaryRule r : l) {
		    logarithmModeRule(r);
		}
	    }
	}
    }

    /**
     * 
     */
    private void logarithmModeURuleArrayArray(UnaryRule[][] a) {
	if (a!=null) {
	    for (UnaryRule[] l : a) {
		if (l==null) continue;
		for (UnaryRule r : l) {
		    logarithmModeRule(r);
		}
	    }
	}
    }

    /**
     * @param r
     */
    private static void logarithmModeRule(BinaryRule r) {
	if (r==null || r.logarithmMode)
	    return;
	r.logarithmMode = true;
	double[][][] scores = r.getScores2();
	for (int i=0; i<scores.length; i++) {
	    for (int j=0; j<scores[i].length; j++) {
		if (scores[i][j]==null)
		    continue;
		for (int k=0; k<scores[i][j].length; k++) {
		    scores[i][j][k] = Math.log(scores[i][j][k]);
		}
	    }
	}
	r.setScores2(scores);
    }

    /**
     * @param r
     */
    private static void logarithmModeRule(UnaryRule r) {
	if (r==null || r.logarithmMode)
	    return;
	r.logarithmMode = true;
	double[][] scores = r.getScores2();
	for (int j=0; j<scores.length; j++) {
	    if (scores[j]==null)
		continue;
	    for (int k=0; k<scores[j].length; k++) {
		scores[j][k] = Math.log(scores[j][k]);
	    }
	}
	r.setScores2(scores);
    }

    public boolean isLogarithmMode() {
	return logarithmMode;
    }
	
    public final boolean isGrammarTag(int n){
	return isGrammarTag[n];
    }

    public Grammar projectGrammar(double[] condProbs, int[][] fromMapping, int[][] toSubstateMapping) {
	short[] newNumSubStates = new short[numSubStates.length];
	for (int state=0; state<numSubStates.length; state++){
	    newNumSubStates[state] = (short)toSubstateMapping[state][0];
	}

	Grammar grammar = new Grammar(newNumSubStates, findClosedPaths, smoother, this, threshold);
	for (BinaryRule oldRule : binaryRuleMap.keySet()) {
	    short pcS = oldRule.getParentState(), lcS = oldRule.getLeftChildState(), rcS = oldRule.getRightChildState();
	    double[][][] oldScores = oldRule.getScores2();
	    //merge binary rule
	    double[][][] newScores = new double[newNumSubStates[lcS]][newNumSubStates[rcS]][newNumSubStates[pcS]];
	    for (int lS=0; lS<numSubStates[lcS]; lS++){
		for (int rS=0; rS<numSubStates[rcS]; rS++){
		    if (oldScores[lS][rS]==null) continue;
		    for (int pS=0; pS<numSubStates[pcS]; pS++){
			newScores[toSubstateMapping[lcS][lS+1]]
			    [toSubstateMapping[rcS][rS+1]]
			    [toSubstateMapping[pcS][pS+1]] += 
			    condProbs[fromMapping[pcS][pS]]*oldScores[lS][rS][pS];
		    }
		}
	    }
	    BinaryRule newRule = new BinaryRule(oldRule,newScores);
	    grammar.addBinary(newRule);
	}
	for (UnaryRule oldRule : unaryRuleMap.keySet()) {
	    short pcS = oldRule.getParentState(), ccS = oldRule.getChildState();
	    double[][] oldScores = oldRule.getScores2();
	    double[][] newScores = new double[newNumSubStates[ccS]][newNumSubStates[pcS]];
	    for (int cS=0; cS<numSubStates[ccS]; cS++){
		if (oldScores[cS]==null) continue;
		for (int pS=0; pS<numSubStates[pcS]; pS++){
		    newScores[toSubstateMapping[ccS][cS+1]]
			[toSubstateMapping[pcS][pS+1]]
			+= condProbs[fromMapping[pcS][pS]]*oldScores[cS][pS];
		}
	    }
	    UnaryRule newRule = new UnaryRule(oldRule,newScores);
	    grammar.addUnary(newRule);
	    //			grammar.closedSumRulesWithParent[newRule.parentState].add(newRule);
	    //			grammar.closedSumRulesWithChild[newRule.childState].add(newRule);
	}

	grammar.computePairsOfUnaries();
	//		grammar.splitRules();
	grammar.makeCRArrays();
	grammar.isGrammarTag = this.isGrammarTag;
	//System.out.println(grammar.toString());
	return grammar;
    }	
	
    public Grammar copyGrammar(boolean noUnaryChains) {
	short[] newNumSubStates = numSubStates.clone();
		
	Grammar grammar = new Grammar(newNumSubStates, findClosedPaths, smoother, this, threshold);
	for (BinaryRule oldRule : binaryRuleMap.keySet()) {
	    BinaryRule newRule = new BinaryRule(oldRule);
	    grammar.addBinary(newRule);
	}
	for (UnaryRule oldRule : unaryRuleMap.keySet()) {
	    UnaryRule newRule = new UnaryRule(oldRule);
	    grammar.addUnary(newRule);
	}
	if (noUnaryChains) {
	    closedSumRulesWithParent = closedViterbiRulesWithParent = unaryRulesWithParent;
	    closedSumRulesWithChild = closedViterbiRulesWithChild = unaryRulesWithC;

	}
	else grammar.computePairsOfUnaries(); 
	grammar.makeCRArrays();
	grammar.isGrammarTag = this.isGrammarTag;
	/*
	  grammar.ruleIndexer = ruleIndexer;
	  grammar.startIndex = startIndex;
	  grammar.nEntries = nEntries;
	  grammar.toBeIgnored = toBeIgnored;*/
	return grammar;
    }	
	
    public Grammar projectTo0LevelGrammar(double[] condProbs, int[][] fromMapping, int[][] toMapping) {
	int newNumStates = fromMapping[fromMapping.length-1][0];
	// all rules have the same parent in this grammar
	double[][] newBinaryProbs = new double[newNumStates][newNumStates];
	double[] newUnaryProbs = new double[newNumStates];
		
	short[] newNumSubStates = new short[numSubStates.length];
	Arrays.fill(newNumSubStates,(short)1);
	Grammar grammar = new Grammar(newNumSubStates, findClosedPaths, smoother, this, threshold);
		
	//short[] newNumSubStates = new short[newNumStates];
	//grammar.numSubStates = newNumSubStates;
	//grammar.numStates = (short)newNumStates;
		
	for (BinaryRule oldRule : binaryRuleMap.keySet()) {
	    short pcS = oldRule.getParentState(), lcS = oldRule.getLeftChildState(), rcS = oldRule.getRightChildState();
	    double[][][] oldScores = oldRule.getScores2();
	    //merge binary rule
	    //double[][][] newScores = new double[1][1][1];
	    for (int lS=0; lS<numSubStates[lcS]; lS++){
		for (int rS=0; rS<numSubStates[rcS]; rS++){
		    if (oldScores[lS][rS]==null) continue;
		    for (int pS=0; pS<numSubStates[pcS]; pS++){
			newBinaryProbs[toMapping[lcS][lS]][toMapping[rcS][rS]] +=
			    //newBinaryProbs[lcS][rcS] +=
			    condProbs[fromMapping[pcS][pS]]*oldScores[lS][rS][pS];
		    }
		}
	    }
	    //BinaryRule newRule = new BinaryRule(oldRule);
	    //newRule.setScores2(newScores);
	    //grammar.addBinary(newRule);
	}
	for (UnaryRule oldRule : unaryRuleMap.keySet()) {
	    short pcS = oldRule.getParentState(), ccS = oldRule.getChildState();
	    double[][] oldScores = oldRule.getScores2();
	    for (int cS=0; cS<numSubStates[ccS]; cS++){
		if (oldScores[cS]==null) continue;
		for (int pS=0; pS<numSubStates[pcS]; pS++){
		    //newScores[0][0] += condProbs[fromMapping[pcS][pS]]*oldScores[cS][pS];
		    newUnaryProbs[toMapping[ccS][cS]] +=
			//newUnaryProbs[ccS] +=
			condProbs[fromMapping[pcS][pS]]*oldScores[cS][pS];

		}
	    }
	    //UnaryRule newRule = new UnaryRule(oldRule);
	    //newRule.setScores2(newScores);
	    //grammar.addUnary(newRule);
	    //			grammar.closedSumRulesWithParent[newRule.parentState].add(newRule);
	    //			grammar.closedSumRulesWithChild[newRule.childState].add(newRule);
	}
		
	for (short lS=0; lS<newBinaryProbs.length; lS++){
	    for (short rS=0; rS<newBinaryProbs.length; rS++){
		if (newBinaryProbs[lS][rS]>0){
		    double[][][] newScores = new double[1][1][1];
		    newScores[0][0][0] = newBinaryProbs[lS][rS];
		    BinaryRule newRule = new BinaryRule((short)0,lS,rS,newScores);
		    //newRule.setScores2(newScores);
		    grammar.addBinary(newRule);
		}
	    }
	}
		
	for (short cS=0; cS<newUnaryProbs.length; cS++){
	    if (newUnaryProbs[cS]>0){
		double[][] newScores = new double[1][1];
		newScores[0][0] = newUnaryProbs[cS];
		UnaryRule newRule = new UnaryRule((short)0,cS,newScores);
		//newRule.setScores2(newScores);
		grammar.addUnary(newRule);
	    }
	}

	grammar.computePairsOfUnaries();
	grammar.makeCRArrays();
	grammar.isGrammarTag = this.isGrammarTag;
	//System.out.println(grammar.toString());
	return grammar;
    }	
	
    public double[] computeConditionalProbabilities(int[][] fromMapping, int[][] toMapping) {
	double[][] transitionProbs = computeProductionProbabilities(fromMapping);
	//System.out.println(ArrayUtil.toString(transitionProbs));
	double[] expectedCounts = computeExpectedCounts(transitionProbs);
	//System.out.println(Arrays.toString(expectedCounts));
	/*for (int state=0; state<mapping.length-1; state++){
	  for (int substate=0; substate<mapping[state].length; substate++){
	  System.out.println((String)tagNumberer.object(state)+"_"+substate+" "+expectedCounts[mapping[state][substate]]);
	  }
	  }*/

	double[] condProbs = new double[expectedCounts.length]; 
	for (int projectedState=0; projectedState<toMapping[toMapping.length-1][0]; projectedState++){
	    double sum = 0;
	    for (int state=0; state<fromMapping.length-1; state++){
		for (int substate=0; substate<fromMapping[state].length; substate++){
		    if (toMapping[state][substate]==projectedState)
			sum += expectedCounts[fromMapping[state][substate]];
		}
	    }
	    for (int state=0; state<fromMapping.length-1; state++){
		for (int substate=0; substate<fromMapping[state].length; substate++){
		    if (toMapping[state][substate]==projectedState)
			condProbs[fromMapping[state][substate]] = 
			    expectedCounts[fromMapping[state][substate]]/sum;
		}
	    }
	}
	return condProbs;
    }

    public int[][] computeToMapping(int level, int[][] toSubstateMapping) {
	if (level==-1) return computeMapping(-1);
	short[] numSubStates = this.numSubStates;
	int[][] mapping = new int[numSubStates.length+1][];
	int k=0;
	for (int state=0; state<numSubStates.length; state++){
	    mapping[state] = new int[numSubStates[state]];
	    int oldVal = -1;
	    for (int substate=0; substate<numSubStates[state]; substate++){
		if (substate!=0 && oldVal != toSubstateMapping[state][substate+1]) k++;
		mapping[state][substate] = k;
		oldVal = toSubstateMapping[state][substate+1];
	    }
	    k++;
	}
	mapping[numSubStates.length] = new int[1];
	mapping[numSubStates.length][0]= k;
	//System.out.println("The merged grammar will have "+k+" substates.");
	return mapping;
    }
	
	
    public int[][] computeMapping(int level) {
	// level -1 -> 0-bar states
	// level 0 -> x-bar states
	// level 1 -> each (state,substate) gets its own index
	short[] numSubStates = this.numSubStates;
	int[][] mapping = new int[numSubStates.length+1][];
	int k=0;
	for (int state=0; state<numSubStates.length; state++){
	    mapping[state] = new int[numSubStates[state]];
	    Arrays.fill(mapping[state],-1);
	    //if (!grammar.isGrammarTag(state)) continue;
	    for (int substate=0; substate<numSubStates[state]; substate++){
		if (level>=1) mapping[state][substate]=k++;
		else if (level==-1){
		    if (this.isGrammarTag(state)) mapping[state][substate] = 0;
		    else mapping[state][substate]=state;
		} else /*level==0*/
		    mapping[state][substate] = state;
	    }
	}
	mapping[numSubStates.length] = new int[1];
	mapping[numSubStates.length][0]= (level<1) ? numSubStates.length : k;
	//System.out.println("The grammar has "+mapping[numSubStates.length][0]+" substates.");
	return mapping;
    }

    public int[][] computeSubstateMapping(int level) {
	// level 0 -> merge all substates
	// level 1 -> merge upto depth 1 -> keep upto 2 substates
	// level 2 -> merge upto depth 2 -> keep upto 4 substates
	short[] numSubStates = this.numSubStates;
	//		for (int i=0; i<numSubStates.length; i++)
	//			System.out.println(i+" "+numSubStates[i]+" "+splitTrees[i].toString());
	int[][] mapping = new int[numSubStates.length][];
	for (int state=0; state<numSubStates.length; state++){
	    mapping[state] = new int[numSubStates[state]+1];
	    int k=0;
	    if (level>=0){
		Arrays.fill(mapping[state],-1);
		Tree<Short> hierarchy = splitTrees[state];
		List<Tree<Short>> subTrees = hierarchy.getAtDepth(level);
		for (Tree<Short> subTree : subTrees){
		    List<Short> leaves = subTree.getYield();
		    for (Short substate : leaves){
			//						System.out.println(substate+" "+numSubStates[state]+" "+state);
			if (substate==numSubStates[state])
			    System.out.print("Will crash.");
			mapping[state][substate+1]=k;
		    }
		    k++;
		}
	    }
	    else {k=1;}
	    mapping[state][0]=k;
	}
	return mapping;
    }
	
    public void computeReverseSubstateMapping(int level, int[][] lChildMap, int[][] rChildMap) {
	// level 1 -> how do the states from depth 1 expand to depth 2
	for (int state=0; state<numSubStates.length; state++){
	    Tree<Short> hierarchy = splitTrees[state];
	    List<Tree<Short>> subTrees = hierarchy.getAtDepth(level);
	    lChildMap[state] = new int[subTrees.size()];
	    rChildMap[state] = new int[subTrees.size()];
	    for (Tree<Short> subTree : subTrees){
		int substate = subTree.getLabel();
		if (subTree.isLeaf()){
		    lChildMap[state][substate] = substate;
		    rChildMap[state][substate] = substate;
		    continue;
		}
		boolean first = true;
		int nChildren = subTree.getChildren().size();
		for (Tree<Short> child : subTree.getChildren()){
		    if (first) {
			lChildMap[state][substate] = child.getLabel();
			first = false;
		    }
		    else rChildMap[state][substate] = child.getLabel();
		    if (nChildren==1) rChildMap[state][substate] = child.getLabel();
		}
	    }
	}
    }

    private double[] computeExpectedCounts(double[][] transitionProbs) {
	//System.out.println(ArrayUtil.toString(transitionProbs));
	double[] expectedCounts = new double[transitionProbs.length];
	double[] tmpCounts = new double[transitionProbs.length];
	expectedCounts[0] = 1;
	tmpCounts[0] = 1;
	//System.out.print("Computing expected counts");
	int iter = 0;
	double diff = 1;
	double sum = 1;  // 1 for the root
	while (diff>1.0e-10 && iter<50){
	    iter++;
	    for (int state=1; state<expectedCounts.length; state++){
		for (int pState=0; pState<expectedCounts.length; pState++){
		    tmpCounts[state] += expectedCounts[pState]*transitionProbs[pState][state];
		}
				
	    }
	    diff = 0;
	    sum=1;
	    for (int state=1; state<expectedCounts.length; state++){
		//tmpCounts[state] /= sum; 
		diff += (Math.abs(expectedCounts[state]-tmpCounts[state]));
		expectedCounts[state] = tmpCounts[state];
		sum += tmpCounts[state];
		tmpCounts[state]=0;
	    }
	    expectedCounts[0] = 1;
	    tmpCounts[0] = 1;
	    //System.out.println(Arrays.toString(tmpCounts));
	    //System.out.println(diff);
	    //System.out.print(".");
	    //System.out.print(diff);
	}
	//System.out.println("done.\nExpected total count: "+sum);
	//System.out.println(Arrays.toString(expectedCounts));
	return expectedCounts;
	//System.out.println(grammar.toString());
    }

    private double[][] computeProductionProbabilities(int[][] mapping) {
	short[] numSubStates = this.numSubStates;
	int totalStates = mapping[numSubStates.length][0];
	// W_ij is the probability of state i producing state j
	double[][] W = new double[totalStates][totalStates];
		
	for (int state=0; state<numSubStates.length; state++){
	    //if (!grammar.isGrammarTag(state)) continue;
	    BinaryRule[] parentRules = this.splitRulesWithP(state);
	    for (int i = 0; i < parentRules.length; i++) {
		BinaryRule r = parentRules[i];
		int lState = r.leftChildState;
		int rState = r.rightChildState;
		/*				if (lState==15||rState==15){
						System.out.println("Found one");
						}*/
		double[][][] scores = r.getScores2();
		for (int lS=0; lS<numSubStates[lState]; lS++){
		    for (int rS=0; rS<numSubStates[rState]; rS++){
			if (scores[lS][rS]==null) continue;
			for (int pS=0; pS<numSubStates[state]; pS++){
			    W[mapping[state][pS]][mapping[lState][lS]] += scores[lS][rS][pS];
			    W[mapping[state][pS]][mapping[rState][rS]] += scores[lS][rS][pS];
			}
		    }
		}
	    }
	    List<UnaryRule> uRules = this.getUnaryRulesByParent(state);
	    for (UnaryRule r : uRules){
		int cState = r.childState;
		if (cState==state) continue;
		/*if (cState==15){
		  System.out.println("Found one");
		  }*/
		double[][] scores = r.getScores2();
		for (int cS=0; cS<numSubStates[cState]; cS++){
		    if (scores[cS]==null) continue;
		    for (int pS=0; pS<numSubStates[state]; pS++){
			W[mapping[state][pS]][mapping[cState][cS]] += scores[cS][pS];
		    }
		}
	    }
	}
	return W;
    }

    public void computeProperClosures(){
	int[][] map = new int[numStates][];
	int index = 0;
	for (int state=0; state<numStates; state++){
	    map[state] = new int[numSubStates[state]];
	    for (int substate=0; substate<numSubStates[state]; substate++){
		map[state][substate] = index++; 
	    }
	}
			
			
	double[][][] sumClosureMatrix = new double[10][index][index];
	// initialize
	for (int parentState=0; parentState<numStates; parentState++){
	    for (int i = 0; i < unaryRulesWithParent[parentState].size(); i++) {
		UnaryRule rule = unaryRulesWithParent[parentState].get(i);
		short childState = rule.getChildState();
		double[][] scores = rule.getScores2();
		for (int childSubState=0; childSubState<numSubStates[childState]; childSubState++){
		    if (scores[childSubState]==null) continue;
		    for (int parentSubState=0; parentSubState<numSubStates[parentState]; parentSubState++){
			sumClosureMatrix[0][map[parentState][parentSubState]][map[childState][childSubState]] = scores[childSubState][parentSubState];
		    }
		}
	    }
	}
	// now loop until convergence = length 10 for now
	for (int length=1; length<10; length++){
	    for (short interState=0; interState<numStates; interState++){
		for (int i = 0; i < unaryRulesWithParent[interState].size(); i++) {
		    UnaryRule rule = unaryRulesWithParent[interState].get(i);
		    short endState = rule.getChildState();
		    double[][] scores = rule.getScores2();
					
		    // loop over substates
		    for (int startState=0; startState<numStates; startState++){
			// we have a start and an end and need to loop over the intermediate state,substates
			for (int startSubState=0; startSubState<numSubStates[startState]; startSubState++){
			    for (int endSubState=0; endSubState<numSubStates[endState]; endSubState++){
				double ruleScore = 0;
				if (scores[endSubState]==null) continue;
				for (int interSubState=0; interSubState<numSubStates[interState]; interSubState++){
				    ruleScore += sumClosureMatrix[length-1][map[startState][startSubState]]
					[map[interState][interSubState]]*
					scores[endSubState][interSubState];
				}
				sumClosureMatrix[length][map[startState][startSubState]][map[endState][endSubState]]
				    += ruleScore;
			    }
			}
		    }
		}
	    }
	}
		
	// now sum up the paths of different lengths
	double[][] sumClosureScores = new double[index][index];
	for (int length=0; length<10; length++){
	    for (int startState=0; startState<index; startState++){
		for (int endState=0; endState<index; endState++){
		    sumClosureScores[startState][endState] += sumClosureMatrix[length][startState][endState];
		}
	    }
	}
		
	// reset the lists of unaries
	closedSumRulesWithParent = new List[numStates];
	closedSumRulesWithChild = new List[numStates];
	for (short startState=0; startState<numStates; startState++){
	    closedSumRulesWithParent[startState] = new ArrayList<UnaryRule>();
	    closedSumRulesWithChild[startState] = new ArrayList<UnaryRule>();
	}
		
	// finally create rules and add them to the arrays
	for (short startState=0; startState<numStates; startState++){
	    for (short endState=0; endState<numStates; endState++){
		if (startState==endState) continue;
		boolean atLeastOneNonZero = false;
		double[][] scores = new double[numSubStates[endState]][numSubStates[startState]];
		for (int startSubState=0; startSubState<numSubStates[startState]; startSubState++){
		    for (int endSubState=0; endSubState<numSubStates[endState]; endSubState++){
			double score = sumClosureScores[map[startState][startSubState]][map[endState][endSubState]];
			if (score>0){
			    scores[endSubState][startSubState]=score;
			    atLeastOneNonZero = true;
			}
		    }
		}
		if (atLeastOneNonZero){
		    UnaryRule newUnary = new UnaryRule(startState, endState, scores);
		    addUnary(newUnary);
		    closedSumRulesWithParent[startState].add(newUnary);
		    closedSumRulesWithChild[endState].add(newUnary);
		}
	    }
	}
	if (closedSumRulesWithP==null){
	    closedSumRulesWithP = new UnaryRule[numStates][];
	    closedSumRulesWithC = new UnaryRule[numStates][];
	}
	for (int i = 0; i < numStates; i++) {
	    closedSumRulesWithP[i] = (UnaryRule[]) closedSumRulesWithParent[i].toArray(new UnaryRule[0]);
	    closedSumRulesWithC[i] = (UnaryRule[]) closedSumRulesWithChild[i].toArray(new UnaryRule[0]);
	}

    }


    /**
     * @param output
     */
    public void writeSplitTrees(Writer w) {
	PrintWriter out = new PrintWriter(w);
	for (int state=1; state<numStates; state++){
	    String tag = (String)tagNumberer.object(state);
	    if (isGrammarTag[state] && tag.endsWith("^g")) tag = tag.substring(0, tag.length()-2);
	    out.write(tag+"\t"+splitTrees[state].toString()+"\n");
	}
	out.flush();
	out.close();
    }

	
	
	
}
