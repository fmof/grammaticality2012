package edu.jhu.coe.syntax;

import edu.jhu.coe.PCFGLA.InternalNodeSet;
import edu.jhu.coe.syntax.TagSigOrWord;

import fig.basic.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An easy way of indicating a given production.
 * @author Frank Ferraro
 */
public class ProductionTuple{
    private short[] tags;
    private short[] sigs;
    private int[] ids;
    private boolean[] overrideInternal;

    private String word;

    private boolean isUnary;
    private int length;
	
    public ProductionTuple(boolean isUnary){
	toggleUnary(isUnary);
	tags = new short[length];
	sigs = new short[length];
	ids = new int[length];
	overrideInternal = new boolean[length];
    }

    public String getWord(){
	return word;
    }

    public boolean isPreterminal(){
	return (word!=null && word.length()>0);
    }

    /**
     * Get the number of children that are internal.
     */
    public int getNumberOfInternal(){
	int sum=0;
	for(int i = 1; i<length;i++)
	    if(isInternal(i)) sum++;
	return sum;
    }

    public int getNumberOfNodes(){
	return length;
    }

    public void toggleUnary(boolean b){
	isUnary = b;
	length = isUnary ? 2 : 3;
    }

    public void addNT(int position, short tag, short sig, int id){
	addNT(position,tag,sig);
	ids[position] = id;
    }

    public void addNT(int position, int tag, int sig){
	addNT(position, (short)tag, (short)sig);
    }

    public void addNT(int position, int tag, int sig, int id){
	addNT(position, (short)tag, (short)sig, id);
    }

    public void addNT(int position, short tag, short sig){
	if(position>=length || position < 0)
	    throw new Error("trying to create malformed ProductionTuple; length="+length+", position="+position);
	tags[position]=tag;
	sigs[position]=sig;
    }

    public int getMaximumID(){
	if(isPreterminal())
	    return ids[0];
	int id=-1;
	for(int i=0;i<length;i++)
	    id=ids[i]>id?ids[i]:id;
	return id;
    }

    public Tree<TagSigOrWord> toTree(){
	TagSigOrWord label = new TagSigOrWord(tags[0],sigs[0]);
	Tree<TagSigOrWord> tree = new Tree<TagSigOrWord>(label);
	ArrayList<Tree<TagSigOrWord>> children=new ArrayList<Tree<TagSigOrWord>>();
	if(isPreterminal()){
	    TagSigOrWord clabel = new TagSigOrWord(word);
	    Tree<TagSigOrWord> child = new Tree<TagSigOrWord>(clabel);
	    children.add(child);
	} else{
	    for(int i=1;i<length;i++){    
		TagSigOrWord clabel = new TagSigOrWord(tags[i],sigs[i]);
		Tree<TagSigOrWord> child = new Tree<TagSigOrWord>(clabel);
		children.add(child);	
	    }
	}
	tree.setChildren(children);
	return tree;
    }

    public void addPreterminal(int tag, int sig, String word){
	tags[0]=(short)tag; sigs[0]=(short)sig;
	this.word = word;
    }


    public void addPreterminal(short tag, short sig, String word){
	tags[0]=tag; sigs[0]=sig;
	this.word = word;
    }
	
    public void addPreterminal(short tag, short sig, int id, String word){
	addPreterminal(tag,sig,word);
	ids[0]=id;
    }
	
    public void addPreterminal(int tag, int sig, int id, String word){
	addPreterminal((short)tag,(short)sig,word);
	ids[0]=id;
    }

    public int adjustIDs(int offset){
	ids[0] += offset;
	if(!isPreterminal()){
	    for(short i = 1; i< length; i++)
		ids[i] += offset;
	}
	return ids[isPreterminal()?0:length-1]+offset+length;
    }

    public int assignIDs(int id){
	ids[0] = id;
	if(!isPreterminal()){
	    for(short i = 1; i< length; i++)
		ids[i] = (id+1);
	}
	return id+length;
    }

    public void assignID(int pos, int id){
	ids[pos]=id;
    }

    /*public short assignIDs(int id){
	return assignIDs(id);
    }*/

    public void overrideInternal(int position, boolean b){
	overrideInternal[position]=b;
    }

    public Pair<Integer,Integer> getTagSigPiar(int position){
	return new Pair<Integer,Integer>((int)tags[position],(int)sigs[position]);
    }

    public int getTag(int position){
	return tags[position];
    }
    public int getSig(int position){
	return sigs[position];
    }
    public int getID(int position){
	return ids[position];
    }
    
    public boolean isInternal(int position){
	return overrideInternal[position] || InternalNodeSet.isSubstateInternal(tags[position], sigs[position]);
    }

    public ProductionTuple update(int[] offset){
	if(isPreterminal()){
	    if(InternalNodeSet.isSubstateInternal(tags[0],sigs[0]))
		sigs[0]+=offset[tags[0]];
	} else{
	    for(int i=0;i<length;i++){
		//System.out.print("changing "+ sigs[i]+" to ");
		if(InternalNodeSet.isSubstateInternal(tags[i],sigs[i]))
		    sigs[i] += offset[tags[i]];
		//System.out.println(sigs[i]);
	    }
	}
	return this;
    }

    public String toString(){
	if(length==0) return "";
	String s = tags[0]+"_"+sigs[0] + "(" + ids[0] + ")" + " ==> ";
	if(word!=null && word.length() > 0)
	    return s + word;
	for(int i=1;i<length;i++)
	    s += tags[i] + "_" + sigs[i] + "(" + ids[i] + ")" + " ";
	return s;//+" (length = " + length + ")";
    }

    public boolean equals(Object other){
	if(!(other instanceof ProductionTuple)) return false;
	ProductionTuple o = (ProductionTuple)other;
	if(o.length!=this.length) return false;
	if((o.word==null ^ this.word==null)) return false;
	if(this.word!=null && o.word!=null&& !this.word.equals(o.word)) return false;
	for(int i=0;i<length;i++){
	    if(tags[i]!=o.tags[i] || this.sigs[i]!=o.sigs[i])
		return false;
	}
	return true;
    }

    public ProductionTuple remapToOriginal(){
	ProductionTuple pt = this.copy();
	if(isPreterminal()) {
	    pt.sigs[0] = remap(0);
	    return pt;
	}
	for(int i=0;i<length;i++)
	    pt.sigs[i] = remap(i);
	return pt;
    }
    
    private short remap(int pos){
	if(InternalNodeSet.justAdded(tags[pos],sigs[pos])){
	    Pair<Integer,Integer> p = InternalNodeSet.getGeneratingTagAndSigs(tags[pos],sigs[pos]);
	    if(p==null) return sigs[pos];
	    return (short)p.getSecond().intValue();
	}
	return sigs[pos];
    }

    public ProductionTuple copy(){
	ProductionTuple pt = new ProductionTuple(this.isUnary);
	pt.word = this.word;
	for(int i = 0; i < length; i++){
	    pt.tags[i]=this.tags[i];
	    pt.sigs[i] = this.sigs[i];
	    pt.ids[i] = this.ids[i];
	    pt.overrideInternal[i] = this.overrideInternal[i];
	}
	return pt;
    }

    public int hashCode(){
	int res = tags[0] << 16 | sigs[0];
	for(int i = 1; i<length; i++)
	    res += (tags[i] << 16 | sigs[i]);
	res += (word==null || word.length()==0)?0:word.hashCode();
	return res;
    }

} 
