/**
 * 
 */
package edu.jhu.coe.PCFGLA;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.jhu.coe.PCFGLA.ArrayParser;
import edu.jhu.coe.PCFGLA.ConstraintSet;
import edu.jhu.coe.syntax.BerkeleyCompatibleFragment;
import edu.jhu.coe.PCFGLA.BCFTraversalContainer;
import edu.jhu.coe.syntax.ProductionTuple;
import edu.jhu.coe.syntax.StateSet;
import edu.jhu.coe.syntax.Tree;
import edu.jhu.coe.util.*;
import edu.jhu.coe.util.PriorityQueue;

import fig.basic.Pair;

import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Iterator;

import edu.jhu.coe.math.DoubleArrays;


/**
 * LatentStatistics calculates symbol, fragment and fragment
 * composition counts for a corpus.
 * 
 * @author Frank Ferraro
 * 
 */
public class LatentStatistics {
    short zero = 0, one = 1;

    private Numberer tagNumberer = Numberer.getGlobalNumberer("tags");

    private boolean USE_SUBSTATES=false;

    int[] counts;
    Collection<Tree<StateSet>> trees;
    Counter<UnaryRule> unaryRuleCounter;
    Counter<BinaryRule> binaryRuleCounter;
  
    int[] contexts;
    CounterMap<Integer,String> posCounter;
	
    Counter<BerkeleyCompatibleFragment> fragmentCounter;
    Counter<BerkeleyCompatibleFragment> fragmentUnigramCounter;
    Map<BerkeleyCompatibleFragment,Counter<BerkeleyCompatibleFragment>> fragmentInvalidBigramMap;
    Map<BerkeleyCompatibleFragment,Counter<BerkeleyCompatibleFragment>> fragmentValidBigramMap;
    Map<BerkeleyCompatibleFragment,Counter<BerkeleyCompatibleFragment>> validBottomSet;
    Map<BerkeleyCompatibleFragment,Counter<BerkeleyCompatibleFragment>> fragmentBigramMapForTops;
    Map<BerkeleyCompatibleFragment,Counter<BerkeleyCompatibleFragment>> fragmentBigramMapForBottoms;

    Counter<Pair<Integer,Integer>> rootCounter;
    Counter<Pair<Integer,Integer>> generalValidBottomCount;        
    ConstraintSet constraintSet;

    public LatentStatistics(ConstraintSet cs){
	fragmentCounter = new Counter<BerkeleyCompatibleFragment>();
	fragmentUnigramCounter = new Counter<BerkeleyCompatibleFragment>();
	rootCounter = new Counter<Pair<Integer,Integer>>();

	fragmentInvalidBigramMap = new HashMap<BerkeleyCompatibleFragment,Counter<BerkeleyCompatibleFragment>>();
	fragmentValidBigramMap = new HashMap<BerkeleyCompatibleFragment,Counter<BerkeleyCompatibleFragment>>();
	fragmentBigramMapForTops = new HashMap<BerkeleyCompatibleFragment,Counter<BerkeleyCompatibleFragment>>();
	fragmentBigramMapForBottoms = new HashMap<BerkeleyCompatibleFragment,Counter<BerkeleyCompatibleFragment>>();

	validBottomSet = new HashMap<BerkeleyCompatibleFragment,Counter<BerkeleyCompatibleFragment>>();
	generalValidBottomCount = new Counter<Pair<Integer,Integer>>();

	constraintSet = cs;
    }

    /**
     * Count statistics for a collection of StateSet trees.
     */
    /*public LatentStatistics(Numberer tagNumberer, Collection<Tree<StateSet>> trees) {
	counts = new int[tagNumberer.objects().size()];
	this.trees = trees;
	unaryRuleCounter = new Counter<UnaryRule>();
	binaryRuleCounter = new Counter<BinaryRule>();
	contexts = new int[tagNumberer.objects().size()];
	posCounter = new CounterMap<Integer,String>();
    }*/	
	
    public void countSymbols(){	
	for (Tree<StateSet> tree : trees) {
	    addCount(tree);
	}
    }

