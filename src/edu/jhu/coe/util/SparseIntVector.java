package edu.jhu.coe.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import edu.jhu.coe.util.IntegerHashtable;

public class SparseIntVector extends IntegerHashtable<Integer> implements Serializable{
    
    public SparseIntVector(){
	super();
    }

    public Integer increment(Integer element){
	put(element, new Integer(get(element).intValue()+1));
	return get(element);
    }

    public Integer increment(Integer element, int i){
	put(element, new Integer(get(element).intValue()+i));
	return get(element);
    }
}
