package edu.jhu.coe.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Hashtable;
import java.util.List;

public class IntegerHashtable<K> extends Hashtable<K,Integer> implements Serializable{
        
    public IntegerHashtable(){
	super();
    }

    public void print(){
	System.out.println(this.toString()+"\n");
    }

    public Integer get(K element){
	return containsKey(element)?super.get(element):(new Integer(0));
    }

    public Integer increment(K element){
	put(element,new Integer(get(element).intValue()+1));
	return get(element);
    }

    public Integer incrementBy(K element, Integer value){
	put(element, new Integer(get(element).intValue() + value.intValue()));
	return get(element);
    }

    public Integer put(K element, Integer v){
	super.put(element, v);
	return get(element);
    }
}
