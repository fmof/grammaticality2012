package edu.jhu.coe.PCFGLA;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import edu.jhu.coe.PCFGLA.LatentStatistics;
import edu.jhu.coe.syntax.BerkeleyCompatibleFragment;
import edu.jhu.coe.syntax.ProductionTuple;
import edu.jhu.coe.util.*;

import fig.basic.Pair;


/**
 * Unary Rules (with ints for parent and child).
 * Internal node support added by Frank Ferraro.
 *
 * @author Dan Klein
 */
public class UnaryRule extends Rule implements java.io.Serializable, Comparable {

    public short childState = -1;
    /**
     * NEW:
     *  scores[childSubState][parentSubState]
     */
    private double[][] scores; 

    public UnaryRule(short pState, short cState, double[][] scores) {
	this.parentState = pState;
	this.childState = cState;
	this.scores = scores;
    }
  
    public UnaryRule(short pState, short cState) {
	this.parentState = pState;
	this.childState = cState;
	//    this.scores = new double[1][1];
    }

    /** Copy constructor */
    public UnaryRule(UnaryRule u) {
  	this(u.parentState,u.childState,ArrayUtil.copy(u.scores));
    }

    public UnaryRule(UnaryRule u,double[][] newScores) {
  	this(u.parentState,u.childState,newScores);
    }

    public UnaryRule(short pState, short cState, short pSubStates, short cSubStates) {
	this.parentState = pState;
	this.childState = cState;
	this.scores = new double[cSubStates][pSubStates];
    }

    public boolean isUnary() {
	return true;
    }

    public int hashCode() {
	return ((int)parentState << 18) ^ ((int)childState);
    }

    public boolean equals(Object o) {
	if (this == o) {
	    return true;
	}
	if (o instanceof UnaryRule) {
	    UnaryRule ur = (UnaryRule) o;
	    if (parentState == ur.parentState &&
		childState == ur.childState) {
		return true;
	    }
	}
	return false;
    }

    public int compareTo(Object o) {
	UnaryRule ur = (UnaryRule) o;
	if (parentState < ur.parentState) {
	    return -1;
	}
	if (parentState > ur.parentState) {
	    return 1;
	}
	if (childState < ur.childState) {
	    return -1;
	}
	if (childState > ur.childState) {
	    return 1;
	}
	return 0;
    }

    private static final char[] charsToEscape = new char[]{'\"'};

    public String toStringMJ() {
	Numberer n = Numberer.getGlobalNumberer("tags");
	String cState = (String)n.object(childState);
	String pState = (String)n.object(parentState);
	if (scores==null) return "";//pState+" -> "+cState+"\n";
	StringBuilder sb = new StringBuilder();
	for (int cS=0; cS<scores.length; cS++){
	    if (scores[cS]==null) continue;
	    for (int pS=0; pS<scores[cS].length; pS++){
		double p = scores[cS][pS]; 
		if (p>0)
		    sb.append(p+" "+pState + "_" + pS + InternalNodeSet.getStringRepresentation(parentState, pS) + " -> " + cState + "_" + cS + InternalNodeSet.getStringRepresentation(childState, cS)+ " " + "\n");
	    }
	}
	return sb.toString();
    }

    public String toString() {
	Numberer n = Numberer.getGlobalNumberer("tags");
	String cState = (String)n.object(childState);
	String pState = (String)n.object(parentState);
	if (scores==null) return pState+" -> "+cState+"\n";
	StringBuilder sb = new StringBuilder();
	for (int cS=0; cS<scores.length; cS++){
	    if (scores[cS]==null) continue;
	    for (int pS=0; pS<scores[cS].length; pS++){
		double p = scores[cS][pS]; 
		if (p>0)
		    sb.append(pState + "_" + pS + InternalNodeSet.getStringRepresentation(parentState, pS) + " -> " + cState + "_" + cS + InternalNodeSet.getStringRepresentation(childState, cS)+ " " + p + "\n");
	    }
	}
	return sb.toString();
    }
  