    private void addCount(Tree<StateSet> tree) {
	counts[tree.getLabel().getState()] += 1.0;
	if (!tree.isPreTerminal()) {
	    for (Tree<StateSet> child : tree.getChildren()) {
		addCount(child);
	    }
	}
    }

    /*
     * Counts how many different 'things' (non-terminals or terminals for the POS)
     * appear under a given nonterminal symbol.
     * Currently POS and other nonterminals are handled the same way.
     * We might to change that.
     */
	
    public void countRuleParents(){	
	for (Tree<StateSet> tree : trees) {
	    addParent(tree);
	}
	for (BinaryRule br : binaryRuleCounter.keySet()){
	    contexts[br.parentState]++;
	    contexts[br.leftChildState]++;
	    contexts[br.rightChildState]++;
	}
	for (UnaryRule ur : unaryRuleCounter.keySet()){
	    contexts[ur.parentState]++;
	    contexts[ur.childState]++;
	}
	for (int i=0; i<contexts.length; i++){
	    Counter<String> tempC = posCounter.getCounter(i);
	    contexts[i] += tempC.size();			
	}
    }

    public int[] getContextCounts(){
	return contexts;
    }
	
    private void addParent(Tree<StateSet> tree) {
	short parentState = tree.getLabel().getState();
	counts[parentState] += 1.0;
	if (!tree.isPreTerminal()) {
	    if (tree.getChildren().size() == 1) {
		UnaryRule r = new UnaryRule(parentState,tree.getChildren().get(0).getLabel().getState(),new double[1][1]);
		unaryRuleCounter.incrementCount(r, 1.0);
	    }
	    else {
		BinaryRule r = new BinaryRule(parentState,
					      tree.getChildren().get(0).getLabel().getState(),
					      tree.getChildren().get(1).getLabel().getState(),new double[1][1][1]);
		binaryRuleCounter.incrementCount(r, 1.0);
	    }
	    for (Tree<StateSet> child : tree.getChildren()) {
		addParent(child);
	    }
	}
	else {
	    posCounter.incrementCount((int)parentState,tree.getChildren().get(0).getLabel().getWord(),1.0);
	}
    }
	
	
    /** Get the number of times each state appeared.
     * 
     * @return
     */
    public int[] getSymbolCounts() {
	countSymbols();
	return counts;
    }
	
    /*public void printStateCountArray(Numberer tagNumberer, int[] array){
  	PriorityQueue<String> pq = new PriorityQueue<String>(array.length);
  	for (int i=0; i<array.length; i++){
	    pq.add((String)tagNumberer.object(i),array[i]);
	    //System.out.println(i+". "+(String)tagNumberer.object(i)+"\t "+symbolCounter.getCount(i,0));
  	}
  	int i = 0;
  	while(pq.hasNext()){
	    i++;
	    int p = (int)pq.getPriority();
	    System.out.println(i+". "+pq.next()+"\t "+p);
  	}
    }*/

    public void updateFragmentCounts(){
	
	for(Tree<StateSet> tree : trees){
	    LinkedList<Tree<StateSet>> q = new LinkedList<Tree<StateSet>>();
	    q.add(tree);
	    while(!q.isEmpty()){
		Tree<StateSet> t = q.poll();
		System.out.println(t);
		for(Tree<StateSet> c : t.getChildren())
		    q.add(c);
	    }
	}
    }

    public void setUseOfSubstates(boolean useSubstates){
	USE_SUBSTATES = useSubstates;
    }

    public boolean doWeUseSubstates(){
	return USE_SUBSTATES;
    }

    public void incrementLatentObservation(BerkeleyCompatibleFragment fragment){
	incrementLatentObservation(fragment,1);
    }

    public void incrementLatentObservation(BerkeleyCompatibleFragment fragment, double amount){
	//if(fragment.getNumberOfProductions()>1)
	//  System.out.println("In LatentStatistics, fragment="+fragment+" is getting additional count");
	fragmentCounter.incrementCount(fragment, amount);
	fragmentUnigramCounter.incrementCount(fragment,amount);
	//update # times see this root
	rootCounter.incrementCount(fragment.getRootTagSig(),amount);
    }

