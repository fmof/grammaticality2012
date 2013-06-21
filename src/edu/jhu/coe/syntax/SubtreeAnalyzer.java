package edu.jhu.coe.syntax;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Serializable;

import java.lang.String;

import java.nio.charset.Charset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.jhu.coe.io.PennTreebankReader;

import edu.jhu.coe.syntax.Fragment;
import edu.jhu.coe.syntax.Tree;
import edu.jhu.coe.syntax.Trees;
import edu.jhu.coe.syntax.Trees.TreeTransformer;
import edu.jhu.coe.syntax.Trees.PennTreeReader;

import edu.jhu.coe.util.AbstractFrequencyMap;
import edu.jhu.coe.util.IntegerFrequencyMap;
import edu.jhu.coe.util.IntegerHashtable;

import edu.jhu.coe.PCFGLA.Binarization;
import edu.jhu.coe.PCFGLA.Corpus;
import edu.jhu.coe.PCFGLA.Corpus.TreeBankType;
import edu.jhu.coe.PCFGLA.OptionParser;
import edu.jhu.coe.PCFGLA.Option;
import edu.jhu.coe.PCFGLA.TreeAnnotations;

import edu.jhu.coe.util.SparseIntVector;
import edu.jhu.coe.util.UniversalTagMapper;

/**
 * Perform subtree analysis
 * @author Frank Ferraro
 */

public abstract class SubtreeAnalyzer implements Serializable{

    public static Options opts;
    IntegerFrequencyMap<Tree<Integer>> fragmentFrequency;
    Map<Tree<Integer>,Set<Tree<Integer>>> fragmentContribution = new HashMap<Tree<Integer>, Set<Tree<Integer>>>();
    List<IntegerFrequencyMap<Integer>> leftOverFrequency;

    IntegerHashtable<Tree<Integer>> fragmentLookup;
    ArrayList<Tree<Integer>> inverseFragmentLookup;

    // the corpus of trees
    protected boolean corpusLoaded_;
    // directory containing the training data
    protected String corpusFile_;
    protected List<Tree<Integer>> corpus_;

    protected Corpus corpus;
    protected boolean applyUniversalMapping;

    protected boolean printIncrementally=false;

    public SubtreeAnalyzer(){
	init();
    }

    public SubtreeAnalyzer(edu.jhu.coe.syntax.SubtreeAnalyzer.Options opts){
	applyUniversalMapping = opts.languageForUniversalTags!=null;
	init();
	
    }

    private void init(){
	fragmentFrequency = new IntegerFrequencyMap<Tree<Integer>>();
	if(applyUniversalMapping){
	    UniversalTagMapper.load(opts.universalTagLocation, opts.languageForUniversalTags);
	}
	UniversalTagMapper.print();
    }

    public List<Tree<Integer>> getCorpus() {
        if (! corpusLoaded_)
            System.err.println("* WARNING: corpus not loaded");
        return corpus_;
    }

    public List<Tree<Integer>> setCorpus(List<Tree<String>> intrees){
	corpus_ = new ArrayList<Tree<Integer>>();
	for (Tree<String> tree : intrees) {
            Tree<Integer> sTree = Trees.stringTreeToIntegerTree(tree);
            corpus_.add(sTree);
        }
	corpusLoaded_=true;
	return corpus_;
    }

    public List<Tree<Integer>> setCorpus(){
	return setCorpus(null,null);
    }

    public List<Tree<Integer>> setCorpus(String path, TreeBankType tb){
	path = (path==null)?opts.corpus:path;
	tb = (tb==null)?opts.treebank:tb;
	System.out.println("Loading trees from "+path+" and using language "+ tb);

	corpus = new Corpus(path,tb,opts.trainingFractionToKeep,false, opts.skipSection, applyUniversalMapping);
	Collection<Tree<String>> trees = corpus.getTrainTrees();
        
        Trees.TreeTransformer treeTransformer = new Trees.StandardTreeNormalizer();

        corpus_ = new ArrayList<Tree<Integer>>();
        for (Tree<String> tree : trees) {
            Tree<String> normalizedTree = opts.binarize ? TreeAnnotations.processTree(treeTransformer.transformTree(tree), opts.verticalMarkovization, opts.horizontalMarkovization, opts.binarization, false, false, true) : treeTransformer.transformTree(tree);
            corpus_.add(Trees.stringTreeToIntegerTree(normalizedTree));
        }
        corpusLoaded_ = true;
	corpusFile_ = path;
        System.out.println("Loaded " + corpus_.size() + " trees from '" + path + "'");

	return corpus_;
    }