    public String toString_old() {
	Numberer n = Numberer.getGlobalNumberer("tags");
	return "\"" + 
	    StringUtils.escapeString(n.object(parentState).toString(), charsToEscape, '\\') + 
	    "\" -> \"" + 
	    StringUtils.escapeString(n.object(childState).toString(), charsToEscape, '\\') + 
	    "\" " + ArrayUtil.toString(scores);
    }

    public short getChildState() {
	return childState;
    }

    public void setScore(int pS, int cS, double score){
  	// sets the score for a particular combination of substates
  	scores[cS][pS] = score;
    }

    public double getScore(int pS, int cS){
  	// gets the score for a particular combination of substates
	if (scores==null || cS>= scores.length ||  scores[cS]==null) {
	    if (logarithmMode)
		return Double.NEGATIVE_INFINITY;
	    return 0;
  	}
  	return scores[cS][pS];
    }
  
    public void setScores2(double[][] scores){
  	this.scores = scores;
    }

    /** scores[parentSubState][childSubState]
     */
    public double[][] getScores2(){
  	return scores;
    }
  
    public void setNodes(short pState, short cState){
  	this.parentState = pState;
  	this.childState = cState;
    }

    public void incrementPartitionFunction(double[][] pf, short[] numSigsPerTag){
	if (logarithmMode)
	    throw new Error("cannot compute partition function in logarithm mode!");
	if(pf[parentState]==null) pf[parentState] = new double[numSigsPerTag[parentState]];
	for(int cs=0;cs<scores.length;cs++){
	    if(scores[cs]==null) continue;
	    for(int ps=0;ps<scores[cs].length;ps++){
		pf[parentState][ps]+=scores[cs][ps];
	    }
	}
    }

    private static final long serialVersionUID = 2L;


    public void initializeScoresAfterCoupling(double[][] oldScores, double[][] parentTally, FragmentProbabilityComputer fragmentProbability, Map<Vector<Integer>,Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>> startingFragmentMap, Set<Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>> topAndBottomFragments, Set<ProductionTuple> allHypoedFrags){
	for(int cs=0;cs<scores.length;cs++){
	    if(scores[cs]==null) continue;
	    for(int ps=0;ps<scores[cs].length;ps++){
		//System.out.println("unary init: "+childState+" ("+cs+") , "+parentState+"("+ ps +") ; setting score to -70.5");
		//scores[cs][ps]=-70.5;
		ProductionTuple pt = new ProductionTuple(true);
		pt.addNT(0,(int)parentState,ps);
		pt.addNT(1,(int)childState,cs);
	
		//or we know of the child from before, but it's never used 
		//in this particular rule
		if(cs<oldScores.length && oldScores[cs]==null) continue;
		Vector<Integer> vec = new Vector<Integer>(4);
		vec.add(new Integer(parentState)); vec.add(new Integer(ps));
		vec.add(new Integer(childState)); vec.add(new Integer(cs));
		Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment> bcfPair = startingFragmentMap.get(vec);
		//System.out.println("\tand bcfPair="+bcfPair);
		boolean parentInternal = InternalNodeSet.isSubstateInternal(parentState,ps);
		boolean childInternal = InternalNodeSet.isSubstateInternal(childState,cs);
		///System.out.println("querying for vec="+vec);
		if(bcfPair==null) {
		    //System.out.println(parentState + ":" + ps + ", " +childState+":"+cs+" => " + bcfPair);		    //since bcf is null, then we obviously didn't deal with this rule starting any fragment
		    //we need to see if this is part of a valid fragment (either previously or recently hypothesized)
		    //if not, then set to 0
		    if(!allHypoedFrags.contains(pt)){
			scores[cs][ps]=0.0;
		    } else{
			scores[cs][ps]=1.0;
			parentTally[parentState][ps] += scores[cs][ps];
		    }
		    //System.out.println("\tand score is "+scores[cs][ps]);
		    continue;
		}
		//System.out.println(bcfPair);
		if(!allHypoedFrags.contains(pt)){
		    scores[cs][ps]=-1.0;
		    throw new Error("how can bcf be non-null but the initial PT not be part of the hypothesized/concerned production tuple set?");
		} else{
		    //System.out.println(parentState + ":" + ps + ", " +childState+":"+cs+" => " + bcfPair);
		    BerkeleyCompatibleFragment top = bcfPair.getFirst();
		    BerkeleyCompatibleFragment bottom = bcfPair.getSecond();
		    if(bottom==null){
			//use eq 5
			scores[cs][ps]=fragmentProbability.computeForFragment(oldScores[cs][ps],parentState, (short)ps, top);			
			parentTally[parentState][ps] += scores[cs][ps];
			
		    } else{
			ProductionTuple root = top.getRoot();
			//System.out.println("top is " + top+" and root="+root);			

			//System.out.println("binary init score, top="+top+", bottom="+bottom);
			//need oldScores[top.tag][top.sig]
			Pair<Integer,Integer> genLineage = InternalNodeSet.getGeneratingTagAndSigs(childState,cs);
			if(genLineage==null)
			    throw new Error("Somehow the lineage wasn't properly set");
			//System.out.println(genLineage);
			//System.out.println(oldScores.length );
			//System.out.println("offending root is : " + root);
			int childIndexToUse = (genLineage==null || !InternalNodeSet.justAdded(root.getTag(1),root.getSig(1)))?root.getSig(1):(genLineage.getSecond().intValue());
			//System.out.println("oldScores is "+ oldScores[childIndexToUse][root.getSig(0)]);
			scores[cs][ps]=fragmentProbability.computeForComposition(oldScores[childIndexToUse][root.getSig(0)], (short)root.getTag(0), (short)root.getSig(0), top, bottom);
			parentTally[parentState][ps] += scores[cs][ps];
			//use eq 6
		    }
		
		}
		//System.out.println("\tand score is "+scores[cs][ps]);
		
	    }
	}
    }

