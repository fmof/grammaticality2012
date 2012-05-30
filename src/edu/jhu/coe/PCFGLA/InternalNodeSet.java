package edu.jhu.coe.PCFGLA;

import fig.basic.Pair;

import edu.jhu.coe.syntax.BerkeleyCompatibleFragment;
import edu.jhu.coe.syntax.ProductionTuple;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class InternalNodeSet{

    private static Map<Pair<Integer,Integer>, Long> internalSet = new HashMap<Pair<Integer,Integer>,Long>();
    private static Map<Long, Pair<Integer,Integer>> reverseMap = new HashMap<Long, Pair<Integer,Integer>>();

    /**
     * field so we can ``backtrack:'' given (tag,sig) that is an internal node, get the (tag, sigPrime) that is the same as (tag,sig), but not-internal
    */
    private static Map<Pair<Integer,Integer>,Pair<Integer,Integer>> internalToNotLineage = new HashMap<Pair<Integer,Integer>,Pair<Integer,Integer>>();

    private static Set<Pair<Integer,Integer>> wasJustAdded = new HashSet<Pair<Integer,Integer>>();

    private static Map<Pair<Integer,Integer>,ProductionTuple> ptMap = new HashMap<Pair<Integer,Integer>,ProductionTuple>();

    private static Set<String> previousCouplings = new HashSet<String>();
    //private static Map<Pair<Integer,Integer>,Set<List<Pair<Integer,Integer>>>> previousCouplings = new HashMap<Pair<Integer,Integer>,Set<List<Pair<Integer,Integer>>>>();

    private static int[] numberInternalForTag=new int[0];

    private static long internalNumber=0L;

    public static void resetInternalForTagArray(int len){
	numberInternalForTag=new int[len];
    }

    
    public static String toString2(){
	return internalSet.toString();
    }

    public static long addInternal(int stateID, int substateID,int genStateID, int genSubstateID){
	if(isSubstateInternal(stateID, substateID))
	    return getInternalID(stateID, substateID);
	Pair<Integer,Integer> p = new Pair<Integer,Integer>(stateID, substateID);
	//map p to unique ID
	internalSet.put(p,new Long(internalNumber));
	numberInternalForTag[stateID]++;
	//unique ID to p
	reverseMap.put(new Long(internalNumber), p);
	//p to its top-level ``generating'' fragment
	internalToNotLineage.put(p,new Pair<Integer,Integer>(genStateID, genSubstateID));
	wasJustAdded.add(p);

	internalNumber++;
	return internalNumber-1L;
    }

    public static void addToPTMap(Pair<Integer,Integer> pair, ProductionTuple pt){
	ptMap.put(pair,pt);
    }

    public static void setPriorCouplings(Set<String> priorCouplings){
    //public static void setPriorCouplings(Map<Pair<Integer,Integer>,Set<List<Pair<Integer,Integer>>>> priorCouplings){
	previousCouplings = priorCouplings;
    }

    public static Set<String> getPriorCouplings(){
	return previousCouplings;
    }

    public static Set<ProductionTuple> update(Set<ProductionTuple> set, int[] offset){
	Set<ProductionTuple> retSet =new HashSet<ProductionTuple>();
	for(ProductionTuple pt : set){
	    retSet.add(pt.update(offset));
	}
	return retSet;
    }

    public static void updateMappings(int[] offset){
	Set<Pair<Integer,Integer>> set = new HashSet<Pair<Integer,Integer>>();
	for(Pair<Integer,Integer> p : internalSet.keySet())
	    set.add(p);
	for(Pair<Integer,Integer> p : wasJustAdded)
	    set.add(p);
	for(Pair<Integer,Integer> p : ptMap.keySet())
	    set.add(p);
	
	Map<Pair<Integer,Integer>,ProductionTuple> tempptMap = new HashMap<Pair<Integer,Integer>,ProductionTuple>();
	Map<Pair<Integer,Integer>, Long> tempinternalSet = new HashMap<Pair<Integer,Integer>,Long>();
	Map<Long, Pair<Integer,Integer>> tempreverseMap = new HashMap<Long, Pair<Integer,Integer>>();
	Map<Pair<Integer,Integer>,Pair<Integer,Integer>> tempinternalToNotLineage = new HashMap<Pair<Integer,Integer>,Pair<Integer,Integer>>();
	Set<Pair<Integer,Integer>> tempwasJustAdded = new HashSet<Pair<Integer,Integer>>();

	//update internalSet
	//System.out.println("THE SET IS + " + set);
	for(Pair<Integer,Integer> p : set){
	    Pair<Integer,Integer> np = new Pair<Integer,Integer>(p.getFirst(),offset[p.getFirst().intValue()]+p.getSecond().intValue());
	    //System.out.println("while doing the remapping, p="+p+", np="+np);
	    //if(np.equals(p)) continue;
	    if(internalSet.containsKey(p)){
		tempinternalSet.put(np, internalSet.get(p));
		tempreverseMap.put(internalSet.get(p), np);
		//internalSet.remove(p);
		//reverseMap.remove(p);
		tempinternalToNotLineage.put(np, internalToNotLineage.get(p));
		//reverseMap.remove(p);
	    }
	    if(ptMap.containsKey(p)){
		ProductionTuple old = ptMap.get(p).copy();
		System.out.print("UPDATED: "+ old+" to ");
		ProductionTuple npt = old.update(offset);
		System.out.println(npt);
		//ptMap.remove(p);
		tempptMap.put(np, npt);
	    }
	    if(wasJustAdded.contains(p)){
		tempwasJustAdded.add(np);
		//wasJustAdded.remove(p);
	    }

	}	

	internalSet = tempinternalSet;
	reverseMap = tempreverseMap;
	internalToNotLineage = tempinternalToNotLineage;
	wasJustAdded = tempwasJustAdded;
	ptMap = tempptMap;

	/*
	Map<Pair<Integer,Integer>,Set<List<Pair<Integer,Integer>>>> tempMap = new HashMap<Pair<Integer,Integer>,Set<List<Pair<Integer,Integer>>>>();
	//iterate through keyset of previousCouplings
	for(Pair<Integer,Integer> p : previousCouplings.keySet()){
	    //change by the offset
	    Pair<Integer,Integer> nkp = new Pair<Integer,Integer>(p.getFirst(),offset[p.getFirst().intValue()]+p.getSecond().intValue());
	    //System.out.println("while doing the remapping, p="+p+", np="+np);

	    Set<List<Pair<Integer,Integer>>> ns = new HashSet<List<Pair<Integer,Integer>>>();
	    for(List<Pair<Integer,Integer>> l : previousCouplings.get(p)){
		List<Pair<Integer,Integer>> nl = new LinkedList<Pair<Integer,Integer>>();

		for(Pair<Integer,Integer> p_l : l){
		//for everything old key mapped to
		//change by offset
		    Pair<Integer,Integer> np = new Pair<Integer,Integer>(p_l.getFirst(),offset[p_l.getFirst().intValue()]+p_l.getSecond().intValue());
		    //System.out.println("while doing the remapping, p="+p+", np="+np);
		    nl.add(np);		
		}
		ns.add(nl);
	    }
	    //add to tempMap
	    tempMap.put(nkp,ns);
	}
    	//make previousCouplings point to tempMap
	previousCouplings = tempMap;*/
	//System.out.println("and now, the previousCouplings="+previousCouplings);
	System.gc();
    }

    public static ProductionTuple pollForRule(Pair<Integer,Integer> pair){
	ProductionTuple ret = ptMap.get(pair);
	if(ret!=null){
	    ret = ret.copy();
	} 
	return ret;
    }

    public static Pair<Integer,Integer> getGeneratingTagAndSigs(int tag, int sig){
	return internalToNotLineage.get(new Pair<Integer,Integer>(tag,sig));
    }

    public static int getNumberOfInternalsForTag(int tag){
	return numberInternalForTag[tag];
    }

    public static void resetJustAdded(){
	wasJustAdded.clear();// = new HashSet<Pair<Integer,Integer>>();
    }

    public static void printJustAdded(){
	System.out.println(wasJustAdded);
    }

    public static boolean justAdded(int stateID, int substateID){
	return wasJustAdded.contains(new Pair<Integer,Integer>(stateID, substateID));
    }

    public static boolean isSubstateInternal(int stateID, int substateID){
	return internalSet.containsKey(new Pair<Integer,Integer>(stateID, substateID));
    }
    
    public static long getInternalID(int stateID, int substateID){
	return internalSet.containsKey(new Pair<Integer,Integer>(stateID, substateID))?internalSet.get(new Pair<Integer,Integer>(stateID,substateID)).longValue():-1L;
    }
    
    public static String getStringRepresentation(int stateID, int substateID){
	return isSubstateInternal(stateID, substateID)?
	    ("*" /*+ getInternalID(stateID, substateID)*/):"";
	    }

}