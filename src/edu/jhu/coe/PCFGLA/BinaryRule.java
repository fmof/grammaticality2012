package edu.jhu.coe.PCFGLA;

import edu.jhu.coe.syntax.BerkeleyCompatibleFragment;
import edu.jhu.coe.syntax.ProductionTuple;
import edu.jhu.coe.PCFGLA.InternalNodeSet;
import edu.jhu.coe.PCFGLA.LatentStatistics;
import edu.jhu.coe.util.*;

import fig.basic.Pair;

import java.io.Serializable;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

/**
 * Binary rules (ints for parent, left and right children).
 * Printing of internal representation added by Frank Ferraro.
 *
 * @author Dan Klein
 */

public class BinaryRule extends Rule implements Serializable, java.lang.Comparable {

    public short leftChildState = -1;
    public short rightChildState = -1;
    /**
     * NEW:
     * scores[leftSubState][rightSubState][parentSubState] gives score for this rule
     */
    private double[][][] scores; 


    /**
     * Creates a BinaryRule from String s, assuming it was created using toString().
     *
     * @param s
     */
    /*  public BinaryRule(String s, Numberer n) {
	String[] fields = StringUtils.splitOnCharWithQuoting(s, ' ', '\"', '\\');
	//    System.out.println("fields:\n" + fields[0] + "\n" + fields[2] + "\n" + fields[3] + "\n" + fields[4]);
	this.parent = n.number(fields[0]);
	this.leftChild = n.number(fields[2]);
	this.rightChild = n.number(fields[3]);
	this.score = Double.parseDouble(fields[4]);
	}
    */
    public BinaryRule(short pState, short lState, short rState, double[][][] scores) {
	this.parentState = pState;
	this.leftChildState = lState;
	this.rightChildState = rState;
	this.scores = scores;
    }
  
    public BinaryRule(short pState, short lState, short rState) {
	this.parentState = pState;
	this.leftChildState = lState;
	this.rightChildState = rState;
	//    this.scores = new double[1][1][1];
    }

  
    /** Copy constructor */
    public BinaryRule(BinaryRule b) {
  	this(b.parentState,b.leftChildState,b.rightChildState,ArrayUtil.copy(b.scores));
    }

    public BinaryRule(BinaryRule b, double[][][] newScores) {
  	this(b.parentState,b.leftChildState,b.rightChildState,newScores);
    }

    public BinaryRule(short pState, short lState, short rState, short pSubStates, int lSubStates, int rSubStates) {
        this.parentState = pState;
        this.leftChildState = lState;
        this.rightChildState = rState;
        this.scores = new double[lSubStates][rSubStates][pSubStates];
    }

    public int hashCode() {
	return ((int)parentState << 16) ^ ((int)leftChildState << 8) ^ ((int)rightChildState);
    }

    public boolean equals(Object o) {
	if (this == o) {
	    return true;
	}
	if (o instanceof BinaryRule) {
	    BinaryRule br = (BinaryRule) o;
	    if (parentState == br.parentState && 
		leftChildState == br.leftChildState &&
		rightChildState == br.rightChildState) {
		return true;
	    }
	}
	return false;
    }

    private static final char[] charsToEscape = new char[]{'\"'};


    public String toStringMJ() {
	Numberer n = Numberer.getGlobalNumberer("tags");
	String lState = (String)n.object(leftChildState);
	String rState = (String)n.object(rightChildState);
	String pState = (String)n.object(parentState);
	StringBuilder sb = new StringBuilder();
	if (scores==null) return "";//pState+" -> "+lState+" "+rState+"\n";
	for (int lS=0; lS<scores.length; lS++){
	    for (int rS=0; rS<scores[lS].length; rS++){
    		if (scores[lS][rS]==null) continue;
    		for (int pS=0; pS<scores[lS][rS].length; pS++){
		    double p = scores[lS][rS][pS]; 
		    if (p>0)
			sb.append(p +" " +pState + "_" + pS + InternalNodeSet.getStringRepresentation(parentState, pS) + " --> " + lState + "_" + lS + InternalNodeSet.getStringRepresentation(leftChildState, lS) +" " + rState + "_" + rS +InternalNodeSet.getStringRepresentation(rightChildState, rS) + " " + "\n");
		}
	    }
	}
	return sb.toString();
    }

