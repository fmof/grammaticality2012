package edu.jhu.coe.syntax;

import edu.jhu.coe.PCFGLA.InternalNodeSet;

import fig.basic.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An easy and (hopefully) efficient way of uniquely 
 * representing fragments, of arbitrary depth, that 
 * is compatible with the shared-structure rule format.
 *
 * @author Frank Ferraro
 */
public class BerkeleyCompatibleFragment{

    private List<ProductionTuple> fragment;
    private ProductionTuple root;
    private int numberOfNodes = 0;
    private int numberInternal = 0;
    private int depth = 0;

    private int currID=0;

    public BerkeleyCompatibleFragment(){
	init();
    }

    private void init(){
	fragment = new ArrayList<ProductionTuple>();
	root = null;
    }

    public BerkeleyCompatibleFragment(ProductionTuple pt){
	init();
	this.addTuple(pt);
    }

    public void clear(){
	fragment.clear();
	root = null;
	numberOfNodes = numberInternal = depth = currID = 0;
    }

    public Pair<Integer,Integer> getRootTagSig(){
	if(root==null) return new Pair<Integer,Integer>(-1,-1);
	return new Pair<Integer,Integer>(root.getTag(0),root.getSig(0));
    }

    public BerkeleyCompatibleFragment addAll(BerkeleyCompatibleFragment other){
	for(ProductionTuple pt : other.fragment){
	    if(!pt.isInternal(0))
		pt.overrideInternal(0,true);
	    this.addTuple(pt);
	}
	return this;
    }

    public void addTuple(ProductionTuple pt){
	if(!pt.isInternal(0)) {
	    if(root!=null){
		System.out.println("offending pt is : " + pt + "\nwhile root = " + root);
		throw new Error("\"Root\" tuple already added to fragment");
	    }
	    root = pt;
	}
	//currID = pt.assignIDs(currID);
	fragment.add(pt);
	numberOfNodes += pt.getNumberOfNodes();
	numberInternal += pt.getNumberOfInternal();
    }

    public List<Pair<Integer,Integer>> getFrontier(){
	List<Pair<Integer,Integer>> rset = new LinkedList<Pair<Integer,Integer>>();
	Set<Pair<Integer,Integer>> set  = new HashSet<Pair<Integer,Integer>>();
	for(ProductionTuple pt : fragment){
	    if(pt.isPreterminal())
		rset.add(new Pair<Integer,Integer>(pt.getTag(0), pt.getSig(0)));
	    else{
		set.add(new Pair<Integer,Integer>(pt.getTag(0),pt.getSig(0)));
	    }
	}
	for(ProductionTuple pt : fragment){
	    if(!pt.isPreterminal()){
		for(int i =0;i<pt.getNumberOfNodes(); i++){
		    Pair<Integer,Integer> pair = new Pair<Integer,Integer>(pt.getTag(i),pt.getSig(i));
		    if(!set.contains(pair))
			rset.add(pair);
		}
	    }
	}
	return rset;
    }

    public boolean isPreterminal(){
	return fragment.size()==1?(fragment.get(0).isPreterminal()):false;
    }
    
    public BerkeleyCompatibleFragment copyWithOffset(int idOffSet){
	BerkeleyCompatibleFragment bcf = new BerkeleyCompatibleFragment();
	//System.err.println("bcf before: "+this +", with offset="+idOffSet);
	for(ProductionTuple pt : fragment){
	    ProductionTuple ptn = pt.copy();
	    ptn.adjustIDs(idOffSet);
	    bcf.addTuple(ptn);
	}
	//System.err.println("\tand after: " + bcf);
	return bcf;
    }

    public BerkeleyCompatibleFragment copy(){
	BerkeleyCompatibleFragment copy = new BerkeleyCompatibleFragment();
	for(ProductionTuple pt : this.fragment)
	    copy.addTuple(pt);
	return copy;
    }

    public static BerkeleyCompatibleFragment compose(BerkeleyCompatibleFragment top, BerkeleyCompatibleFragment bottom){
	BerkeleyCompatibleFragment ret = new BerkeleyCompatibleFragment();
	for(ProductionTuple pt : top.fragment)
	    ret.addTuple(pt);
	for(ProductionTuple pt : bottom.fragment){
	    if(!pt.isInternal(0)) 
		pt.overrideInternal(0,true);
	    ret.addTuple(pt);
	}
	return ret;
    }

