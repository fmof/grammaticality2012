package edu.jhu.coe.util;

import java.io.*;
import java.util.HashMap;
import java.util.Map;


public class UniversalTagMapper{
    
    private static Map<String,String> predefNames = new HashMap<String,String>();
    private static Map<String,String> tagMapping = new HashMap<String,String>();
    private static boolean loaded=false;
    private static boolean allToX=false;
    private static String currentMap;

    public static boolean throwErrorsOnNull=false;

    static {
	fillPredefNames();
	tagMapping.put("ROOT","ROOT");
    }

    public static String get(String s){
	if(!loaded)
	    throw new Error("universal tag mapper is not set!");
	if(allToX){
	    int ntidx = s.lastIndexOf("^g");
	    if(ntidx!=-1){
		s=s.substring(0,ntidx);
	    }
	    int bidx = s.indexOf("@");
	    if(bidx!=-1){
		s=s.substring(bidx+1, s.length());
	    }
	}
	String t = tagMapping.get(s);
	if(throwErrorsOnNull && t==null) 
	    throw new Error("No tag for "+s+" in map "+ currentMap);
	
	return t==null?s:t;
    }

    /**
     * Read <code>lang</code> from <code>path</code>.
     */
    public static Map<String,String> load(String path, String lang){
       	if(predefNames.containsKey(lang))
	    lang=predefNames.get(lang);
	//read in file=path/lang
	try{
	    if(lang.contains("all-to-X"))
		allToX=true;
	    FileInputStream fstream = new FileInputStream(path+lang);
	    BufferedReader br = new BufferedReader(new InputStreamReader(new DataInputStream(fstream)));
	    String strline;
	    while((strline=br.readLine())!=null){
		String[] mps = strline.split("\\s");
		if(mps.length>2)
		    throw new Error("line "+strline+" doesn't seem to be a proper mapping");
		tagMapping.put(mps[0],mps[1]);
	    }
	} catch(Exception e){
	    System.err.println("Error reading in universal tag mapping: "+e.getMessage());
	    System.exit(-1);
	}
	currentMap=path+lang;
	loaded=true;
	return new HashMap<String,String>(tagMapping);
    }

    public static void print(){
	System.out.println(tagMapping);
    }

    private static void fillPredefNames(){
	predefNames.put("korean","ko-sejong.map");
	predefNames.put("english","en-ptb.map");
	predefNames.put("ktb2","ko-sejong.map");
	predefNames.put("ptb","en-ptb.map");
	predefNames.put("brown","en-brown.map");
	predefNames.put("all-to-X","all-to-X.map");
	/*
	  do these when I have time... (harhar)
ar-padt.map    da-ddt.map    es-cast3lb.map  it-isst.map       nl-alpino.map  sl-sdt.map         zh-ctb6.map
bg-btb.map     de-negra.map    eu-eus3lb.map   ja-kyoto.map      pt-bosque.map  sv-talbanken.map   zh-sinica.map
ca-cat3lb.map  de-tiger.map  en-tweet.map     fr-paris.map    ja-verbmobil.map  README         tu-metusbanci.map
cs-pdt.map     el-gdt.map     hu-szeged.map   ru-rnc.map */
    }

}