    public String toString() {
	Numberer n = Numberer.getGlobalNumberer("tags");
	String lState = (String)n.object(leftChildState);
	String rState = (String)n.object(rightChildState);
	String pState = (String)n.object(parentState);
	StringBuilder sb = new StringBuilder();
	if (scores==null) return pState+" -> "+lState+" "+rState+"\n";
	//sb.append(pState+ " -> "+lState+ " "+rState+ "\n");
	for (int lS=0; lS<scores.length; lS++){
	    for (int rS=0; rS<scores[lS].length; rS++){
    		if (scores[lS][rS]==null) continue;
    		for (int pS=0; pS<scores[lS][rS].length; pS++){
		    double p = scores[lS][rS][pS]; 
		    if (p>0)
			sb.append(pState + "_" + pS + InternalNodeSet.getStringRepresentation(parentState, pS) + " -> " + lState + "_" + lS + InternalNodeSet.getStringRepresentation(leftChildState, lS) +" " + rState + "_" + rS +InternalNodeSet.getStringRepresentation(rightChildState, rS) + " " + p + "\n");
		    //sb.append(pState+"_"+pS+ " -> "+lState+"_"+lS+ " "+rState+"_"+rS +" "+p+"\n");
    		}
	    }
	}
	return sb.toString();
    }
    
    
    public String toString_old() {
	Numberer n = Numberer.getGlobalNumberer("tags");
	return "\"" + 
	    StringUtils.escapeString(n.object(parentState).toString(), charsToEscape, '\\') + 
	    "\" -> \"" + 
	    StringUtils.escapeString(n.object(leftChildState).toString(), charsToEscape, '\\') + 
	    "\" \"" + 
	    StringUtils.escapeString(n.object(rightChildState).toString(), charsToEscape, '\\') + 
	    "\" " + ArrayUtil.toString(scores);
    }

    public int compareTo(Object o) {
	BinaryRule ur = (BinaryRule) o;
	if (parentState < ur.parentState) {
	    return -1;
	}
	if (parentState > ur.parentState) {
	    return 1;
	}
	if (leftChildState < ur.leftChildState) {
	    return -1;
	}
	if (leftChildState > ur.leftChildState) {
	    return 1;
	}
	if (rightChildState < ur.rightChildState) {
	    return -1;
	}
	if (rightChildState > ur.rightChildState) {
	    return 1;
	}
	return 0;
    }


    public short getLeftChildState() {
	return leftChildState;
    }

    public short getRightChildState() {
	return rightChildState;
    }
  
    public double getScore(int pS, int lS, int rS){
  	// gets the score for a particular combination of substates
  	if (scores[lS][rS]==null) {
	    if (logarithmMode)
		return Double.NEGATIVE_INFINITY;
	    return 0;
  	}
  	return scores[lS][rS][pS];
    }
  
    public void setScores2(double[][][] scores){
  	this.scores = scores;
    }

    /**
     * scores[leftSubState][rightSubState][parentSubState] gives score for this rule
     */
    public double[][][] getScores2(){
  	return scores;
    }
  
    public void setNodes(short pState, short lState, short rState){
  	this.parentState = pState;
  	this.leftChildState = lState;
  	this.rightChildState = rState;
    }

    public void incrementPartitionFunction(double[][] pf, short[] numSigsPerTag){
	if (logarithmMode)
	    throw new Error("cannot compute partition function in logarithm mode!");
	if(pf[parentState]==null) pf[parentState] = new double[numSigsPerTag[parentState]];
	for(int lcs=0;lcs<scores.length;lcs++){
	    if(scores[lcs]==null) continue;
	    for(int rcs=0;rcs<scores[lcs].length;rcs++){
		if(scores[lcs][rcs]==null) continue;
		for(int ps=0;ps<scores[lcs][rcs].length;ps++){
		    pf[parentState][ps]+=scores[lcs][rcs][ps];
		}
	    }
	}
    }