    public double getUnigramFragmentCount(BerkeleyCompatibleFragment X){
	return fragmentUnigramCounter.getCount(X);
    }

    public void incrementLatentBigram(BerkeleyCompatibleFragment top, BerkeleyCompatibleFragment bottom, int nodeInTree, double amount, boolean useSubstates){
	fragmentCounter.incrementCount(BerkeleyCompatibleFragment.compose(top,bottom), amount);
	//normal top X, bottom Y
	Counter<BerkeleyCompatibleFragment> countbcfTops = fragmentBigramMapForTops.get(top);
	if(countbcfTops==null) countbcfTops = new Counter<BerkeleyCompatibleFragment>();
	countbcfTops.incrementCount(bottom,amount);
	fragmentBigramMapForTops.put(top,countbcfTops);
	//valid top X, bottom Y
	//System.out.println("trying to get see if " + top +" and " + bottom + " are valid");
	if(constraintSet.isCouplingAllowed(top,bottom,nodeInTree,tagNumberer,useSubstates)){
	    //System.out.println("\tYES");
	    Counter<BerkeleyCompatibleFragment> countValid = fragmentValidBigramMap.get(top);
	    if(countValid==null) countValid = new Counter<BerkeleyCompatibleFragment>();
	    countValid.incrementCount(bottom,amount);
	    fragmentValidBigramMap.put(top,countValid);
	    //also increment the ``bottom'' set count for Y...
	    countValid = validBottomSet.get(bottom);
	    if(countValid==null) countValid = new Counter<BerkeleyCompatibleFragment>();
	    countValid.incrementCount(top,amount);
	    validBottomSet.put(bottom,countValid);
	    //... and for any L s.t. root(L)=root(Y)
	    generalValidBottomCount.incrementCount(bottom.getRootTagSig(),amount);
	} else{
	    //System.out.println("\tNO");
	    Counter<BerkeleyCompatibleFragment> countInvalid = fragmentInvalidBigramMap.get(top);
	    if(countInvalid==null) countInvalid = new Counter<BerkeleyCompatibleFragment>();
	    countInvalid.incrementCount(bottom,amount);
	    fragmentInvalidBigramMap.put(top,countInvalid);
	}
	//normal top Y, bottom X
	Counter<BerkeleyCompatibleFragment> countbcfBottoms = fragmentBigramMapForBottoms.get(top);
	if(countbcfBottoms==null) countbcfBottoms = new Counter<BerkeleyCompatibleFragment>();
	countbcfBottoms.incrementCount(top,amount);
	fragmentBigramMapForBottoms.put(bottom,countbcfBottoms);
    }

    public void incrementLatentBigram(BerkeleyCompatibleFragment top, BerkeleyCompatibleFragment bottom, int nodeInTree, boolean useSubstates){
	incrementLatentBigram(top,bottom,nodeInTree, 1, useSubstates);
    }

    public String toString(){
	return fragmentUnigramCounter.toString() + "\n\n" +
	    //fragmentInvalidBigramMap.toString() + "\n\n" +
	    fragmentValidBigramMap.toString() + "\n\n";// +
	/*validBottomSet.toString() + "\n\n" +
	    rootCounter.toString() + "\n\n" +
	    generalValidBottomCount.toString();*/
    }

    public String toString1(){
	return fragmentCounter.toString() + "\n\n" + 
	    fragmentUnigramCounter.toString() + "\n\n" + 
	    fragmentBigramMapForTops.toString() + "\n\n" + 
	    fragmentBigramMapForBottoms.toString()+"\n";
    }

    public double getGeneralValidBottomCountOf(int tag, int sig){
	return generalValidBottomCount.getCount(new Pair<Integer,Integer>(tag,sig));
    }

    public double getValidBottomCountOf(BerkeleyCompatibleFragment X){
	return validBottomSet.containsKey(X)?(validBottomSet.get(X).sum()):0.0;
    }