    public void OLDinitializeScoresAfterCoupling(double[][] oldScores, LatentStatistics LS, Set<Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>> topAndBottomFragments, Set<ProductionTuple> allHypoedFrags){
	for(int cs=0;cs<scores.length;cs++){
	    if(scores[cs]==null) continue;
	    for(int ps=0;ps<scores[cs].length;ps++){
		//System.out.println(cs + "," + ps +" ; " + oldScores.length + ", " + oldScores[cs].length);
		ProductionTuple pt = new ProductionTuple(true);
		pt.addNT(0,(int)parentState,ps);
		pt.addNT(1,(int)childState,cs);
		//or we know of the child from before, but it's never used 
		//in this particular rule
		if(cs<oldScores.length && oldScores[cs]==null) continue;
		//if the child has just been added
		if(cs>=oldScores.length){
		    //any rule part of a fragment MUST have prob = 1.0
		    if(InternalNodeSet.isSubstateInternal(parentState,ps))
			scores[cs][ps]=1.0;
		    else{
			//check to make sure that this fragment was actually hypothesized
			if(!allHypoedFrags.contains(pt)) scores[cs][ps]=0.0;
			else{
			    //ZZZ UPDATE!!!			    
			    //get the fragment that (parentState,ps) -> (childState,cs) defines
			    //note that this frag will be unique
			    //USE EQ 6
			    scores[cs][ps]=1.3;//reestimateProb(scores[cs][ps],LS.getCountOfNTSymbol(parentState,ps));
			}
		    }
		} else{
		    if(ps < oldScores[cs].length){
			if(InternalNodeSet.isSubstateInternal(childState,cs)){
			    //check to make sure that this fragment was actually hypothesized
			    if(!allHypoedFrags.contains(pt))
				scores[cs][ps]=5.0;
			    else{
				//THIS ONE IS PROBLEMATIC!!!
				//ZZZ UPDATE!!!
				//USE EQ 6
				//scores[cs][ps]=1.2;
			    }
			} else {
			    //this is when the fragment defined by this rule already existed
			    //ZZZ UPDATE!!!
			    //Use EQ 5
			    scores[cs][ps]=oldScores[cs][ps];
			}
		    } else{
			if(InternalNodeSet.isSubstateInternal(parentState,ps))
			    if(!allHypoedFrags.contains(pt))
				scores[cs][ps]=0.0;
			    else
				scores[cs][ps]=1.0;
			else throw new Error("Newly added parent should be internal, but it isn't.");		   
		    }
		}
	    } 
	}
    }



