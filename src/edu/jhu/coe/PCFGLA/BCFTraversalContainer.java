package edu.jhu.coe.PCFGLA;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.jhu.coe.syntax.BerkeleyCompatibleFragment;
import edu.jhu.coe.syntax.StateSet;
import edu.jhu.coe.syntax.Tree;
import edu.jhu.coe.util.*;
import edu.jhu.coe.util.PriorityQueue;


public class BCFTraversalContainer{
	
    private BerkeleyCompatibleFragment minimallyComplete;
    private Collection<BerkeleyCompatibleFragment> expansionSet;

    @SuppressWarnings("unchecked")
    public BCFTraversalContainer(){
	expansionSet = Collections.EMPTY_LIST;
    }
	
    public BCFTraversalContainer(BerkeleyCompatibleFragment X, Collection<BerkeleyCompatibleFragment> frontierExpansion){
	minimallyComplete = X;
	expansionSet = frontierExpansion;
    }
	
    public Collection<BerkeleyCompatibleFragment> getExpansionSet(){
	return expansionSet==null ? (new LinkedList<BerkeleyCompatibleFragment>()) : expansionSet;
    }

    public BerkeleyCompatibleFragment getMinimalFragment(){
	return minimallyComplete;
    }
}
