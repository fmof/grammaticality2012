package edu.jhu.coe.util;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

//provides easy support for manipulating frequency counts, when dealing with integers
public class IntegerFrequencyMap<K> extends AbstractFrequencyMap<K, Integer> implements Serializable{
 
    public IntegerFrequencyMap(){
        super(Collections.reverseOrder());
    }

    public IntegerFrequencyMap(int sizeThreshold){
	super(Collections.reverseOrder());
	setSizeThreshold(sizeThreshold);
    }

    public IntegerFrequencyMap(Integer valueThreshold){
	super(Collections.reverseOrder());
	setValueThreshold(valueThreshold);
    }

    public Integer get(K element){
	return (containsKey(element)==true)?(super.get(element)):(new Integer(0));
    }
    
    public void clearLeftOver(){
	super.clearLeftOver();
    }

    public Integer increment(K element){
	put(element,new Integer(get(element).intValue()+1));
	return get(element);
    }

    public Integer incrementBy(K element, Integer value){
	put(element, new Integer(get(element).intValue() + value.intValue()));
	return get(element);
    }
}