    public UnaryRule splitRule(short[] numSubStates, short[] newNumSubStates, int[] offset, Random random, double randomness, boolean doNotNormalize, int mode) {
	// when splitting on parent, never split on ROOT parent
	short parentSplitFactor = this.getParentState() == 0 ? (short)1 : (short)2;
	if (newNumSubStates[this.parentState]==numSubStates[this.parentState]){parentSplitFactor=1;}
	int childSplitFactor = 2;
	if (newNumSubStates[this.childState]==numSubStates[this.childState]){childSplitFactor=1;}
	double[][] oldScores = this.getScores2();
	double[][] newScores = new double[newNumSubStates[this.childState]][];
		
	//for all current substates
	for (short cS = 0; cS < oldScores.length; cS++) {
	    if (oldScores[cS]==null)
		continue;
			
	    //if cS represents internal node, then shift it (but not yet! first just prepare to do so)
	    boolean isChildInternal = InternalNodeSet.isSubstateInternal(this.childState, cS);
	    if(isChildInternal){
		newScores[cS+offset[this.childState]] = new double[newNumSubStates[this.parentState]];
	    } else{ //split as normal
		for (short  c = 0; c < childSplitFactor; c++) {
		    short newCS = (short)(childSplitFactor * cS + c);
		    newScores[newCS]= new double[newNumSubStates[this.parentState]];
		}
	    }

	    for (short pS = 0; pS < oldScores[cS].length; pS++) {
		double score = oldScores[cS][pS];
		
		//if pS represents internal node, then shift
		boolean isParentInternal = InternalNodeSet.isSubstateInternal(this.parentState,pS);

		//split on parent
		if(isParentInternal){
		    if(isChildInternal){ //simple transfer
			newScores[cS+offset[this.childState]][pS+offset[this.parentState]] = score;
		    } else{
			//just transfer one of them
			newScores[childSplitFactor*cS][pS+offset[this.parentState]] = score;
			for(int j=1;j<childSplitFactor;j++)
			    newScores[childSplitFactor*cS+j][pS+offset[this.parentState]] = 0.0;
		    }
		} else{
		    if(isChildInternal){
			newScores[cS+offset[this.childState]][parentSplitFactor*pS] = score;
			for(int j=1; j<parentSplitFactor;j++)
			    newScores[cS+offset[this.childState]][parentSplitFactor*pS + j] = 0.0;
		    } else{
			for (short p = 0; p < parentSplitFactor; p++) {
			    double divFactor = (doNotNormalize) ? 1.0 : childSplitFactor;
			    double randomComponent = score / divFactor * randomness / 100 * (random.nextDouble() - 0.5);
			    // split on child
			    for (short  c = 0; c < childSplitFactor; c++) {
				if (c == 1) {
				    randomComponent *= -1;
				}
				if (childSplitFactor==1){ randomComponent=0; }
				// divide score by divFactor because we're splitting each rule in 1/divFactor
				short newPS = (short)(parentSplitFactor * pS + p);
				short newCS = (short)(childSplitFactor * cS + c);
				double splitFactor = (doNotNormalize) ? 1.0 : childSplitFactor;
				newScores[newCS][newPS] = (score / splitFactor + randomComponent);
				if (mode==2) newScores[newCS][newPS] = 1.0+random.nextDouble()/100.0;
			    }
			}
		    }
		}
	    }
	}
	return new UnaryRule(this,newScores);
    }

}