    public double getCountOfNTSymbol(int tag, int sig){
	return rootCounter.getCount(new Pair<Integer,Integer>(tag,sig));
    }

    public double getUpperCouplingFractionalCount(BerkeleyCompatibleFragment X, double gamma){
	Counter<BerkeleyCompatibleFragment> validCounts = fragmentValidBigramMap.get(X);
	//System.out.println("for fragment X = " + X+ " validCounts is " + (validCounts==null));
	Counter<BerkeleyCompatibleFragment> invalidCounts = fragmentInvalidBigramMap.get(X);
	if((validCounts==null && invalidCounts==null)||
	   (validCounts!=null && invalidCounts!=null && validCounts.size()==0 && invalidCounts.size()==0)){
	    return 1.0;
	}
	double invalidSum = (invalidCounts==null)?0.0:invalidCounts.sum();
	double validSum = (validCounts==null)?0.0:validCounts.sum();
	//System.out.println("invalidSum = " + invalidSum + "; validSum = " + validSum);
	return (invalidSum + (1-gamma)*validSum)/(invalidSum+validSum);
    }

    public double getCoupledFractionalCount(BerkeleyCompatibleFragment X, BerkeleyCompatibleFragment Y){
	BerkeleyCompatibleFragment XX = X.remapToOriginal(); BerkeleyCompatibleFragment YY = Y.remapToOriginal();
	//System.out.println("X="+X+" is mapped to " + XX);
	//System.out.println("Y="+Y+" is mapped to " + YY);
	Counter<BerkeleyCompatibleFragment> invalidCounts = fragmentInvalidBigramMap.get(XX);
	Counter<BerkeleyCompatibleFragment> validCounts = fragmentValidBigramMap.get(XX);
	//System.out.println(validCounts);
	if(validCounts==null || (validCounts!=null && validCounts.size()==0)){
	    //System.out.println("In LatentStatistics:getCoupledFractionalCount :: validCounts is null");
	    return 1.0;
	}
	double validSum = (validCounts==null)?0.0:validCounts.sum();
	double invalidSum = (invalidCounts==null)?0.0:invalidCounts.sum();
	return (validSum+invalidSum==0.0)?1.0:(validCounts.getCount(YY)/(invalidSum+validSum));
    }

    public double getCoupledFractionalCountAsGiven(BerkeleyCompatibleFragment X, BerkeleyCompatibleFragment Y){
	Counter<BerkeleyCompatibleFragment> validCounts = fragmentValidBigramMap.get(X);
	//System.out.println(validCounts);
	if(validCounts==null || (validCounts!=null && validCounts.size()==0)){
	    return 1.0;
	}
	double validSum = (validCounts==null)?0.0:validCounts.sum();
	return validSum==0.0?1.0:(validCounts.getCount(Y)/validSum);
    }

