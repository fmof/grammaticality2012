/**
 * 
 */
package edu.jhu.coe.PCFGLA.smoothing;

import java.io.Serializable;
import java.util.List;


import edu.jhu.coe.PCFGLA.BinaryCounterTable;
import edu.jhu.coe.PCFGLA.BinaryRule;
import edu.jhu.coe.PCFGLA.InternalNodeSet;
import edu.jhu.coe.PCFGLA.UnaryCounterTable;
import edu.jhu.coe.PCFGLA.UnaryRule;

import edu.jhu.coe.syntax.Tree;
import edu.jhu.coe.util.ArrayUtil;

/**
 * @author leon
 *
 */
public class SmoothAcrossParentBits implements Smoother, Serializable  {
	
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    double same;
    double[][][] diffWeights;
    double weightBasis = 0.5;
    double totalWeight;
	
    public SmoothAcrossParentBits copy(){
	return new SmoothAcrossParentBits(same,diffWeights,weightBasis,totalWeight);
    }
	
    public SmoothAcrossParentBits(double smooth, Tree<Short>[] splitTrees) {
	// does not smooth across top-level split, otherwise smooths uniformly
	same = 1-smooth;
		
	int nStates = splitTrees.length;
	diffWeights = new double[nStates][][];
	for (short state=0; state<nStates; state++){
	    Tree<Short> splitTree = splitTrees[state];
	    //System.out.println("splitTree="+splitTree);
	    List<Short> allSubstates = splitTree.getYield();
	    int nSubstates = 1;
	    for (int i=0; i<allSubstates.size(); i++){ 
		if (allSubstates.get(i)>=nSubstates && !InternalNodeSet.isSubstateInternal(state,i)) 
		    nSubstates = allSubstates.get(i)+1;
	    }
	    //System.out.println("In SmoothAcrossParentBits, for state="+state+", nSubstates="+nSubstates);
	    diffWeights[state] = new double[nSubstates][nSubstates];
	    if (nSubstates==1){
		// state has only one substate -> no smoothing
		diffWeights[state][0][0] = 1.0;
	    }
	    else {
		// smooth only with ones in the same top-level branch
		// TODO: weighted smoothing

		// descend down to first split first
		while (splitTree.getChildren().size()==1) { splitTree = splitTree.getChildren().get(0); }				
		for (int branch=0; branch<2; branch++){
		    // compute weights for substates in top-level branch
		    List<Short> substatesInBranch = splitTree.getChildren().get(branch).getYield();
		    int total = substatesInBranch.size();
		    double normalizedSmooth = smooth/(total-1);

		    for (short i : substatesInBranch) {
			if(i>=nSubstates) break;
			for (short j : substatesInBranch) {
			    if(j>=nSubstates) break;
			    //19 Mar 2012: problem (array out of bounds) here
			    if (i==j) { diffWeights[state][i][j] = same; }
			    else { diffWeights[state][i][j] = normalizedSmooth; }
			}
		    }
		}			      			
	    }
	}
	//System.out.println(InternalNodeSet.toString2());
    }

    /**
     * @param same2
     * @param diffWeights2
     * @param weightBasis2
     * @param totalWeight2
     */
    public SmoothAcrossParentBits(double same2, double[][][] diffWeights2, double weightBasis2, double totalWeight2) {
	this.same = same2;
	this.diffWeights = diffWeights2;
	this.weightBasis = weightBasis2;
	this.totalWeight = totalWeight2;
    }

    /* (non-Javadoc)
     * @see edu.jhu.coe.PCFGLA.smoothing.Smoother#smooth(edu.jhu.coe.util.UnaryCounterTable, edu.jhu.coe.util.BinaryCounterTable)
     */
    public void smooth(UnaryCounterTable unaryCounter, BinaryCounterTable binaryCounter) {
	for (UnaryRule r : unaryCounter.keySet()) {
	    double[][] scores = unaryCounter.getCount(r);
	    double[][] scopy = new double[scores.length][];
	    short pState = r.parentState; short cState = r.childState;
	    
	    //iterate through child states
	    //it shouldn't matter if the child is internal; it should only
	    //matter if the parent is internal. If p is internal but c isn't,
	    //then that rule *could* have a non-zero probability we want to
	    //consider when doing the average
	    for (int j=0; j<scores.length; j++) {
		if( scores[j]==null ) continue; // nothing to smooth
		scopy[j] = new double[scores[j].length];
		//iterate through parent states
		for (int i=0; i<scores[j].length; i++) {
		    //check if it's internal
		    if(InternalNodeSet.isSubstateInternal(pState,i)){
			scopy[j][i] = scores[j][i];
			continue;
		    }
		    if(i>= diffWeights[pState].length) continue;
		    //iterate through all other parent states
		    for (int k=0; k<scores[j].length; k++) {
			if(InternalNodeSet.isSubstateInternal(pState,k)){
			    break;
			}
			if(InternalNodeSet.isSubstateInternal(cState,j) && scores[j][i]==0.0){
			    continue;
			}
			if(k>= diffWeights[pState].length) break;
			/*System.out.println("Let's see where the trouble is for pState="+pState+", i="+i+", j="+j+", k="+k+":");
			System.out.println("\tscopy["+j+"]["+i+"] = " + scopy[j][i]);
			System.out.println("\t\t"+diffWeights.length);
			System.out.println("\t\t"+diffWeights[pState].length);
			System.out.println("\t\t"+diffWeights[pState][i].length);
			System.out.println("\tdiffWeights["+pState+"]["+i+"]["+k+"] = " + diffWeights[pState][i][k]);
			System.out.println("\tscores["+j+"]["+i+"] = " + scores[j][k]);*/
			scopy[j][i] += diffWeights[pState][i][k] * scores[j][k];
		    }
		}
	    }
	    unaryCounter.setCount(r,scopy);
	}
	for (BinaryRule r : binaryCounter.keySet()) {
	    double[][][] scores = binaryCounter.getCount(r);
	    double[][][] scopy = new double[scores.length][scores[0].length][];
	    short pState = r.parentState;
	    short lcState = r.leftChildState;
	    short rcState = r.rightChildState;
	    //iterate through left child
	    for (int j=0; j<scores.length; j++) {
		//iterate through right child
		for (int l=0; l<scores[j].length; l++) {
		    if (scores[j][l]==null)	continue; //nothing to smooth
					
		    scopy[j][l] = new double[scores[j][l].length]; 
		    //iterate through parent
		    for (int i=0; i<scores[j][l].length; i++) {
			if(InternalNodeSet.isSubstateInternal(pState,i)){
			    scopy[j][l][i] = scores[j][l][i];
			    continue;
			}
			if(i>=diffWeights[pState].length) continue;
			//iterate through all other parents
			for (int k=0; k<scores[j][l].length; k++) {
			    if(InternalNodeSet.isSubstateInternal(pState,k)){
				break;
			    }
			    if((InternalNodeSet.isSubstateInternal(lcState,j) && scores[j][l][i]==0.0) ||
			       (InternalNodeSet.isSubstateInternal(rcState,l) && scores[j][l][i]==0.0)){
				continue;
			    }
			    if(k>=diffWeights[pState].length) break;
			    scopy[j][l][i] += diffWeights[pState][i][k] * scores[j][l][k];
			}
		    }
		}
	    }
	    binaryCounter.setCount(r,scopy);
	}
    }
	
