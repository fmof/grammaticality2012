package edu.jhu.coe.PCFGLA;

import edu.jhu.coe.io.PennTreebankReader;
import edu.jhu.coe.syntax.BerkeleyCompatibleFragment;
import edu.jhu.coe.syntax.Tree;
import edu.jhu.coe.util.Numberer;

import fig.basic.Pair;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConstraintSet extends HashSet<Tree<String>> {
    private Map<Tree<String>,Set<Pair<Tree<String>,Integer>>> map;
    private Map<Tree<String>,Set<Pair<Tree<String>,Integer>>> reverseMap;

    private boolean allowAll = false;

    public ConstraintSet(){
	super();
    }

    public ConstraintSet(boolean allowAll){
	super();
	this.allowAll = allowAll;
    }

    private Collection<Tree<String>> readFile(String file){
	Collection<Tree<String>> trees = null;
        
	System.out.println("reading constraint set from " + file);
	try {
	    trees = PennTreebankReader.readTrees(file,Charset.defaultCharset());
	} catch (Exception e) {
	    System.err.println(e);
	    System.exit(1);
	}
	return trees;
    }
    
    public ConstraintSet(String file){
	super();
	//read in the file
	Collection<Tree<String>> trees = readFile(file);
	System.out.println(trees.size());
	parseConstraints(trees,null);
	System.gc();
    }

    public ConstraintSet(String file, Collection<Tree<String>> trees){
	super();
	Collection<Tree<String>> coarseTrees = readFile(file);
	//System.out.println("the coarse trees are "+ coarseTrees);
	parseConstraints(trees, new HashSet<Tree<String>>(coarseTrees));
	System.gc();
    }

    private void parseConstraints(Collection<Tree<String>> trees, Set<Tree<String>> coarseTrees){
	map = new HashMap<Tree<String>,Set<Pair<Tree<String>,Integer>>>();
	boolean cTNotNull = coarseTrees!=null;
	Numberer tagnum = Numberer.getGlobalNumberer("tags");
	
	for (Tree<String> tree : trees) {
	    //make sure that tree.coarsen() is in coarseTrees
	    if(cTNotNull && !coarseTrees.contains(Tree.coarsen(tree))){
		//System.out.println("catching tree="+tree+" with coarse rep="+Tree.coarsen(tree));
		continue;
	    }
	    //parse the root label to see if it's internal
	    //if so, don't bother working with it
	    String rtLab = tree.getLabel();
	    int idx;
	    boolean treeCont=false;
	    if((idx=rtLab.lastIndexOf('_'))!=-1){
		String sym = rtLab.substring(0,idx);
		String sbst= rtLab.substring(idx+1,rtLab.length());
		if(InternalNodeSet.isSubstateInternal(tagnum.number(sym),Integer.parseInt(sbst)))
		   treeCont=true;
	    }
	    if(treeCont) continue;

	    
	    //System.out.println("parsing tree="+tree+" (depth="+tree.getDepth()+") for available constraints");
	    if(tree.getChildren() == null || tree.getChildren()!=null && tree.getChildren().size()==0)
		continue;
	    int nodeNum = 0;
	    Tree<String> root = tree.shallowClone();
	    LinkedList<Tree<String>> Q = new LinkedList<Tree<String>>();
	    Q.add(tree.shallowClone());
	    while(!Q.isEmpty()){
		Tree<String> t = Q.poll();
		if(t.isLeaf()){
		    nodeNum++;
		    continue;
		}
		Map<Tree<String>,Pair<Tree<String>,Integer>> slicemap = root.sliceSubtree(t, nodeNum);
		for(Map.Entry<Tree<String>,Pair<Tree<String>,Integer>> entry : slicemap.entrySet()){
		    Tree<String> stTop = entry.getKey();//Tree.stripLatentInternals(entry.getKey());
		    Pair<Tree<String>,Integer> pair = entry.getValue();
		    Set<Pair<Tree<String>,Integer>> bottoms = map.get(stTop);
		    if(bottoms==null) bottoms = new HashSet<Pair<Tree<String>,Integer>>();
		    Tree<String> stBot = pair.getFirst();//Tree.stripLatentInternals(pair.getFirst());
		    //pair.setFirst(stBot);
		    bottoms.add(pair);
		    //System.out.println("\ttop portion="+stTop+" and bottom="+bottoms);
		    map.put(stTop,bottoms);
		}
		for(Tree<String> c : t.getChildren()){
		    Q.add(c);
		}
		nodeNum++;
	    }
	    Q=null;
	    this.add(tree);
        }
    }

    public void setPermissiveness(boolean allowAll){
	this.allowAll = allowAll;
    }

    public void setMap(Map<Tree<String>,Set<Pair<Tree<String>,Integer>>> m){
	map=m;
	System.out.println(map);
    }

    public boolean add(Tree<String> e){
	return super.add(e);
    }

    public Set<Pair<Tree<String>,Integer>> getTopCouplingSet(Tree<String> topFragment){
	Set<Pair<Tree<String>,Integer>> set = map.get(topFragment);
	return set==null ? (new HashSet<Pair<Tree<String>,Integer>>()) : set;
    }

    public void printMap(){
	System.out.println(map);
    }



    public boolean isCouplingAllowed(BerkeleyCompatibleFragment X, BerkeleyCompatibleFragment Y, int nodeInTree, edu.jhu.coe.util.Numberer tagNumberer, boolean useSubstates){
	//System.out.println("can " + X.toCoarseTreeStringWRTTagger(tagNumberer,useSubstates) + " and " + Y.toCoarseTreeStringWRTTagger(tagNumberer,useSubstates) + " couple? " + nodeInTree);
	Set<Pair<Tree<String>,Integer>> set = map.get(X.toCoarseTreeStringWRTTagger(tagNumberer,useSubstates));
	//System.out.println("\t"+set + " :: " + (set==null ? false : set.contains(new Pair<Tree<String>,Integer>(Y.toCoarseTreeStringWRTTagger(tagNumberer,useSubstates),nodeInTree))));
	return allowAll ? true : (set==null ? false : set.contains(new Pair<Tree<String>,Integer>(Y.toCoarseTreeStringWRTTagger(tagNumberer,useSubstates),nodeInTree)));
    }

}