    private Pair<Tree<String>,BCFTraversalContainer> extractBestViterbiDerivation(Tree<StateSet> tree, int substate, ArrayParser AP, boolean outputScore, int nodeInTree){
	//System.out.println("eBVD: " + tree);
  	if (tree.isLeaf()) {
	    //should never happen, me thinks
	    throw new Error("Malformed tree");
	}
  	if (substate==-1) substate=0;
	//if it's a preterminal, then construct it and return (with a very brief recursive call)
  	if (tree.isPreTerminal()){
	    ArrayList<Tree<String>> child = new ArrayList<Tree<String>>();
	    ProductionTuple pt = new ProductionTuple(true);
	    pt.addPreterminal(tree.getLabel().getState(),substate, tree.getNodeNumber(), tree.getChildren().get(0).getLabel().getWord());
	    //pt.addPreterminal(tree.getLabel().getState(),substate, nodeInTree, tree.getChildren().get(0).getLabel().getWord());
	    child.add(new Tree<String>(tree.getChildren().get(0).getLabel().getWord()));
	    BerkeleyCompatibleFragment bcf = new BerkeleyCompatibleFragment(pt);
	    BCFTraversalContainer pretermBCFTraversalContainer = new BCFTraversalContainer(bcf, new LinkedList<BerkeleyCompatibleFragment>());

	    String goalStr = tagNumberer.object(tree.getLabel().getState())+"-"+substate;
	    if (outputScore) goalStr = goalStr + " " + tree.getLabel().getIScore(substate);
	    
	    //update the counts 
	    //because it's a preterminal, we don't have to update
	    //the returning sets
	    if(!InternalNodeSet.isSubstateInternal(tree.getLabel().getState(),substate)){
		this.incrementLatentObservation(bcf);
	    }
	    return new Pair<Tree<String>,BCFTraversalContainer>(new Tree<String>(goalStr, child), pretermBCFTraversalContainer);
	}
  	
	//cache the node
	StateSet node = tree.getLabel();
	//get the tag
  	short pState = node.getState();
	//the signature is substate 
	
	ArrayList<Tree<String>> newChildren = new ArrayList<Tree<String>>();
	List<Tree<StateSet>> children = tree.getChildren();
		
	double myScore = node.getIScore(substate);
	if (myScore==Double.NEGATIVE_INFINITY){
	    myScore = DoubleArrays.max(node.getIScores());
	    substate = DoubleArrays.argMax(node.getIScores());
	}
	List<BerkeleyCompatibleFragment> listOfPreviousBCFs = new LinkedList<BerkeleyCompatibleFragment>();
	BerkeleyCompatibleFragment nodeBCF = new BerkeleyCompatibleFragment();
	//expansion set for frag rooted at THIS node
	Collection<BerkeleyCompatibleFragment> expansionSet = new LinkedList<BerkeleyCompatibleFragment>();
	switch (children.size()) {
	case 1:
	    StateSet child = children.get(0).getLabel();
	    short cState = child.getState();
	    int nChildStates = child.numSubStates();
	    double[][] uscores = AP.grammar.getUnaryScore(pState,cState);
	    int childIndex = -1;
	    for (int j = 0; j < nChildStates; j++) {
		if (childIndex != -1) break;
		if (uscores[j]!=null) { 
		    double cS = child.getIScore(j);
		    if (cS==Double.NEGATIVE_INFINITY) continue;
		    double rS = uscores[j][substate]; // rule score
		    if (rS==Double.NEGATIVE_INFINITY) continue;
		    double res = rS + cS;
		    if (matches(res,myScore)) childIndex = j;
		}
	    }
	    Pair<Tree<String>,BCFTraversalContainer> unaryPair = extractBestViterbiDerivation(children.get(0), childIndex, AP, outputScore, children.get(0).getNodeNumber());
	    newChildren.add(unaryPair.getFirst());
	    //if parent is internal
	    ProductionTuple pt = new ProductionTuple(true);
	    pt.addNT(0,(int)pState,substate,tree.getNodeNumber());
	    pt.addNT(1,(int)cState,childIndex,children.get(0).getNodeNumber());
	    nodeBCF.addTuple(pt);
	    /*if(InternalNodeSet.isSubstateInternal(pState,substate)){
		nodeBCF = unaryPair.getSecond().getMinimalFragment();		
		nodeBCF.addTuple(pt);
	    } else{
		nodeBCF.addTuple(pt);
		//set expansion set appropriately
		//expansionSet = unaryPair.getSecond().getExpansionSet();
	    }*/
	    //update the expansion set of this node based on internal status of child
	    if(InternalNodeSet.isSubstateInternal(cState,childIndex)){
		//add elements in child's expansion set
		//correct: 19 Mar 2012
		for(BerkeleyCompatibleFragment cbcf : unaryPair.getSecond().getExpansionSet()){
		    expansionSet.add(cbcf);
		}
		for(ProductionTuple cpt : unaryPair.getSecond().getMinimalFragment().getTuples())
		    nodeBCF.addTuple(cpt);
		    
	    } else{
		//add minimal set of child
		//correct: 19 Mar 2012
		expansionSet.add(unaryPair.getSecond().getMinimalFragment());
	    }
	    break;
	case 2:
	    StateSet leftChild = children.get(0).getLabel();
	    StateSet rightChild = children.get(1).getLabel();
	    int nLeftChildStates = leftChild.numSubStates();
	    int nRightChildStates = rightChild.numSubStates();
	    short lState = leftChild.getState();
	    short rState = rightChild.getState();
	    double[][][] bscores = AP.grammar.getBinaryScore(pState,lState,rState);
	    int lChildIndex = -1, rChildIndex = -1;
	    for (int j = 0; j < nLeftChildStates; j++) {
		if (lChildIndex!=-1 && rChildIndex!=-1) break;
		double lcS = leftChild.getIScore(j);
		if (lcS==Double.NEGATIVE_INFINITY) continue;
		for (int k = 0; k < nRightChildStates; k++) {
		    if (lChildIndex!=-1 && rChildIndex!=-1) break;
		    double rcS = rightChild.getIScore(k);
		    if (rcS==Double.NEGATIVE_INFINITY) continue;
		    if (bscores[j][k]!=null) { // check whether one of the parents can produce these kids
			double rS = bscores[j][k][substate];
			if (rS==Double.NEGATIVE_INFINITY) continue;
			double res = rS + lcS + rcS;
			if (matches(myScore,res)){
			    lChildIndex = j;
			    rChildIndex = k;
			}
		    }
		}
	    }
	    
	    Pair<Tree<String>,BCFTraversalContainer> binaryL = extractBestViterbiDerivation(children.get(0), lChildIndex, AP, outputScore, children.get(0).getNodeNumber());
	    Pair<Tree<String>,BCFTraversalContainer> binaryR = extractBestViterbiDerivation(children.get(1), rChildIndex, AP, outputScore, children.get(1).getNodeNumber());
	    newChildren.add(binaryL.getFirst());
	    newChildren.add(binaryR.getFirst());

	    BerkeleyCompatibleFragment lFrag = binaryL.getSecond().getMinimalFragment();
	    BerkeleyCompatibleFragment rFrag = binaryR.getSecond().getMinimalFragment();

	    //if parent is internal
	    pt = new ProductionTuple(false);
	    pt.addNT(0,(int)pState,substate,tree.getNodeNumber());
	    pt.addNT(1,(int)lState,lChildIndex, children.get(0).getNodeNumber());
	    pt.addNT(2,(int)rState,rChildIndex, children.get(1).getNodeNumber());
	    if(InternalNodeSet.isSubstateInternal(pState,substate)){
		nodeBCF.addTuple(pt);
		//add PTs to the expansion set of this parent
		//expansionSet = new LinkedList<BerkeleyCompatibleFragment>();//unaryPair.getSecond().getExpansionSet());
	    } else{
		nodeBCF.addTuple(pt);
	    }

	    if(InternalNodeSet.isSubstateInternal(lState,lChildIndex)){
		//add elements in child's expansion set
		for(BerkeleyCompatibleFragment cbcf : binaryL.getSecond().getExpansionSet()){
		    expansionSet.add(cbcf);
		}
		//		System.out.println("what is bcf before error? "+ nodeBCF);
		//System.out.println("what is lFrag before? "+ lFrag);
		for(ProductionTuple lpt : lFrag.getTuples()){
		    lpt.overrideInternal(0,true);
		    nodeBCF.addTuple(lpt);
		}
	    } else{
		//add minimal set of child
		expansionSet.add(binaryL.getSecond().getMinimalFragment());
	    }

	    if(InternalNodeSet.isSubstateInternal(rState,rChildIndex)){
		//add elements in child's expansion set
		for(BerkeleyCompatibleFragment cbcf : binaryR.getSecond().getExpansionSet()){
		    expansionSet.add(cbcf);
		}
		for(ProductionTuple rpt : rFrag.getTuples()){
		    rpt.overrideInternal(0,true);
		    nodeBCF.addTuple(rpt);
		}
	    } else{
		//add minimal set of child
		expansionSet.add(binaryR.getSecond().getMinimalFragment());
	    }
	    break;
	default:
	    throw new Error ("Malformed tree: more than two children");
	}
  	String parentString = (String)tagNumberer.object(node.getState());
	if (parentString.endsWith("^g")) parentString = parentString.substring(0,parentString.length()-2);
	parentString = parentString+"-"+substate;
	if (outputScore) parentString = parentString + " " + myScore;
	
	//parent is not internal
	//so update counts
	if(!InternalNodeSet.isSubstateInternal(pState,substate)){
	    //update "unigram" count
	    //if(nodeBCF.getNumberOfProductions()>1)
	    //	System.out.println("for tree " + tree + ", \n\tbcf is " + nodeBCF);
	    this.incrementLatentObservation(nodeBCF);
	    //update "bigram" counts (go through expansionSet)
	    //System.out.println("for nodeBCF = " + nodeBCF + ", expansionSet = " + expansionSet);
	    for(BerkeleyCompatibleFragment bottomFrag : expansionSet){
		//System.out.println("nodeBCF="+nodeBCF+", bottomFrag="+bottomFrag+", bottomFrag.getRoot()="+bottomFrag.getRoot()+", root id="+bottomFrag.getRoot().getID(0));
		this.incrementLatentBigram(nodeBCF, bottomFrag,giveBottomReorderedIndex(tree, bottomFrag.getRoot().getID(0)), USE_SUBSTATES);
		//System.out.println("for tree " + tree + ", \n\ttop is " + nodeBCF + ", \n\tbottom is " + bottomFrag);
	    }
	}

	return new Pair<Tree<String>,BCFTraversalContainer>(new Tree<String>(parentString, newChildren), new BCFTraversalContainer(nodeBCF,expansionSet));
    }