    public List<Tree<Integer>> setCorpus(String corpusFile,int from,int to) {
        corpusFile_ = corpusFile;
        loadCorpus(from,to);
        return corpus_;
    }

    public void dumpCorpus(String fileName) {
        try {
            BufferedOutputStream out = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(fileName)));
            for (Tree<Integer> tree : getCorpus()) {
                out.write(Trees.integerTreeToStringTree(tree).toString().getBytes());
                out.write("\n".getBytes());
            }
            out.close();
        } catch (IOException e) {
            System.out.println("error dumping counts: " + e);
        }
    }

    public void loadCorpus(int start, int end) {
        Collection<Tree<String>> trees = null;
        File corpusPath = new File(corpusFile_);
        if (corpusPath.isDirectory()) {
            trees = PennTreebankReader.readTrees(corpusFile_,start,end,Charset.defaultCharset());
        } else {
            try {
                trees = PennTreebankReader.readTrees(corpusFile_,Charset.defaultCharset());
            } catch (Exception e) {
                System.err.println(e);
                System.exit(1);
            }
        }

        Trees.TreeTransformer treeTransformer = new Trees.StandardTreeNormalizer();
	Trees.UniversalTagSetTransformer utTrans = new Trees.UniversalTagSetTransformer();
        
        corpus_ = new ArrayList<Tree<Integer>>();
        for (Tree<String> tree : trees) {
            Tree<String> normalizedTree = opts.binarize ? TreeAnnotations.processTree(treeTransformer.transformTree(tree), opts.verticalMarkovization, opts.horizontalMarkovization, opts.binarization, false, false, true) : treeTransformer.transformTree(tree);

            corpus_.add(Trees.stringTreeToIntegerTree(applyUniversalMapping?utTrans.transformTree(normalizedTree):normalizedTree));
        }
        corpusLoaded_ = true;
        System.out.println("Loaded " + corpus_.size() + " trees from '" + corpusFile_ + "'");
	
    }

    public List<Tree<Integer>> loadCorpusInto(String file, int start, int end) {
        Collection<Tree<String>> trees = null;
        File corpusPath = new File(file);
        if (corpusPath.isDirectory()) {
            trees = PennTreebankReader.readTrees(file,start,end,Charset.defaultCharset());
        } else {
            try {
                trees = PennTreebankReader.readTrees(file,Charset.defaultCharset());
            } catch (Exception e) {
                System.err.println(e);
                System.exit(1);
            }
        }

	
        Trees.TreeTransformer treeTransformer = new Trees.StandardTreeNormalizer();

        List<Tree<Integer>> otherCorpus = new ArrayList<Tree<Integer>>();
        for (Tree<String> tree : trees) {
            Tree<String> normalizedTree = treeTransformer.transformTree(tree);

	    otherCorpus.add(Trees.stringTreeToIntegerTree(normalizedTree));
        }
        
        System.out.println("Loaded " + otherCorpus.size() + " trees from '" + file + "'");
	return otherCorpus;
    }


    public void dumpFragmentToGZ(String fileName) {
        try {
            BufferedOutputStream out = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(fileName)));
	    for(Integer key : fragmentFrequency.getInverseMap().keySet()){
		HashSet<Tree<Integer>> set = fragmentFrequency.getInverseMap().get(key);
		for(Tree<Integer> T : set){
		    out.write((key + " " + getHumanTree(T)).getBytes());
		    out.write("\n".getBytes());
		}
	    }	
            out.close();
        } catch (IOException e) {
            System.out.println("error dumping counts: " + e);
        }
    }

    //ZZZ TO DO
    /*
    public void readFragmentFromGZ(String fileName) {
        try {
            BufferedInputStream in = new BufferedInputStream(new GZIPInputStream(new FileInputStream(fileName)));
	    for(Integer key : fragmentFrequency.getInverseMap().keySet()){
		HashSet<Tree<Integer>> set = fragmentFrequency.getInverseMap().get(key);
		for(Tree<Integer> T : set){
		    out.write((key + " " + getHumanTree(T)).getBytes());
		    out.write("\n".getBytes());
		}
	    }	
            out.close();
        } catch (IOException e) {
            System.out.println("error dumping counts: " + e);
        }
    }
    */

    private boolean parseFragmentFrequency(IntegerHashtable<Tree<Integer>> vec, String line, Pattern pattern, int val){
	//discard anything after a '#'
	int end = line.indexOf("#");
	String tline;
	if(end<0) {
	    tline=line;
	} else
	    tline = line.substring(0,line.indexOf("#"));
	if(tline.isEmpty()) return false;
	//line should be ^(\d+)\s+(\(.+\))$
	Matcher m = pattern.matcher(tline);
	if(!m.matches()) return false;
	String s = m.group(2).replaceAll("_([^\\\\S\\\\)\\\\(]+)_","$1");
	//remove matching underscores
	Tree<Integer> t = Trees.stringTreeToIntegerTree(Trees.PennTreeReader.parseEasy(s));
	fragmentLookup.put(t,new Integer(val));
	inverseFragmentLookup.add(new Integer(val), t);
	/*Set<Tree<Integer>> fragmentStartSet = fragmentLookupStart.contains(t.getLabel())?fragmentLookupStart.get(t.getLabel()):(new HashSet<Tree<Integer>>());
	fragmentStartSet.add(inverseFragmentLookup.get(new Integer(val)));
	fragmentLookupStart.put(t.getLabel(),fragmentStartSet);*/
	fragmentFrequency.put(t,new Integer(m.group(1)));
	return true;
    }

    public void readFragmentFromText(String fileName) {
        try {
	    int val=0;
	    fragmentLookup = new IntegerHashtable<Tree<Integer>>();
	    inverseFragmentLookup = new ArrayList<Tree<Integer>>();
	    DataInputStream in = new DataInputStream(new FileInputStream(fileName));
	    BufferedReader br = new BufferedReader(new InputStreamReader(in));
	    String line;
	    Pattern pattern = Pattern.compile("^(\\d+)\\s+(\\(.+\\))$");
	    while ((line = br.readLine()) != null){
		if(parseFragmentFrequency(fragmentLookup,line, pattern, val))
		    val++;
	    }
	    //System.out.println(fragmentLookup);
	    in.close();
        } catch (IOException e) {
            System.out.println("error reading counts: " + e);
        }
    }

    protected void printFragmentFrequency(){
	printFragmentFrequency(true);
    }

    protected void printFragmentFrequency(boolean humanReadable){
	if(humanReadable) {
	    for(Integer key : fragmentFrequency.getInverseMap().keySet()){
		HashSet<Tree<Integer>> set = fragmentFrequency.getInverseMap().get(key);
		for(Tree<Integer> T : set){
		    System.out.println(key + " " + getHumanTree(T));
		}
	    }	
	}
	else
	    System.out.println(fragmentFrequency.toString()+"\n");
    }

    protected String getHumanTree(Tree<Integer> T){
	return Trees.integerTreeToStringTree(T).toTerminalMarkedString();
    }

    /**
     * Return a mapping from trees in {@param otherCorpus} to
     * a feature vector.
     */
    /*protected Map<Tree<Integer>, SparseIntVector> extractFeaturesOn(List<Tree<Integer>> otherCorpus){
	Map<Tree<Integer>, SparseIntVector> map = new HashMap<Tree<Integer>, SparseIntVector>();
	//this part is only for debugging purposes
	for(Tree<Integer> f : fragmentLookup.keySet()){
	    System.out.println(getHumanTree(f) + " ==> " + fragmentLookup.get(f));
	}
	//actually start alg now
	for(Tree<Integer> T : otherCorpus){
	    System.out.println("Tree is :"+ getHumanTree(T));
	    SparseIntVector SV = new SparseIntVector();
	    Queue<Tree<Integer>> expandQ = new LinkedList<Tree<Integer>>();
	    Map<Tree<Integer>, Set<Tree<Integer>>> alreadySeen = new HashMap<Tree<Integer>, Set<Tree<Integer>>>();
	    Tree<Integer> tempFrag =(new Fragment(T)).getTree();
	    Set<Tree<Integer>> alreadySeenSet = new HashSet<Tree<Integer>>();
	    alreadySeenSet.add(T);
	    alreadySeen.put(tempFrag,alreadySeenSet);
	    //alreadySeenSet=null;
	    tempFrag.calculateNumberOfExpansions();
	    expandQ.add(tempFrag);
	    //add children
	    Queue<Tree<Integer>> childQ = new LinkedList<Tree<Integer>>();
	    for(Tree<Integer> child : T.getChildren())
		childQ.add(child);

	    while(!childQ.isEmpty()){
		Tree<Integer> currTree = childQ.poll();
		Tree<Integer> tempFrag = (new Fragment(currTree)).getTree();
		tempFrag.calculateNumberOfExpansions();
		expandQ.add(tempFrag);
		Set<Tree<Integer>> alreadySeenChildSet = new HashSet<Tree<Integer>>();
		alreadySeenChildSet.add(T);
		alreadySeen.put(tempFrag,alreadySeenChildSet);
		for(Tree<Integer> child : currTree.getChildren()){
		    if(!child.isLeaf())
			childQ.add(child);
		}
	    }

	    //loop
	    while(!expandQ.isEmpty()){
		Tree<Integer> X = expandQ.poll();
		//test if X matches
		if(fragmentLookup.containsKey(sub)){
		    System.out.println("\tcontain!");
		    SV.increment(fragmentLookup.get(X));
		}
		//expand X
		List<Tree<Integer>> frontier = Fragment.getFrontierFragments(X,T);
		for(Tree<Integer> expandedX : frontier){
		    if(alreadySeen.contains(expandedX))
			continue;
		    expandQ.add(expandedX);
		    alreadySeen.add(expandedX);
		}
	    }
	    map.put(T,SV);
	}
	System.out.println(map);
	return map;
    }*/

    private boolean reconcileChildren(List<Tree<Integer>> l1, List<Tree<Integer>> l2){
	if(l1==null && l2==null) return true;
	else if((l1==null && l2!= null) ||
		(l1==null && l2!= null)) return false;
	//this assumes that the labels of the given trees have been matched properly!	
	else if(l1.size() == 0) return true;
	else if(l1.size() != l2.size()) return false;
	boolean val = true;
	for(int i = 0; i < l1.size(); i++){
	    //System.out.println(l1.get(i).getLabel() + " --- " + l2.get(i).getLabel());
	    val &= l1.get(i).getLabel().equals(l2.get(i).getLabel());
	    if(!val)
		break;
	}
	return val;
    }


    private boolean matchFragmentToSubtree(Tree<Integer> fragment, Tree<Integer> T){
	//compare (labels) of fragment.getChildren() to T.getChildren()
	//System.out.println("\t\ttrying to match : " + getHumanTree(fragment) +", "+ getHumanTree(T));
	
	if(!fragment.getLabel().equals(T.getLabel()))
	    return false;
	
	boolean keep = true;
	List<Tree<Integer>> fCs = fragment.getChildren();
	List<Tree<Integer>> tCs = T.getChildren();
	if(!reconcileChildren(fCs, tCs)) return false;
	if(fCs == null) return true;
	int size = fCs.size();
	for(int i = 0; i<size; i++){
	    //System.out.println("\t\t\tmatching child: " + getHumanTree(fCs.get(i)) +" :: "+ getHumanTree(tCs.get(i)));
	    keep &= matchFragmentToSubtree(fCs.get(i), tCs.get(i));
	    if(!keep) return false;
	}
	return keep;
    }

    /**
     * Extract count-based features on <code>otherCorpus</code>
     * using the <code>K</code>-most frequent counted subtrees of 
     * {@see AnalyzeSubtrees}. This is guaranteed to maintain order of the "test" corpus.
     */
    protected List<SparseIntVector> extractFeaturesOn(List<Tree<Integer>> otherCorpus){
	//very important to keep corpus ordering
	List<SparseIntVector> lst = new ArrayList<SparseIntVector>();
	/*
	for(Tree<Integer> f : fragmentLookup.keySet()){
	    System.out.println(getHumanTree(f) + " ==> " + fragmentLookup.get(f));
	}
	System.out.println("\n\n");
	*/

	//actually start alg now
	//for every tree in the test-corpus
	for(Tree<Integer> T : otherCorpus){
	    //System.out.println(getHumanTree(T));
	    SparseIntVector SV = new SparseIntVector();
	    //for every node in T
	    Queue<Tree<Integer>> Q = new LinkedList<Tree<Integer>>();
	    Q.add(T);
	    while(!Q.isEmpty()){
		Tree<Integer> node = Q.poll();
		//System.out.println("looking at subtree starting at: " + getHumanTree(node));
		//for every fragment from "training"
		for(Tree<Integer> trainingFragment : fragmentLookup.keySet()){
		    //System.out.println("\tfragment to consider: " + getHumanTree(trainingFragment));
		    if(matchFragmentToSubtree(trainingFragment, node)){
			//System.out.println("\t\tmatch with " + getHumanTree(trainingFragment));
			SV.increment(fragmentLookup.get(trainingFragment));
		    }
		}
		//add children of node to Q
		for(Tree<Integer> child : node.getChildren())
		    Q.add(child);
	    }
	    Q=null;
	    //store feature vector
	    //System.out.println(SV);
	    lst.add(SV);
	}
	return lst;
    }

    public void printExtractedFeatures(List<SparseIntVector> lst){
	for(SparseIntVector SV : lst){
	    for(Integer capturedT : SV.keySet()){
		System.out.print(removeSpaces(getHumanTree(inverseFragmentLookup.get(capturedT)), "_") + ":" + SV.get(capturedT) + " ");
	    }
	
	    System.out.println();
	}
    }

    public void printExtractedFeatures(List<SparseIntVector> lst, String outpath){
	try{
	    BufferedWriter out = new BufferedWriter(new FileWriter(outpath));
	    for(SparseIntVector SV : lst){
		for(Integer capturedT : SV.keySet()){
		    out.write(removeSpaces(getHumanTree(inverseFragmentLookup.get(capturedT)), "_") + ":" + SV.get(capturedT) + " ");
		}
		out.write("\n");
	    }
	    out.close();
	}catch (Exception e){
	    System.err.println("Error writing extracted features:\n\t" + e.getMessage());
	}

	
    }

    private String removeSpaces(String s, String p){
	return s.replaceAll(" ",p);
    }

    public static class Options {
	@Option(name = "-universalTagLocation", usage = "path to universal tag mappings (default null)")
	    public String universalTagLocation = null;
	@Option(name = "-universalTagLanguage", usage = "Which universal tag set to acces; default = null = don't use universal tags; all-to-X = replace *every* symbol by \"X\"")
	    public String languageForUniversalTags=null;
	@Option(name = "-trfr", usage = "The fraction of the training corpus to keep (Default: 1.0)\n")
	    public double trainingFractionToKeep = 1.0;
	@Option(name = "-skipSection", usage = "Skips a particular section of the training corpus (Needed for training Mark Johnsons reranker")
	    public int skipSection = -1;

        @Option(name = "-debug", required = false, usage = "Produce extra debugging output (default: false)")
	    public int debug = 0;

        @Option(name = "-seed", required = false, usage = "Seed of the random number generator (default: 0)")
	    public long seed = 0;

	@Option(name = "-extractcounts", required = false, usage = "Extract counts from the \"training\" corpus (default 1 (==true))")
	    public int extractcounts = 1;
        @Option(name = "-corpus", required = false, usage = "Location of training corpus hierarchy (required when extract_counts=1, default=null)")
	    public String corpus = "/export/common/data/corpora/LDC/LDC99T42/treebank_3/parsed/mrg/wsj/";
        @Option(name = "-first", required = false, usage = "First section of training data (default 0200)")
	    public int first = 200;
        @Option(name = "-last", required = false, usage = "Last section of training data (default 2199)")
	    public int last = 2199;

	@Option(name = "-testcorpus", required = false, usage = "Location of \"test\" corpus (default null-string)")
	    public String testcorpus = null;
        @Option(name = "-testfirst", required = false, usage = "First section of \"test\" data (default -1; read entire directory/file)")
	    public int testfirst = 200;
        @Option(name = "-testlast", required = false, usage = "Last section of \"test\" data (default -1; read entire directory/file)")
	    public int testlast = 2199;


	@Option(name = "-cutoff", required = false, usage = "Upperbound on number of fragments to return (default 50000)")
	    public int cutoff = 50000;
	@Option(name = "-rulecriterion", required = false, usage = "Upperbound on number of rules a counted fragment may have (default 10)")
	    public int rulecriterion = 10;
	@Option(name = "-asConstraintSet", required = false, usage = "Return the final result as a constraint set (default false)")
	    public boolean asConstraintSet = false;
	@Option(name = "-incrementalprint", required = false, usage = "Print incrementally (default false)")
	    public boolean printincrementally = false;
	@Option(name = "-analyzeleftover", required = false, usage = "Perform an analysis on what remains after each reduce (default false)")
	    public boolean performLeftOverAnalysis = false;
	@Option(name = "-alldepthone", required = false, usage = "Keep all depth-1 fragments, regardless of frequency and cutoff (default 0=false)")
	    public int alldepthone = 0;
	@Option(name = "-fragmenttogz", required = false, usage = "Path (directory) to print gzip of fragment look-up (default empty, don't print)")
	    public String fragmenttogz = "";
	@Option(name = "-gztofragment", required = false, usage = "Path (file) to access gzip of fragment look-up (default null-string, don't read in)")
	    public String gztofragment = null;
	@Option(name = "-texttofragment", required = false, usage = "Path (file) to access text of fragment look-up (default null-string, don't read in)")
	    public String texttofragment = null;

	@Option(name = "-outputtextfeatures", required = false, usage = "Path (file) to print (plain text) extracted features (default null-string)")
	    public String outputtextfeatures = null;

	@Option(name = "-binarize", required = false, usage = "Binarize the \"training\" grammar prior to analysis (default false)")
	    public boolean binarize = false;
	@Option(name = "-b", usage = "LEFT/RIGHT Binarization (Default: RIGHT)")
	    public Binarization binarization = Binarization.RIGHT;

	@Option(name = "-treebank", usage = "Language:  WSJ (ENG), CHNINESE, GERMAN, CONLL, SINGLEFILE, KOREAN (Default: ENGLISH)")
	    public TreeBankType treebank = TreeBankType.WSJ;

	@Option(name = "-hor", usage = "Horizontal Markovization (Default: 0)")
	    public int horizontalMarkovization = 0;
	@Option(name = "-ver", usage = "Vertical Markovization (Default: 1)")
	    public int verticalMarkovization = 1;


	//
	/*
	  @Option(name = "-alpha", required = false, usage = "Initial value of alpha hyperparameter (default 1)")
	  public double alpha = 1.0;
	  @Option(name = "-stop", required = false, usage = "Initial value of stop hyperparameter (default 0.9)")
	  public double stop = 0.9;
	  @Option(name = "-fix", required = false, usage = "Do not resample hyperparameters (default false)")
	  public boolean fix = false;
	  @Option(name = "-fix-alpha", required = false, usage = "Do not resample alpha (default false)")
	  public boolean fixAlpha = false;
	  @Option(name = "-fix-stop", required = false, usage = "Do not resample the stop hyperparamter (default false)")
	  public boolean fixStop = false;
	  @Option(name = "-iterations", required = false, usage = "Number of iterations (default 1000)")
	  public int numIterations = 1000;
	  @Option(name = "-dump", required = false, usage = "Dump frequency (default 10)")
	  public int dump = 10;
	  //@Option(name = "-derivation", required = false, usage = "Initial derivation of trees (depthone,spinal,full) (default spinal)")
	  //public String initialDerivation = "spinal";
	  @Option(name = "-hyper", required = false, usage = "Frequency with which to resample hyper-parameters (default 10)")
	  public int hyperSampleRate = 10;
	  @Option(name = "-mean", required = false, usage = "Proposal distribution mean is of log-normal (default: false)")
	  public boolean mean = false;
	  @Option(name = "-continue", required = false, usage = "Continue sampling from last dump")
	  public boolean continueSampling = false;

	  @Option(name = "-decay", required = false, usage = "Decay rate (default 1.0)")
	  public double decayRate = 1.0;
	  @Option(name = "-delete", required = false, usage = "Number of fragments to delete at each step (default 1)")
	  public double numFragmentsToDelete = 1;
	  @Option(name = "-tree-samples", required = false, usage = "Number of times to sample the nodes in each tree (default 1)")
	  public int numSamplesPerTree = 1;
	*/
    }

}