    private static final long serialVersionUID = 2L;

    public void initializeScoresAfterCoupling(double[][][] oldScores, double[][] parentTally, FragmentProbabilityComputer fragmentProbability, Map<Vector<Integer>,Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>> startingFragmentMap, Set<Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>> topAndBottomFragments, Set<ProductionTuple> allHypoedFrags){
	//System.out.println("for rule="+this);
	for(int lcs=0;lcs<scores.length;lcs++){
	    //System.out.println("\tlcs="+lcs);
	    for(int rcs=0;rcs<scores[lcs].length;rcs++){
		//System.out.println("\t\trcs="+rcs);
		if(scores[lcs][rcs]==null || 
		   (lcs < oldScores.length && oldScores[lcs]!=null && 
		    rcs < oldScores[lcs].length && oldScores[lcs][rcs]==null)) continue;
		for(int ps=0;ps<scores[lcs][rcs].length;ps++){
		    //System.out.println("\t\t\tps");
		    //System.out.println("Really weird: " + lcs + " , " + rcs + " ,, " + ps);
		    //System.out.println("\tscores[lcs][rcs].length="+scores[lcs][rcs].length);
		    ProductionTuple pt = new ProductionTuple(false);
		    pt.addNT(0,(int)parentState,ps);
		    pt.addNT(1,(int)leftChildState,lcs);
		    pt.addNT(2,(int)rightChildState,rcs);

		    //or we know of the child from before, but it's never used 
		    //in this particular rule
		    Vector<Integer> vec = new Vector<Integer>(6);
		    vec.add(new Integer(parentState)); vec.add(new Integer(ps));
		    vec.add(new Integer(leftChildState)); vec.add(new Integer(lcs));
		    vec.add(new Integer(rightChildState)); vec.add(new Integer(rcs));
		    Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment> bcfPair = startingFragmentMap.get(vec);
		    boolean parentInternal = InternalNodeSet.isSubstateInternal(parentState,ps);
		    if(bcfPair==null) {
			//since bcf is null, then we obviously didn't deal with this rule starting any fragment
			//we need to see if this is part of a valid fragment (either previously or recently hypothesized)
			//if not, then set to 0
			if(!allHypoedFrags.contains(pt)){
			    //System.out.println("THROWING OUT : " + pt);
			    scores[lcs][rcs][ps]=0.0;
			} else{
			    //System.out.println("or here?");
			    scores[lcs][rcs][ps]=1.0;
			    parentTally[parentState][ps] += scores[lcs][rcs][ps];
			}
			continue;
		    }
		    //System.out.println(bcfPair);
		    if(!allHypoedFrags.contains(pt)){
			scores[lcs][rcs][ps]=-1.0;
			throw new Error("how can bcf be non-null but the initial PT not be part of the hypothesized/concerned production tuple set?\n\tbcfPair=" + bcfPair + "\n\tPT =" +pt+"\n\thypoedFrags ="+ allHypoedFrags+"\n\tstartingFragmentMap="+startingFragmentMap);
		    } else{
			//System.out.println(parentState + ":" + ps + ", " +leftChildState+":"+lcs+", " +rightChildState+":"+rcs+" => " + bcfPair);
			BerkeleyCompatibleFragment top = bcfPair.getFirst();
			BerkeleyCompatibleFragment bottom = bcfPair.getSecond();
			if(bottom==null){
			    //use eq 5
			    scores[lcs][rcs][ps]=fragmentProbability.computeForFragment(oldScores[lcs][rcs][ps],parentState, (short)ps, top);
			    parentTally[parentState][ps] += scores[lcs][rcs][ps];
			
			} else{
			    //System.out.println("top is " + top);
			    ProductionTuple root = top.getRoot();
			    //System.out.println("binary init score, top="+top+", bottom="+bottom);
			    //need oldScores[top.tag][top.sig]
			    Pair<Integer,Integer> genLLineage = InternalNodeSet.getGeneratingTagAndSigs(leftChildState,lcs);
			    Pair<Integer,Integer> genRLineage = InternalNodeSet.getGeneratingTagAndSigs(rightChildState,rcs);
			    if(genLLineage==null && genRLineage==null)
				throw new Error("Somehow the lineage wasn't properly set");
			    //System.out.println(genLLineage+ " ... " + genRLineage);
			    //System.out.println("offending root is : " + root);
			    //check the logic here...
			    int leftIndexToUse = (genLLineage==null || !InternalNodeSet.justAdded(root.getTag(1),root.getSig(1)))?root.getSig(1):(genLLineage.getSecond().intValue());
			    int rightIndexToUse = (genRLineage==null || !InternalNodeSet.justAdded(root.getTag(2), root.getSig(2)))?root.getSig(2):(genRLineage.getSecond().intValue());
			    scores[lcs][rcs][ps]=fragmentProbability.computeForComposition(oldScores[leftIndexToUse][rightIndexToUse][root.getSig(0)], (short)root.getTag(0), (short)root.getSig(0), top, bottom);
			    parentTally[parentState][ps] += scores[lcs][rcs][ps];
			    //use eq 6
			}
		    }

		}
	    }
	}
    }