    private void fillWeightsArray(short state, short substate, double weight, Tree<Short> subTree){
	if (subTree.isLeaf()){
	    if (subTree.getLabel()==substate) diffWeights[state][substate][substate] = same;
	    else { diffWeights[state][substate][subTree.getLabel()] = weight; totalWeight+=weight;}
	    return;
	}
	if (subTree.getChildren().size()==1) { 
	    fillWeightsArray(state,substate,weight,subTree.getChildren().get(0));
	    return;
	}
	for (int branch=0; branch<2; branch++) {	
	    Tree<Short> branchTree = subTree.getChildren().get(branch);
	    List<Short> substatesInBranch = branchTree.getYield();
	    //int nSubstatesInBranch = substatesInBranch.size();
	    if (substatesInBranch.contains(substate)) fillWeightsArray(state,substate,weight,branchTree);
	    else fillWeightsArray(state,substate,weight*weightBasis/2.0,branchTree);
	}
    }

    /* (non-Javadoc)
     * @see edu.jhu.coe.PCFGLA.smoothing.Smoother#smooth(short, float[])
     */
    public void smooth(short tag, double[] scores) {
	double[] scopy = new double[scores.length];
	//System.out.println("In smooth, for tag="+tag+", scores.length="+scores.length);
	for (int i=0; i<scores.length; i++) {
	    if(InternalNodeSet.isSubstateInternal(tag,i)){
		scopy[i] = scores[i];
		continue;
	    }
	    if(i>= diffWeights[tag].length)
		continue;
	    for (int k=0; k<scores.length; k++) {
		if(InternalNodeSet.isSubstateInternal(tag,k))
		    break;
		if(k >= diffWeights[tag].length)
		    break;
		//System.out.println("\twhere's the problem? tag="+tag+", i="+i+", k="+k);
		//System.out.println("\t\tdiffWeights["+tag+"].length="+diffWeights[tag].length);
		//System.out.println("\t\tInternalNodeSet.isSubstateInternal("+tag+","+i+")="+InternalNodeSet.isSubstateInternal(tag,i));
		scopy[i] += diffWeights[tag][i][k] * scores[k];
	    }
	}
	for (int i=0; i<scores.length; i++) {
	    scores[i] = scopy[i];
	}
    }

    /* (non-Javadoc)
     * @see edu.jhu.coe.PCFGLA.smoothing.Smoother#updateWeights(int[][])
     */
    public void updateWeights(int[][] toSubstateMapping) {
	double[][][] newWeights = new double[toSubstateMapping.length][][];
	for (int state=0; state<toSubstateMapping.length; state++){
	    int nSub = toSubstateMapping[state][0];
	    newWeights[state] = new double[nSub][nSub];
	    if (nSub==1) {
		newWeights[state][0][0] = 1.0;
		continue;
	    } 
	    double[] total = new double[nSub];
	    for (int substate1=0; substate1<diffWeights[state].length; substate1++){
		for (int substate2=0; substate2<diffWeights[state].length; substate2++){
		    newWeights[state][toSubstateMapping[state][substate1+1]][toSubstateMapping[state][substate2+1]] += diffWeights[state][substate1][substate2];
		    total[toSubstateMapping[state][substate1+1]] += diffWeights[state][substate1][substate2];
		}
	    }
	    for (int substate1=0; substate1<nSub; substate1++){
		for (int substate2=0; substate2<nSub; substate2++){
		    newWeights[state][substate1][substate2] /= total[substate1];
		}
	    }
	}
	diffWeights = newWeights;
    }
}