    public ProductionTuple getRoot(){
	return root;
    }

    public int[] getTagAndSigFromID(int id){
	int[] ret = new int[2];
	for(ProductionTuple pt : fragment){
	    if(pt.isPreterminal()){
		if(pt.getID(0)==id){
		    ret[0]=pt.getTag(0); ret[1]=pt.getSig(0);
		    return ret;
		}
	    } 
	    for(int i = 0; i < pt.getNumberOfNodes(); i++){
		if(pt.getID(i)==id){
		    ret[0]=pt.getTag(i); ret[1]=pt.getSig(i);
		    return ret;
		}
	    }
	}
	return null;
    }

    /**this will return a copy of this current BCF with all just-added
     * signatures (substates) mapped back to their original form
     */
    public BerkeleyCompatibleFragment remapToOriginal(){
	BerkeleyCompatibleFragment toRet = new BerkeleyCompatibleFragment();
	for(ProductionTuple pt : fragment){
	    toRet.addTuple(pt.remapToOriginal());
	}
	return toRet;
    }
    
    public int getNumberOfProductions(){
	return fragment.size();
    }
    
    public List<ProductionTuple> getTuples(){
	return fragment;
    }

    public String toString(){
	String s = "{";
	for(ProductionTuple pt : fragment)
	    s += pt.toString() + " ";
	return s + "}";
    }

    /*
    public static BerkeleyCompatibleFragment createFromTree(Tree<String> tree, edu.jhu.coe.util.Numberer numberer){
	BerkeleyCompatibleFragment bcf = new BerkeleyCompatibleFragment();
	int nodeNum=0;
	LinkedList<Tree<String>> Q = new LinkedList<Tree<String>>();
	Q.add(tree);
	while(!Q.isEmpty()){
	    Tree<String> t = Q.poll();
	    if(t.isPreterminal()){
		ProductionTuple pt = new ProductionTuple(true);
		pt.addPreterminal(short tag, short sig, nodeNum, t.getChildren().get(0).getLabel());
	    } else{
		switch(t.getChildren().size()){
		case 1:
		    break;
		case 2:
		    break;
		default:
		    throw new Error("malformed tree");
		}
	    }
		nodeNum++;
	}
	return bcf;
    }
    */

    public static BerkeleyCompatibleFragment createFromPairTree(Tree<TagSigOrWord> tree){
	BerkeleyCompatibleFragment bcf = new BerkeleyCompatibleFragment();
	//System.out.println("what is the tree? "+ tree);
	LinkedList<Tree<TagSigOrWord>> Q = new LinkedList<Tree<TagSigOrWord>>();
	Q.add(tree);
	while(!Q.isEmpty()){
	    Tree<TagSigOrWord> t = Q.poll();
	    //System.out.println("pulling t="+t);
	    List<Tree<TagSigOrWord>> children = t.getChildren();
	    int csize=children.size();
	    if(csize==0) continue;
	    Tree<TagSigOrWord> preempt = children.get(0);
	    TagSigOrWord label = preempt.getLabel();
	    ProductionTuple pt;
	    if(label.hasWord()){
		pt = new ProductionTuple(true);
		pt.addPreterminal(t.getLabel().getTag(), t.getLabel().getSig(), t.getNodeNumber(), label.getWord());
	    } else{
		label = t.getLabel();
		if(csize>2 || csize<0)
		    throw new Error("malformed tree " + t + ", from big tree "+ tree);
		pt = new ProductionTuple(csize==1);
		pt.addNT(0, (short)(label.getTag()),(short)(label.getSig()), t.getNodeNumber());
		for(int i=0;i<csize;i++){
		    Tree<TagSigOrWord> c = children.get(i);
		    TagSigOrWord clab = c.getLabel();
		    pt.addNT(i+1,(short)(clab.getTag()),(short)(clab.getSig()), c.getNodeNumber());
		    Q.add(c);
		}
	    }
	    bcf.addTuple(pt);
	}
	return bcf;
    }

    public int getMaximumID(){
	int id=-1;
	for(ProductionTuple pt : fragment){
	    int tmax=pt.getMaximumID();
	    id=tmax>id?tmax:id;
	}
	return id;
    }