    public void OLDinitializeScoresAfterCoupling(double[][][] oldScores, LatentStatistics LS, Set<Pair<BerkeleyCompatibleFragment,BerkeleyCompatibleFragment>> topAndBottomFragments, Set<ProductionTuple> allHypoedFrags){
	for(int lcs=0;lcs<scores.length;lcs++){
	    for(int rcs=0;rcs<scores[lcs].length;rcs++){
		if(scores[lcs][rcs]==null || 
		   (lcs < oldScores.length && oldScores[lcs]!=null && rcs < oldScores[lcs].length && oldScores[lcs][rcs]==null)) continue;
		for(int ps=0;ps<scores[lcs][rcs].length;ps++){
		    ProductionTuple pt = new ProductionTuple(false);
		    pt.addNT(0,(int)parentState,ps);
		    pt.addNT(1,(int)leftChildState,lcs);
		    pt.addNT(2,(int)rightChildState,rcs);
		    if(lcs>= oldScores.length || rcs >= oldScores[lcs].length) {
			if(InternalNodeSet.isSubstateInternal(parentState,ps)){
			    if(!allHypoedFrags.contains(pt)){
				//System.out.println("BCaught");
				scores[lcs][rcs][ps]=0.0;
			    }
			    else 
				scores[lcs][rcs][ps]=1.0;
			}
			else{
			    
			    scores[lcs][rcs][ps]=1.3;
			}
		    } else{
			if(ps < oldScores[lcs][rcs].length){
			    if(InternalNodeSet.isSubstateInternal(leftChildState,lcs) ||
			       InternalNodeSet.isSubstateInternal(rightChildState,rcs)){
				scores[lcs][rcs][ps]=1.2;
			    } else
				scores[lcs][rcs][ps]=oldScores[lcs][rcs][ps];
			}
			else{
			    if(InternalNodeSet.isSubstateInternal(parentState,ps))
				if(!allHypoedFrags.contains(pt))
				    scores[lcs][rcs][ps]=0.0;
				else 
				    scores[lcs][rcs][ps]=1.0;
			    else{
				scores[lcs][rcs][ps]=5.0;
			    }
			}
		    }
		}
	    }
	}
    }


