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
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

//provides easy support for manipulating frequency counts                                                                   
public abstract class AbstractFrequencyMap<K,V extends Number & Comparable> extends HashMap<K,V> implements Serializable{

    private int sizeThreshold;
    private boolean sizeThresholdSet;
    private V valueThreshold;
    
    private Comparator<? super V> comparator;
    private TreeMap<V,HashSet<K>> sortedMap;
    private HashMap<K,V> leftOver;

    
    public AbstractFrequencyMap(Comparator<? super V> c){
	super();
	sortedMap = new TreeMap<V,HashSet<K>>(c);
	leftOver = new HashMap<K,V>();
	comparator = c;
    }

    public V setValueThreshold(V v){
	valueThreshold=v;
	return valueThreshold;
    }
    
    public V getValueThreshold(){
	return valueThreshold;
    }

    public int setSizeThreshold(int i){
	sizeThreshold=i;
	sizeThresholdSet=true;
	return sizeThreshold;
    }

    public Map<V,HashSet<K>> getInverseMap(){
	return sortedMap;
    }

    public int getSizeThreshold(){
	return sizeThreshold;
    }

    private void addToNewMappedToSet(TreeMap<V,HashSet<K>> m, K element, V v){
	if(m.containsKey(v)){
	    HashSet<K> set = m.get(v);
	    set.add(element);
	    m.put(v,set);	  
	} else{
	    HashSet<K> set = new HashSet<K>();
	    set.add(element);
	    m.put(v,set);
	}
	    
    }

    private void addToMappedToSet(K element, V v){
	if(containsKey(element)){
	    HashSet<K> set = sortedMap.get(get(element));
	    set.remove(element);
	    sortedMap.put(get(element),set);
	} else{
	    /*HashSet<K> set = new HashSet<K>();
	    set.add(element);
	    sortedMap.put(v,set);*/
	}
    }

    public V put(K element, V v){
	//System.out.println("putting " + v + " to " + element);
	addToMappedToSet(element,v);
	super.put(element, v);
	//now add in the sort stuff
	HashSet<K> set = sortedMap.get(v);
	if(set==null) {
	    set = new HashSet<K>();
	    //System.out.println("FIRST TIME ADDING " + v);
	}
	set.add(element);
	sortedMap.put(v,set);
	return get(element);
    }

    public void merge(AbstractFrequencyMap<K,V> other){
	for(K key : other.keySet()){
	    if(!this.containsKey(key))
		this.incrementBy(key, other.get(key));
	}
    }

    public void print(){
	System.out.println(this.toString()+"\n");
    }

    public String toString(){	
	return super.toString() + "\n\n" + sortedMap.toString();
    }

    public void printMainOnly(){
	System.out.println(super.toString());
    }

    public V get(K element){
	return super.get(element);
    }

    public abstract V increment(K element);

    public abstract V incrementBy(K element, V val);

    public void clearLeftOver(){
	leftOver.clear();
    }
    
    /**
       Trim the map down to either the sizeThreshold first 
       (as given by some comparator) or keep all above/below 
       some comparator.
    */
    public HashMap<K,V> reduce(Map<K,Set<K>> mapToClear, boolean performAnalysis){
	HashMap<K,V> retSet = new HashMap<K,V>();
	TreeMap<V,HashSet<K>> newSortedMap = new TreeMap<V,HashSet<K>>(comparator);
	if(sizeThresholdSet && valueThreshold == null){
	    int currsize=0;
	    IntegerFrequencyMap<V> imf = new IntegerFrequencyMap<V>();
	    for(Map.Entry<V,HashSet<K>> entry : sortedMap.entrySet()){
		V freq = entry.getKey();
		HashSet<K> set = entry.getValue();
		if(currsize > sizeThreshold){
		    for(K k : set){
			if(mapToClear!=null)
			    mapToClear.remove(k);
		   
			//ZZZ analyze
			if(performAnalysis){
			    leftOver.put(k,freq);
			}
			k=null;
			//leftOver.put(k,freq);
		    }
		}
		if(set.size() + currsize <= sizeThreshold){
		    for(K k : set){
			addToNewMappedToSet(newSortedMap,k,freq);
			retSet.put(k,freq);
		    }
		    currsize+=set.size();   
		} else{
		    int numAdded=0;
		    //System.out.println("processing freq="+freq);
		    for(K k : set){		       
			if(currsize+numAdded+1 <= sizeThreshold){
			    retSet.put(k,freq);
			    addToNewMappedToSet(newSortedMap,k,freq);
			    numAdded++;
			} else{
			    if(mapToClear!=null)
				mapToClear.remove(k);
			    //leftOver.put(k,freq);	    
			    //ZZZ analyze
			    if(performAnalysis){
				leftOver.put(k,freq);
			    }
			    k=null;
			}
		    }
		    currsize+=numAdded;
		}
	    }
	    //leftOverIntFrequency.add(imf);
	    //imf.clear(); imf=null;
	    super.clear();
	    super.putAll(retSet);
	    retSet.clear();
	    sortedMap=newSortedMap;
	} else{
	    
	}
	return leftOver;
    }

    public HashMap<K,V> reduce(boolean b){
	return reduce(null,b);
    }

    public HashMap<K,V> reduce(Map<K,Set<K>> mapToClear){
	return reduce(mapToClear, false);
    }

    public HashMap<K,V> reduce(){
	return reduce(null,false);
    }
}