    private int giveBottomReorderedIndex(Tree<StateSet> tree, int nodeIDToRemap){
	int remappedID=-1;
	LinkedList<Tree<StateSet>> Q=new LinkedList<Tree<StateSet>>();
	Q.add(tree);
	int counter = 0;
	while(!Q.isEmpty() || remappedID==-1){
	    Tree<StateSet> t = Q.poll();
	    if(t.getNodeNumber() == nodeIDToRemap){
		remappedID=counter;
		break;
	    }
	    for(Tree<StateSet> c : t.getChildren())
		Q.add(c);
	    counter++;
	}
	if(remappedID==-1) throw new Error("Whoops! I can't find any node with number "+nodeIDToRemap+" in the tree="+tree);
	return remappedID;
    }
    
    private int giveBottomReorderedIndex(int start, int end){
	int count = 0;
	int curr=end;
	LinkedList<Integer> leftOrRight = new LinkedList<Integer>();
	while(curr!=start){
	    int sub = curr%2==0 ? 2 : 1;
	    leftOrRight.push(new Integer(sub));
	    curr = (curr - sub)/2;
	    count++;
	    if(curr<0){
		throw new Error("start="+start+" and end="+end+" don't conform to complete binary tree ordering!");
	    }
	}
	int tmp=0;
	for(int i = 0; i < count;i++)
	    tmp = 2*tmp + leftOrRight.pop().intValue();
	return tmp;	  
    }
  
    public Pair<Tree<String>,BCFTraversalContainer> getBestViterbiDerivation(Tree<StateSet> tree, ArrayParser AP, boolean outputScore){
  	AP.doViterbiInsideScores(tree);
	setTN();
  	if (tree.getLabel().getIScore(0)==Double.NEGATIVE_INFINITY) {
	    //  		System.out.println("Tree is unparsable!");
	    return null;
  	}
	//System.out.println("\n\nEXTRACTING on depth="+tree.getDepth()+": " + tree);
  	return extractBestViterbiDerivation(tree, 0, AP, outputScore, 0);
    }

    private void setTN(){
	tagNumberer = Numberer.getGlobalNumberer("tags");
    }


    private static final double TOL = 1e-5;
    protected boolean matches(double x, double y) {
	return (Math.abs(x - y) / (Math.abs(x) + Math.abs(y) + 1e-10) < TOL);
    }


}