    public BinaryRule splitRule(short[] numSubStates, short[] newNumSubStates, int[] offset, Random random, double randomness, boolean doNotNormalize, int mode) {
	// when splitting on parent, never split on ROOT
	//System.out.println("SPLITTING " + this);
	//System.out.println("\t"+this.leftChildState+"->"+newNumSubStates[this.leftChildState]+", " +this.rightChildState+"->"+ newNumSubStates[this.rightChildState]);
	int parentSplitFactor = this.getParentState() == 0 ? 1 : 2;
	if (newNumSubStates[this.parentState]==numSubStates[this.parentState]){parentSplitFactor=1;}
	int lChildSplitFactor = 2;
	if (newNumSubStates[this.leftChildState]==numSubStates[this.leftChildState]){lChildSplitFactor=1;}
	int rChildSplitFactor = 2;
	if (newNumSubStates[this.rightChildState]==numSubStates[this.rightChildState]){rChildSplitFactor=1;}
		
	double[][][] oldScores =  this.getScores2();
	double[][][] newScores = new double[newNumSubStates[this.leftChildState]][newNumSubStates[this.rightChildState]][];
	//double[][][] newScores = new double[oldScores.length * lChildSplitFactor][oldScores[0].length * rChildSplitFactor][];
	//for all current substates
	for (short lcS = 0; lcS < oldScores.length; lcS++) {
	    boolean isLeftChildInternal = InternalNodeSet.isSubstateInternal(this.leftChildState, lcS);
	    for (short rcS = 0; rcS < oldScores[0].length; rcS++) {
		if (oldScores[lcS][rcS]==null)
		    continue;
		
		boolean isRightChildInternal = InternalNodeSet.isSubstateInternal(this.rightChildState, rcS);
	
		//first, deal with initialization
		//this could be better, but the deadline is waaaaaaaaaaaay too close!
		if(isLeftChildInternal || isRightChildInternal){
		    if(isLeftChildInternal && !isRightChildInternal){
			short newLCS = (short)(lcS+offset[this.leftChildState]);
			for (short rc = 0; rc < rChildSplitFactor; rc++) {
			    short newRCS = (short)(rChildSplitFactor * rcS + rc);
			    newScores[newLCS][newRCS] = new double[newNumSubStates[ this.parentState]];
			}
		    } else if(!isLeftChildInternal){
			short newRCS = (short)(rcS + offset[this.rightChildState]);
			for (short lc = 0; lc < lChildSplitFactor; lc++) {
			    short newLCS = (short)(lChildSplitFactor * lcS + lc);
			    newScores[newLCS][newRCS] = new double[newNumSubStates[ this.parentState]];
			}
		    } else{
			newScores[lcS+offset[this.leftChildState]][rcS+offset[this.rightChildState]] = new double[newNumSubStates[this.parentState]];
		    }
		}else{
		    for (short lc = 0; lc < lChildSplitFactor; lc++) {
			for (short rc = 0; rc < rChildSplitFactor; rc++) {
			    short newLCS = (short)(lChildSplitFactor * lcS + lc);
			    short newRCS = (short)(rChildSplitFactor * rcS + rc);
			    newScores[newLCS][newRCS] = new double[newNumSubStates[ this.parentState]];
			}
		    }
		}
				 
		//now for the actual copying
		for (short pS = 0; pS < oldScores[lcS][rcS].length; pS++) {
		    double score = oldScores[lcS][rcS][pS];
		    if(InternalNodeSet.isSubstateInternal(this.parentState, pS)){
			short newPS = (short)(pS+offset[this.parentState]);
			if(isLeftChildInternal && !isRightChildInternal){
			    short newLCS = (short)(lcS+offset[this.leftChildState]);
			    newScores[newLCS][rChildSplitFactor*rcS][newPS]=score;
			    for (short rc = 1; rc < rChildSplitFactor; rc++) {
				short newRCS = (short)(rChildSplitFactor * rcS + rc);
				newScores[newLCS][newRCS][newPS] = 0.0;
			    }
			} else if(!isLeftChildInternal && isRightChildInternal){
			    short newRCS = (short)(rcS + offset[this.rightChildState]);
			    newScores[lChildSplitFactor*lcS][newRCS][newPS] = score;
			    for (short lc = 1; lc < lChildSplitFactor; lc++) {
				short newLCS = (short)(lChildSplitFactor * lcS + lc);
				newScores[newLCS][newRCS][newPS] = 0.0;
			    }
			} else if(!isLeftChildInternal && !isRightChildInternal){
			    newScores[lChildSplitFactor*lcS][rChildSplitFactor*rcS][newPS]=score;
			    for(short lc=1;lc<lChildSplitFactor;lc++){
				for(short rc=1;rc<rChildSplitFactor;rc++){
				    newScores[lChildSplitFactor*lcS+lc][rChildSplitFactor*rcS+rc][newPS]=0.0;
				}
			    }
			} else{
			    newScores[lcS+offset[this.leftChildState]][rcS+offset[this.rightChildState]][pS+offset[this.parentState]] = score;
			}
		    } else{
			if(isLeftChildInternal && !isRightChildInternal){
			    short newLCS = (short)(lcS+offset[this.leftChildState]);
			    newScores[newLCS][rChildSplitFactor*rcS][parentSplitFactor*pS] = score;
			    for (short rc = 1; rc < rChildSplitFactor; rc++) {
				for(short pc=1; pc<parentSplitFactor;pc++){
				    short newRCS = (short)(rChildSplitFactor * rcS + rc);
				    newScores[newLCS][newRCS][parentSplitFactor*pS+pc] = 0.0;
				}
			    }
			} else if(!isLeftChildInternal && isRightChildInternal){
			    short newRCS = (short)(rcS + offset[this.rightChildState]);
			    newScores[lChildSplitFactor*lcS][newRCS][parentSplitFactor*pS] = score;
			    for (short lc = 1; lc < lChildSplitFactor; lc++) {
				for(short pc=1;pc<parentSplitFactor; pc++){
				    short newLCS = (short)(lChildSplitFactor * lcS + lc);
				    newScores[newLCS][newRCS][parentSplitFactor*pS+pc] = 0.0;
				}
			    }
			} else if(isLeftChildInternal && isRightChildInternal) {
			    newScores[lcS+offset[this.leftChildState]][rcS+offset[this.rightChildState]][parentSplitFactor*pS] = score;
			    for(short pc=1;pc<parentSplitFactor;pc++){
				newScores[lcS+offset[this.leftChildState]][rcS+offset[this.rightChildState]][parentSplitFactor*pS+pc] = 0.0;
			    }
			} else{
			    for (short p = 0; p < parentSplitFactor; p++) {
				double divFactor = (doNotNormalize) ? 1.0 : lChildSplitFactor * rChildSplitFactor;
				double randomComponentLC = score / divFactor * randomness / 100
				    * (random.nextDouble() - 0.5);
				// split on left child
				for (short lc = 0; lc < lChildSplitFactor; lc++) {
				    // reverse the random component for half of the rules
				    if (lc == 1) { randomComponentLC *= -1; }
				    // don't add randomness if we're not splitting
				    if (lChildSplitFactor==1){ randomComponentLC = 0;}
				    double randomComponentRC = score / divFactor * randomness / 100
					* (random.nextDouble() - 0.5);
				    // split on right child
				    for (short rc = 0; rc < rChildSplitFactor; rc++) {
					// reverse the random component for half of the rules
					if (rc == 1) {
					    randomComponentRC *= -1;
					}
					// don't add randomness if we're not splitting
					if (rChildSplitFactor==1){ randomComponentRC = 0;}
					// set new score; divide score by 4 because we're dividing each
					// binary rule under a parent into 4
					short newPS = (short)(parentSplitFactor * pS + p);
					short newLCS = (short)(lChildSplitFactor * lcS + lc);
					short newRCS = (short)(rChildSplitFactor * rcS + rc);
					double splitFactor = (doNotNormalize) ? 1.0 : lChildSplitFactor * rChildSplitFactor; 
					newScores[newLCS][newRCS][newPS] = 
					    (score / (splitFactor) + randomComponentLC + randomComponentRC);
					if (mode==2) newScores[newLCS][newRCS][newPS] = 1.0+random.nextDouble()/100.0;
				    }
				}
			    }
			}
		    }
		}
	    }
	}
	BinaryRule newRule = new BinaryRule(this,newScores);
	return newRule;

		
    }

}
