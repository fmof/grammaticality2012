package edu.jhu.coe.syntax;

import fig.basic.Pair;

public class TagSigOrWord{
    private int tag;
    private int sig;
    private String word;

    public TagSigOrWord(){
	init(-1,-1,null);
    }

    public TagSigOrWord(int t, int s){
	init(t,s,null);
    }
    public TagSigOrWord(String s){
	init(-1,-1,s);
    }
    private void init(int t,int s, String str){
	tag=t; sig=s; word=str;
    }

    public Pair<Integer,Integer> getPair(){
	return new Pair<Integer,Integer>(tag,sig);
    }
    public int getTag(){ return tag;}
    public int getSig(){ return sig;}
    
    public String getWord(){
	return word;
    }

    public boolean hasWord(){
	return word!=null;
    }

    public String toString(){
	if(hasWord()) return word;
	return "("+tag+", "+sig+")";
    }

}