    public Tree<String> toCoarseTreeStringWRTTagger(edu.jhu.coe.util.Numberer numberer, boolean useSubstates){
	//add children IDs to Q
	int rootID = this.root.getID(0);
	Map<Integer,ProductionTuple> ptmap = new HashMap<Integer,ProductionTuple>();
	Map<Integer,Tree<String>> tmap = new HashMap<Integer,Tree<String>>();
	for(ProductionTuple pt : fragment){
	    String actString = (String)(numberer.object(pt.getTag(0)));
	    if(useSubstates)
		actString+= "_"+(pt.getSig(0));
	    Tree<String> t = new Tree<String>(actString);
	    if(ptmap.containsKey(pt.getID(0)))
		throw new Error("Woah! This BCF="+this+" has duplicate IDs in production tuple = "+pt);
	    ptmap.put(new Integer(pt.getID(0)), pt);
	    tmap.put(new Integer(pt.getID(0)), t);
	}
	//go through ptmap
	for(Integer id : tmap.keySet()){
	    ProductionTuple pt = ptmap.get(id);
	    Tree<String> t = tmap.get(id);
	    List<Tree<String>> children = new ArrayList<Tree<String>>();
	    if(pt.isPreterminal()){
		children.add(new Tree<String>(pt.getWord()));
	    } else{
		int ptsize=pt.getNumberOfNodes();
		for(int i = 1; i<ptsize; i++){
		    String childActString = (String)(numberer.object(pt.getTag(i)));
		    if(useSubstates)
			childActString+= "_"+(pt.getSig(i));
		    if(tmap.get(new Integer(pt.getID(i))) == null){
			children.add(new Tree<String>(childActString));
		    } else
			children.add(tmap.get(new Integer(pt.getID(i))));
		}
	    }
	    t.setChildren(children);
	    tmap.put(id,t);
	}
	return tmap.get(new Integer(rootID));
    }

    public Tree<String> toTreeStringWRTTaggerNoLatentInternal(edu.jhu.coe.util.Numberer numberer){
	//add children IDs to Q
	int rootID = this.root.getID(0);
	Map<Integer,ProductionTuple> ptmap = new HashMap<Integer,ProductionTuple>();
	Map<Integer,Tree<String>> tmap = new HashMap<Integer,Tree<String>>();
	for(ProductionTuple pt : fragment){
	    String actString = (String)(numberer.object(pt.getTag(0)));
	    if(pt.getID(0)==this.root.getID(0))
		actString += "_"+(pt.getSig(0));
	    Tree<String> t = new Tree<String>(actString);
	    if(ptmap.containsKey(pt.getID(0)))
		throw new Error("Woah! This BCF="+this+" has duplicate IDs in production tuple = "+pt);
	    ptmap.put(new Integer(pt.getID(0)), pt);
	    tmap.put(new Integer(pt.getID(0)), t);
	}
	//go through ptmap
	for(Integer id : tmap.keySet()){
	    ProductionTuple pt = ptmap.get(id);
	    Tree<String> t = tmap.get(id);
	    List<Tree<String>> children = new ArrayList<Tree<String>>();
	    if(pt.isPreterminal()){
		children.add(new Tree<String>(pt.getWord()));
	    } else{
		int ptsize=pt.getNumberOfNodes();
		for(int i = 1; i<ptsize; i++){
		    String childActString = (String)(numberer.object(pt.getTag(i)));
		    if( !tmap.containsKey(pt.getID(i)))
			childActString += "_"+(pt.getSig(i));
		    if(tmap.get(new Integer(pt.getID(i))) == null){
			children.add(new Tree<String>(childActString));
		    } else
			children.add(tmap.get(new Integer(pt.getID(i))));
		}
	    }
	    t.setChildren(children);
	    tmap.put(id,t);
	}
	return tmap.get(new Integer(rootID));
    }

    public boolean equals(Object o){
	if(!(o instanceof BerkeleyCompatibleFragment)) return false;
	BerkeleyCompatibleFragment bcf = (BerkeleyCompatibleFragment)o;
	if(fragment.size() != bcf.fragment.size()) return false;
	
	boolean val = true;
	for(int i=0; i<fragment.size(); i++){
	    boolean valI=false;
	    ProductionTuple ptI = bcf.fragment.get(i);
	    for(int j=0;j<fragment.size();j++){
		valI = valI || ptI.equals(fragment.get(j));
		if(valI) break;
	    }
	    val = val && valI;
	    if(!val) break;
	}
	return val;
    }

    public int hashCode(){
       	int res = 0;
	for(ProductionTuple pt : fragment)
	    res += pt.hashCode();
	return res;
    }

}

