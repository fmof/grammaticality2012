package edu.jhu.coe.PCFGLA;

import edu.jhu.coe.io.PennTreebankReader;
import edu.jhu.coe.PCFGLA.ConstraintSet;
import edu.jhu.coe.PCFGLA.LatentStatistics;
import edu.jhu.coe.PCFGLA.OptionParser;
import edu.jhu.coe.PCFGLA.Option;
import edu.jhu.coe.PCFGLA.Corpus.TreeBankType;
import edu.jhu.coe.PCFGLA.smoothing.NoSmoothing;
import edu.jhu.coe.PCFGLA.smoothing.SmoothAcrossParentBits;
import edu.jhu.coe.PCFGLA.smoothing.SmoothAcrossParentSubstate;
import edu.jhu.coe.PCFGLA.smoothing.Smoother;
import edu.jhu.coe.syntax.BerkeleyCompatibleFragment;
import edu.jhu.coe.syntax.StateSet;
import edu.jhu.coe.syntax.SubtreeAnalyzerByRule;
import edu.jhu.coe.syntax.Tree;
import edu.jhu.coe.syntax.Trees;
import edu.jhu.coe.util.Numberer;
import edu.jhu.coe.util.CommandLineUtils;
import edu.jhu.coe.util.UniversalTagMapper;

import fig.basic.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Reads in the Penn Treebank and generates N_GRAMMARS different grammars.
 *
 * @author Slav Petrov
 * @author Frank Ferraro
 */
public class GrammarTrainer {


    public static boolean VERBOSE = false; 
    public static int HORIZONTAL_MARKOVIZATION = 1; 
    public static int VERTICAL_MARKOVIZATION = 2;
    public static Random RANDOM = new Random(0);
  
    public static class Options {

	@Option(name = "-out", required = true, usage = "Output File for Grammar (Required)")
	    public String outFileName;
		
	@Option(name = "-path", usage = "Path to Corpus (Default: null)")
	    public String path = null;
	@Option(name = "-train", usage = "List of training files (Default: null)")
	    public String trainList = null;
        @Option(name = "-validation", usage = "List of valiation files (Default: null)")
	    public String validationList = null;
        
	@Option(name = "-SMcycles", usage = "The number of split&merge iterations (Default: 6)")
	    public int numSplits = 6;

	@Option(name = "-mergingPercentage", usage = "Merging percentage (Default: 0.5)")
	    public double mergingPercentage = 0.5;

	@Option(name = "-baseline", usage = "Just read of the MLE baseline grammar")
	    public boolean baseline = false;

	@Option(name = "-treebank", usage = "Language:  WSJ (ENG), CHNINESE, GERMAN, CONLL, SINGLEFILE, KOREAN (Default: ENGLISH)")
	    public TreeBankType treebank = TreeBankType.WSJ;

	@Option(name = "-splitMaxIt", usage = "Maximum number of EM iterations after splitting (Default: 50)")
	    public int splitMaxIterations = 50;

	@Option(name = "-splitMinIt", usage = "Minimum number of EM iterations after splitting (Default: 50)")
	    public int splitMinIterations = 50;

	@Option(name = "-mergeMaxIt", usage = "Maximum number of EM iterations after merging (Default: 20)")
	    public int mergeMaxIterations = 20;

	@Option(name = "-mergeMinIt", usage = "Minimum number of EM iterations after merging (Default: 20)")
	    public int mergeMinIterations = 20;

	@Option(name = "-di", usage = "The number of allowed iterations in which the validation likelihood drops. (Default: 6)")
	    public int di = 6;

	@Option(name = "-trfr", usage = "The fraction of the training corpus to keep (Default: 1.0)\n")
	    public double trainingFractionToKeep = 1.0;

	@Option(name = "-filter", usage = "Filter rules with prob below this threshold (Default: 1.0e-30)")
	    public double filter = 1.0e-30;

	@Option(name = "-smooth", usage = "Type of grammar smoothing used.")
	    public String smooth = "SmoothAcrossParentBits";
		
	@Option(name = "-maxL", usage = "Maximum sentence length (Default <=10000)")
	    public int maxSentenceLength = 10000;

	@Option(name = "-b", usage = "LEFT/RIGHT Binarization (Default: RIGHT)")
	    public Binarization binarization = Binarization.RIGHT;

	@Option(name = "-noSplit", usage = "Don't split - just load and continue training an existing grammar (true/false) (Default:false)")
	    public boolean noSplit = false;

	@Option(name = "-in", usage = "Input File for Grammar")
	    public String inFile = null;

	@Option(name = "-randSeed", usage = "Seed for random number generator")
	    public int randSeed = 8;

	@Option(name = "-sep", usage = "Set merging threshold for grammar and lexicon separately (Default: false)")
	    public boolean separateMergingThreshold = false;

	@Option(name = "-trainOnDevSet", usage = "Include the development set into the training set (Default: false)")
	    public boolean trainOnDevSet = false;

	@Option(name = "-hor", usage = "Horizontal Markovization (Default: 0)")
	    public int horizontalMarkovization = 0;

	@Option(name = "-sub", usage = "Number of substates to split (Default: 2)")
	    public short nSubStates = 1;

	@Option(name = "-ver", usage = "Vertical Markovization (Default: 1)")
	    public int verticalMarkovization = 1;

	@Option(name = "-v", usage = "Verbose/Quiet (Default: Quiet)\n")
	    public boolean verbose = false;

	@Option(name = "-lowercase", usage = "Lowercase all words in the treebank")
	    public boolean lowercase = false;

	@Option(name = "-r", usage = "Level of Randomness at init (Default: 1)\n")
	    public double randomization = 1.0;

	@Option(name = "-sm1", usage = "Lexicon smoothing parameter 1")
	    public double smoothingParameter1 = 0.5;

	@Option(name = "-sm2", usage = "Lexicon smoothing parameter 2)")
	    public double smoothingParameter2 = 0.1;

	@Option(name = "-spath", usage = "Whether or not to store the best path info (true/false) (Default: true)")
	    public boolean findClosedUnaryPaths = true;

	@Option(name = "-simpleLexicon", usage = "Use the simple generative lexicon")
	    public boolean simpleLexicon = false;

	@Option(name = "-skipSection", usage = "Skips a particular section of the training corpus (Needed for training Mark Johnsons reranker")
	    public int skipSection = -1;
	@Option(name = "-universalTagLocation", usage = "path to universal tag mappings")
	    public String universalTagLocation = "/home/hltcoe/fferraro/code/universal_pos_tags.1.02/";
	@Option(name = "-universalTagLanguage", usage = "Whether to use universal tag set; default = null = don't use universal tags")
	    public String languageForUniversalTags=null;
	@Option(name = "-useConstraintSet", usage="Whether or not to use a constraint set (if false, then allow all couplings) (default: true)")
	    public boolean useConstraintSet = true;
	@Option(name = "-constraints", usage = "Specify the constraint set (default: null [=<allow no couplings>, unless \"-reobtainConstraints\" is set to true])")
	    public String constraintPath = null;
	@Option(name = "-reobtainConstraints", usage = "Whether or not to reobtain the constraint set with respect to the latest state-split treebank (default: false)")
	    public boolean reobtainConstraints = false;
	@Option(name = "-cutoff", required = false, usage = "Upperbound on number of fragments to return (default 50000)")
	    public int cutoff = 50000;
	@Option(name = "-increasecutoff", required=false, usage="Whether or not to increase the k-most frequent subtree list (default false)")
	    public boolean increasecutoff = false;
	@Option(name = "-rulecriterion", required = false, usage = "Upperbound on number of rules a counted fragment may have (default 10)")
	    public int rulecriterion = 10;
	@Option(name = "-maxrulecriterion", required=false, usage= "Upperbound on rule-depth for subtree count (default -1 = no maximum)")
	    public int maxrulecriterion = -1;
	@Option(name = "-nocouple", usage = "Whether or not to couple/decouple (default false: we couple)")
	    public boolean nocouple = false;
	@Option(name = "-gamma", usage = "Gamma for coupling (TSG confidence) (default: 0.5)")
	    public double gamma = 0.5;

	//		@Option(name = "-grsm", usage = "Grammar smoothing parameter, in range [0,1].  (Default: 0.1)\n")
	//		public double grammarSmoothingParameter = 0.1;

	//	@Option(name = "-a", usage = "annotate (Default: true)\n")
	//	public boolean annotate = true;

    }


  
    public static void main(String[] args) {
	OptionParser optParser = new OptionParser(Options.class);
	Options opts = (Options) optParser.parse(args, true);
	// provide feedback on command-line arguments
	System.out.println("Calling with " + optParser.getPassedInOptions());

	if(opts.path!=null && (opts.trainList !=null || opts.validationList != null)){
	    System.err.println("Please specify either a single path or two separate training/validation paths (but not both!)");
	    System.exit(2);
	}
	if(opts.trainList ==null ^ opts.validationList == null){
	    System.err.println("Please specify both a training path and a validation path");
	    System.exit(2);
	}


	double trainingFractionToKeep = opts.trainingFractionToKeep;    
	int maxSentenceLength = opts.maxSentenceLength;    
	HORIZONTAL_MARKOVIZATION = opts.horizontalMarkovization;
	VERTICAL_MARKOVIZATION = opts.verticalMarkovization;    
	Binarization binarization = opts.binarization; 
	double randomness = opts.randomization;    
	String outFileName = opts.outFileName;
	if (outFileName==null) {
	    System.out.println("Output File name is required.");
	    System.exit(-1);
	}
	else System.out.println("Using grammar output file "+outFileName+".");
	VERBOSE = opts.verbose;
	RANDOM = new Random(opts.randSeed);

	boolean manualAnnotation = false;
	boolean baseline = opts.baseline;
	boolean noSplit = opts.noSplit;
	int numSplitTimes = opts.numSplits;
	if (baseline) numSplitTimes = 0;
	String splitGrammarFile = opts.inFile;
	int allowedDroppingIters = opts.di;

	int maxIterations = opts.splitMaxIterations;
	int minIterations = opts.splitMinIterations;

	double[] smoothParams = {opts.smoothingParameter1,opts.smoothingParameter2};
   
	boolean allowMoreSubstatesThanCounts = false;
	boolean findClosedUnaryPaths = opts.findClosedUnaryPaths;

	String path;
        String trainList;
        String validationList;

	Corpus corpus;
	boolean applyUniversalMapping = opts.languageForUniversalTags!=null;
	
	if(applyUniversalMapping){
	    UniversalTagMapper.load(opts.universalTagLocation, opts.languageForUniversalTags);
	}

	UniversalTagMapper.print();
	
	if(opts.path!=null){
	    path = opts.path;
	    System.out.println("Loading trees from "+path+" and using language "+opts.treebank);

	    corpus = new Corpus(path,opts.treebank,trainingFractionToKeep,false, opts.skipSection, applyUniversalMapping);
	} else{        
	    trainList = opts.trainList;
	    validationList = opts.validationList;
	    System.out.println("Loading training trees from " + trainList + " and validation trees from " + validationList);
	    corpus = new Corpus(trainList, validationList, trainingFractionToKeep, applyUniversalMapping);
	}
	
	System.out.println("Will remove sentences with more than "+maxSentenceLength+" words.");
	System.out.println("Using horizontal="+HORIZONTAL_MARKOVIZATION+" and vertical="+VERTICAL_MARKOVIZATION+" markovization.");
	System.out.println("Using "+ binarization.name() + " binarization.");// and "+annotateString+".");
	System.out.println("Using a randomness value of "+randomness);
    
	System.out.println("Random number generator seeded at "+opts.randSeed+".");
	if (minIterations>0)
	    System.out.println("I will do at least "+minIterations+" iterations.");
	System.out.println("Using smoothing parameters "+smoothParams[0]+" and "+smoothParams[1]);
    

	List<Tree<String>> trainTrees = Corpus.binarizeAndFilterTrees(corpus.getTrainTrees(), 
								      VERTICAL_MARKOVIZATION,
								      HORIZONTAL_MARKOVIZATION, 
								      maxSentenceLength, 
								      binarization, 
								      manualAnnotation,
								      VERBOSE);
	List<Tree<String>> validationTrees = Corpus.binarizeAndFilterTrees(corpus.getValidationTrees(), VERTICAL_MARKOVIZATION, HORIZONTAL_MARKOVIZATION, maxSentenceLength, binarization, manualAnnotation,VERBOSE);
	Numberer tagNumberer =  Numberer.getGlobalNumberer("tags");

	//System.out.println(trainTrees);

	if (opts.trainOnDevSet){
	    System.out.println("Adding devSet to training data.");
	    trainTrees.addAll(validationTrees);
	}
    
	if (opts.lowercase){
	    System.out.println("Lowercasing the treebank.");
	    Corpus.lowercaseWords(trainTrees);
	    Corpus.lowercaseWords(validationTrees);
	}
    
	int nTrees = trainTrees.size();

	System.out.println("There are "+nTrees+" trees in the training set.");
	System.out.println(trainTrees);
	
	double filter = opts.filter;
	if(filter>0) System.out.println("Will remove rules with prob under "+filter+
					".\nEven though only unlikely rules are pruned the training LL is not guaranteed to increase in every round anymore " +
					"(especially when we are close to converging)." +
					"\nFurthermore it increases the variance because 'good' rules can be pruned away in early stages.");

	short nSubstates = opts.nSubStates;
	short[] numSubStatesArray = initializeSubStateArray(trainTrees, validationTrees, tagNumberer, nSubstates);
	if (baseline) {
	    short one = 1;
	    Arrays.fill(numSubStatesArray, one);
	    System.out.println("Training just the baseline grammar (1 substate for all states)");
	    randomness = 0.0f;
	}
    
	if (VERBOSE){
	    for (int i=0; i<numSubStatesArray.length; i++){
	    	System.out.println("Tag "+(String)tagNumberer.object(i)+" "+i);
	    }
	}
    

	//initialize lexicon and grammar
	Lexicon lexicon = null, maxLexicon = null, previousLexicon = null;
	Grammar grammar = null, maxGrammar = null, previousGrammar = null;
	double maxLikelihood = Double.NEGATIVE_INFINITY;

	// EM: iterate until the validation likelihood drops for four consecutive
	// iterations
	int iter = 0;
	int droppingIter = 0;
    
	//  If we are splitting, we load the old grammar and start off by splitting.
	int startSplit = 0;
	if (splitGrammarFile!=null) {
	    System.out.println("Loading old grammar from "+splitGrammarFile);
	    startSplit = 1; // we've already trained the grammar
	    ParserData pData = ParserData.Load(splitGrammarFile);
	    maxGrammar = pData.gr;
	    maxLexicon = pData.lex;
	    numSubStatesArray = maxGrammar.numSubStates;
	    previousGrammar = grammar = maxGrammar;
	    previousLexicon = lexicon = maxLexicon;
	    Numberer.setNumberers(pData.getNumbs());
	    tagNumberer =  Numberer.getGlobalNumberer("tags");
	    System.out.println("Loading old grammar complete.");
	    if (noSplit){
		System.out.println("Will NOT split the loaded grammar.");
		startSplit=0;
	    }
	} 
    
	double mergingPercentage = opts.mergingPercentage;
  	boolean separateMergingThreshold = opts.separateMergingThreshold;
	if (mergingPercentage>0){
	    System.out.println("Will merge "+(int)(mergingPercentage*100)+"% of the splits in each round.");
	    System.out.println("The threshold for merging lexical and phrasal categories will be set separately: "+separateMergingThreshold);
	}
    
	StateSetTreeList trainStateSetTrees = new StateSetTreeList(trainTrees, numSubStatesArray, false, tagNumberer);
	StateSetTreeList validationStateSetTrees = new StateSetTreeList(validationTrees, numSubStatesArray, false, tagNumberer);//deletePC);
    
	// get rid of the old trees
	trainTrees = null;
	validationTrees = null;
	corpus = null;
	System.gc();
    
	if (opts.simpleLexicon){
	    System.out.println("Replacing words which have been seen less than 5 times with their signature.");
	    Corpus.replaceRareWords(trainStateSetTrees,new SimpleLexicon(numSubStatesArray,-1), Math.abs(5));
	}


    
	// If we're training without loading a split grammar, then we run once without splitting.
	if (splitGrammarFile==null) {
	    grammar = new Grammar(numSubStatesArray, findClosedUnaryPaths, new NoSmoothing(), null, filter);
	    lexicon = (opts.simpleLexicon) ? 
		new SimpleLexicon(numSubStatesArray,-1,smoothParams, new NoSmoothing(),filter, trainStateSetTrees) : 
		new SophisticatedLexicon(numSubStatesArray,SophisticatedLexicon.DEFAULT_SMOOTHING_CUTOFF,smoothParams, new NoSmoothing(),filter);
	    int n = 0;
	    boolean secondHalf = false;
	    for (Tree<StateSet> stateSetTree : trainStateSetTrees) {
		secondHalf = (n++>nTrees/2.0); 
		lexicon.trainTree(stateSetTree, randomness, null, secondHalf,false);
		grammar.tallyUninitializedStateSetTree(stateSetTree);
	    }
	    lexicon.optimize();
	    grammar.optimize(randomness);
	    //System.out.println(grammar);
	    previousGrammar = maxGrammar = grammar; //needed for baseline - when there is no EM loop
	    previousLexicon = maxLexicon = lexicon;
	}

	//read in the constraint set here
	System.out.println(Numberer.getGlobalNumberer("tags"));
	System.out.println(Numberer.getGlobalNumberer("words"));

	boolean doCouplingDecoupling = !opts.nocouple;
	ConstraintSet constraintSet = opts.useConstraintSet ? ((opts.constraintPath==null)?(new ConstraintSet()):readConstraintSet(opts.constraintPath)) : (new ConstraintSet((boolean)true));

	short[] numNonInternal = new short[previousGrammar.numSubStates.length];
	if(splitGrammarFile!=null)
	    throw new Error("Whoops! I haven't gotten around to filling numNonInternal[] array with a provided grammar file! Sorry!");
	for(int i=0;i<numNonInternal.length;i++){
	    numNonInternal[i] = previousGrammar.numSubStates[i];
	}
	InternalNodeSet.resetInternalForTagArray(previousGrammar.numSubStates.length);
	
	// the main loop: split and train the grammar
	int loopFactor = doCouplingDecoupling ? 5 : 3;
	for (int splitIndex = startSplit; splitIndex < numSplitTimes*loopFactor; splitIndex++) {
	    //split-merge-couple-decouple
	    String opString = "";
	    if (splitIndex % loopFactor==2){//(splitIndex==numSplitTimes*2){
    		if (opts.smooth.equals("NoSmoothing")) continue;
    		System.out.println("Setting smoother for grammar and lexicon.");
		Smoother grSmoother = new SmoothAcrossParentBits(0.01,maxGrammar.splitTrees);
		Smoother lexSmoother = new SmoothAcrossParentBits(0.1,maxGrammar.splitTrees);
		maxGrammar.setSmoother(grSmoother);
		maxLexicon.setSmoother(lexSmoother);
		minIterations = maxIterations = 10;
		opString = "smoothing";
	    }
	    else if (splitIndex % loopFactor==0) {
    		// the case where we split
    		if (opts.noSplit) continue;
    		System.out.println("Before splitting, we have a total of "+maxGrammar.totalSubStates()+" substates.");
    		CorpusStatistics corpusStatistics = new CorpusStatistics(tagNumberer,trainStateSetTrees);
		int[] counts = corpusStatistics.getSymbolCounts();
		//System.out.println("Before splitting, maxGrammar is \n" + maxGrammar+"\n and maxLexicon is\n"+maxLexicon);

		for(int i = 0; i < maxGrammar.numSubStates.length; i++){
		    numNonInternal[i] = (short)(Math.abs((maxGrammar.numSubStates[i] - InternalNodeSet.getNumberOfInternalsForTag(i))));
		    //System.out.println("numNonInternal["+i+"]="+numNonInternal[i]+"; maxGrammar.numSubStates[i]="+maxGrammar.numSubStates[i]+"; InternalNodeSet.getNumberOfInternalsForTag(i)="+InternalNodeSet.getNumberOfInternalsForTag(i));
		}

		int[] offsets = new int[maxGrammar.numSubStates.length];
    		maxGrammar = maxGrammar.splitAllStates(numNonInternal, offsets, randomness, counts, allowMoreSubstatesThanCounts, 0);
    		maxLexicon = maxLexicon.splitAllStates(numNonInternal, offsets, counts, allowMoreSubstatesThanCounts, 0);
		InternalNodeSet.updateMappings(offsets);
		//System.err.println(((SophisticatedLexicon)maxLexicon).ptSeen);
		if(!opts.simpleLexicon)
		    ((SophisticatedLexicon)maxLexicon).ptSeen = InternalNodeSet.update(((SophisticatedLexicon)maxLexicon).ptSeen, offsets);
		//System.err.println(((SophisticatedLexicon)maxLexicon).ptSeen);
		//System.out.println("After splitting, maxGrammar is \n" + maxGrammar);//+"\n and maxLexicon is\n"+maxLexicon);
		Smoother grSmoother = new NoSmoothing();
		Smoother lexSmoother = new NoSmoothing();
		maxGrammar.setSmoother(grSmoother);
		maxLexicon.setSmoother(lexSmoother);
    		System.out.println("After splitting, we have a total of "+maxGrammar.totalSubStates()+" substates.");
    		System.out.println("Rule probabilities are NOT normalized in the split, therefore the training LL is not guaranteed to improve between iteration 0 and 1!");
    		opString = "splitting";
		maxIterations = opts.splitMaxIterations;
		minIterations = opts.splitMinIterations;
	    }
	    else if(splitIndex % loopFactor == 1) {
    		if (mergingPercentage==0) continue;
    		// the case where we merge
    		double[][] mergeWeights = GrammarMerger.computeMergeWeights(maxGrammar, maxLexicon,trainStateSetTrees);
    		double[][][] deltas = GrammarMerger.computeDeltas(maxGrammar, maxLexicon, mergeWeights, trainStateSetTrees);
    		boolean[][][] mergeThesePairs = GrammarMerger.determineMergePairs(deltas,separateMergingThreshold,mergingPercentage,maxGrammar);
    		
    		grammar = GrammarMerger.doTheMerges(maxGrammar, maxLexicon, mergeThesePairs, mergeWeights);
    		short[] newNumSubStatesArray = grammar.numSubStates;
		trainStateSetTrees = new StateSetTreeList(trainStateSetTrees, newNumSubStatesArray, false);
		validationStateSetTrees = new StateSetTreeList(validationStateSetTrees, newNumSubStatesArray, false);
    		// retrain lexicon to finish the lexicon merge (updates the unknown words model)...
    		lexicon = (opts.simpleLexicon) ? 
		    new SimpleLexicon(newNumSubStatesArray,-1,smoothParams, maxLexicon.getSmoother() ,filter, trainStateSetTrees) :
		    new SophisticatedLexicon(newNumSubStatesArray,SophisticatedLexicon.DEFAULT_SMOOTHING_CUTOFF, maxLexicon.getSmoothingParams(), maxLexicon.getSmoother(), maxLexicon.getPruningThreshold());
		if(!opts.simpleLexicon)
		    ((SophisticatedLexicon)lexicon).ptSeen = ((SophisticatedLexicon)maxLexicon).ptSeen;
		
    		boolean updateOnlyLexicon = true;
    		double trainingLikelihood = GrammarTrainer.doOneEStep(grammar, maxLexicon, null, lexicon, trainStateSetTrees, updateOnlyLexicon);
		
    		lexicon.optimize(); // M Step    		

    		GrammarMerger.printMergingStatistics(maxGrammar, grammar);
    		opString = "merging";
    		maxGrammar = grammar; maxLexicon = lexicon;
    		maxIterations = opts.mergeMaxIterations;
    		minIterations = opts.mergeMinIterations;
	    } else if(doCouplingDecoupling && splitIndex % loopFactor == 3){
		//coupling                
                System.out.println("Some debugging info.");//Before coupling, we have a total of " + maxGrammar.totalSubStates() + " substates.");
		InternalNodeSet.resetJustAdded();
                CorpusStatistics corpusStatistics = new CorpusStatistics(tagNumberer, trainStateSetTrees);
                int[] counts = corpusStatistics.getSymbolCounts();

		ArrayParser parser = new ArrayParser(maxGrammar, maxLexicon);
		//0: Reobtain the constraint set, if needed
		if(opts.reobtainConstraints){
		    System.out.println("tag num is \n"+ tagNumberer);
		    List<Tree<String>> trees = new LinkedList<Tree<String>>();
		    for (Tree<StateSet> stateSetTree : trainStateSetTrees) {
			Tree<String> st = parser.getBestViterbiDerivation(stateSetTree.shallowCloneAndOrder(), false, true);
			System.out.println("THE TREE IS ="+st);
			trees.add(st);
		    }
		    System.out.println("tag num is \n"+ tagNumberer);
		    OptionParser subOptParser = new OptionParser(edu.jhu.coe.syntax.SubtreeAnalyzer.Options.class);

		    int cutc = (opts.increasecutoff)?(int)(opts.cutoff*Math.pow(2,(int)(splitIndex/loopFactor))):opts.cutoff;
		    int rulec = (opts.rulecriterion==-1)?(int)Math.max(Math.pow(2,((int)(splitIndex/loopFactor)+1)), opts.maxrulecriterion):opts.rulecriterion;
		    String[] argsToPass = {"-cutoff",Integer.toString(cutc),
					   "-rulecriterion", Integer.toString(rulec),
					   //"-treebank", opts.treebank,
					   "-asConstraintSet", "-binarize"};
		    System.out.print("args to pass to SubtreeAnalyzerByRule:\n\t");
		    System.err.print("args to pass to SubtreeAnalyzerByRule:\n\t");
		    for(int i = 0;i<argsToPass.length;i++){
			    System.err.print(argsToPass[i]+" ");
			    System.out.print(argsToPass[i]+" ");
		    }
		    System.err.println();
		    System.out.println();
		    SubtreeAnalyzerByRule SABR = new SubtreeAnalyzerByRule((edu.jhu.coe.syntax.SubtreeAnalyzer.Options) subOptParser.parse(argsToPass, true));
		    SABR.setCorpus(trees);
		    Collection<Tree<String>> mostFrequentList = SABR.AnalyzeSubtreesAsConstraints(rulec);
		    //System.out.println("what is mostFrequentList? "+ mostFrequentList);
		    constraintSet = new ConstraintSet(opts.constraintPath, mostFrequentList);
		    //System.out.println("tag num is \n"+ tagNumberer);
		    SABR=null;
		}
		System.out.println("What is the constraint set? \n");
		constraintSet.printMap();
		//1: Get updated fragment counts
		LatentStatistics parsedCorpusStatistics = new LatentStatistics(constraintSet);
		if(opts.reobtainConstraints)
		    parsedCorpusStatistics.setUseOfSubstates(true);
		List<Tree<String>> parsedStringTraining = new LinkedList<Tree<String>>();
		for (Tree<StateSet> stateSetTree : trainStateSetTrees) {
		    parsedCorpusStatistics.getBestViterbiDerivation(stateSetTree.shallowCloneAndOrder(), parser, false);
		}
		
		//System.out.println("tag num is \n"+ tagNumberer);
		//System.out.println("latent stats: \n" + parsedCorpusStatistics);

		//2: Hypothesize the couplings in the grammar
		short[] numSigsPerTag=new short[maxGrammar.numSubStates.length];
		
		Set<BerkeleyCompatibleFragment> easyPreterminalCouplingMap = new HashSet<BerkeleyCompatibleFragment>();
		//Map<Pair<Integer,Integer>, BerkeleyCompatibleFragment> easyPreterminalCouplingMap = new HashMap<Pair<Integer,Integer>, BerkeleyCompatibleFragment>();
                maxGrammar = maxGrammar.hypothesizeCouplings(constraintSet, parsedCorpusStatistics, easyPreterminalCouplingMap, numSigsPerTag, opts.gamma, 0.0, 0);
		maxLexicon = maxLexicon.propagateHypothesizedCouplings(parsedCorpusStatistics, easyPreterminalCouplingMap, numSigsPerTag, opts.gamma);

		//3: Set smoother to be no smoothing
		Smoother grSmoother = new NoSmoothing();
		Smoother lexSmoother = new NoSmoothing();
		maxGrammar.setSmoother(grSmoother);
		maxLexicon.setSmoother(lexSmoother);

		//System.out.println("THE NEW GRAMMAR!!\n" + maxGrammar);
		//System.out.println(maxLexicon);

                opString = "coupling";

                maxIterations = opts.splitMaxIterations;
                minIterations = opts.splitMinIterations;
		
		constraintSet=null;
		parsedCorpusStatistics = null;		
	    } else if(doCouplingDecoupling && splitIndex % loopFactor == 4){
		//decoupling
                opString = "decoupling";
	    } else {
		//there's some error....
	    }
	    // update the substate dependent objects
	    previousGrammar = grammar = maxGrammar;
	    previousLexicon = lexicon = maxLexicon;
	    if(opString.equals("decoupling"))
		continue;
	    droppingIter = 0;
	    numSubStatesArray = grammar.numSubStates;
	    trainStateSetTrees = new StateSetTreeList(trainStateSetTrees, numSubStatesArray, false);
	    validationStateSetTrees = new StateSetTreeList(validationStateSetTrees, numSubStatesArray, false);
	    maxLikelihood = calculateLogLikelihood(maxGrammar, maxLexicon, validationStateSetTrees);
	    System.out.println("After "+opString+" in the " + (splitIndex/loopFactor+1) + "th round, we get a validation likelihood of " + maxLikelihood);
	    iter = 0;
  		
	    boolean printEMLL=true;
	    //the inner loop: train the grammar via EM until validation likelihood reliably drops
	    do {
    		iter += 1;
		if(printEMLL)
		    System.out.println("Beginning iteration "+(iter-1)+":");

		// 1) Compute the validation likelihood of the previous iteration
		if(printEMLL)
		    System.out.print("Calculating validation likelihood...");
		double validationLikelihood = calculateLogLikelihood(previousGrammar, previousLexicon, validationStateSetTrees);  // The validation LL of previousGrammar/previousLexicon
		if(printEMLL)
		    System.out.println("done: "+validationLikelihood);
  			
		// 2) Perform the E step while computing the training likelihood of the previous iteration
		if(printEMLL)
		    System.out.print("Calculating training likelihood...");

		grammar = new Grammar(grammar.numSubStates, grammar.findClosedPaths, grammar.smoother, grammar, grammar.threshold);
		lexicon = (opts.simpleLexicon) ? 
		    new SimpleLexicon(grammar.numSubStates,-1,smoothParams, lexicon.getSmoother() ,filter, trainStateSetTrees) :
		    new SophisticatedLexicon(grammar.numSubStates, SophisticatedLexicon.DEFAULT_SMOOTHING_CUTOFF, lexicon.getSmoothingParams(), lexicon.getSmoother(), lexicon.getPruningThreshold());
		if(!opts.simpleLexicon)
		    ((SophisticatedLexicon)lexicon).ptSeen = ((SophisticatedLexicon)maxLexicon).ptSeen;
		boolean updateOnlyLexicon = false;
		double trainingLikelihood = doOneEStep(previousGrammar,previousLexicon,grammar,lexicon,trainStateSetTrees,updateOnlyLexicon);  // The training LL of previousGrammar/previousLexicon
		if(printEMLL)
		    System.out.println("done: "+trainingLikelihood);

	   
		// 3) Perform the M-Step
		lexicon.optimize(); // M Step   
		grammar.optimize(0); // M Step
		
		//System.out.println("After optimizing, grammar is : \n" + grammar);
		//System.out.println("let's see what happens here...\n"+lexicon);
  			
		// 4) Check whether previousGrammar/previousLexicon was in fact better than the best
		if(iter<minIterations || validationLikelihood >= maxLikelihood) {
		    maxLikelihood = validationLikelihood;
		    maxGrammar = previousGrammar;
		    maxLexicon = previousLexicon;
		    droppingIter = 0;
		} else { droppingIter++; }

		// 5) advance the 'pointers'
    		previousGrammar = grammar;
     		previousLexicon = lexicon;
	    } while ((droppingIter < allowedDroppingIters) && (!baseline) && (iter<maxIterations));
			
	    // Dump a grammar file to disk from time to time
	    ParserData pData = new ParserData(maxLexicon, maxGrammar, null, Numberer.getNumberers(), numSubStatesArray, VERTICAL_MARKOVIZATION, HORIZONTAL_MARKOVIZATION, binarization);
	    String outTextTmpName = outFileName + "_"+ (splitIndex/loopFactor+1)+"_"+opString;//+".all.txtgr";
	    String outTmpName = outFileName + "_"+ (splitIndex/loopFactor+1)+"_"+opString+".gr";
	    System.out.println("Saving grammar to "+outTmpName+".");
	    if (pData.Save(outTmpName)) System.out.println("Saving successful.");
	    else System.out.println("Saving failed!");
	    pData = null;

	    //print grammar to MJ-type text file
	    //open file
	    try{
		FileWriter grtext_fw = new FileWriter(outTextTmpName+".all.txtgr");
		//write the grammar productions, PCFG style
		BufferedWriter grtext = new BufferedWriter(grtext_fw);
		maxGrammar.writeMJStyle(grtext);
		maxLexicon.writeMJStyle(grtext);
		grtext.close();

		//write the fragments
		FileWriter grfrag_fw = new FileWriter(outTextTmpName+".fragments.txtgr");
		//write the grammar productions, PCFG style
		grtext = new BufferedWriter(grfrag_fw);
		maxGrammar.writeFragments(grtext);
		grtext.close();
	    } catch(Exception e){
		System.err.println("Error: " + e.getMessage());
	    }
	    

	    System.out.println("AFTER " + opString + " EM: THE NEW GRAMMAR!!\n" + maxGrammar);
	    //System.out.println("internal node set is " + InternalNodeSet.toString2());
	    //System.out.println(maxLexicon);


	}
    
	// The last grammar/lexicon has not yet been evaluated. Even though the validation likelihood
	// has been dropping in the past few iteration, there is still a chance that the last one was in
	// fact the best so just in case we evaluate it.
	System.out.print("Calculating last validation likelihood...");
	double validationLikelihood = calculateLogLikelihood(grammar, lexicon, validationStateSetTrees);
	System.out.println("done.\n  Iteration "+iter+" (final) gives validation likelihood "+validationLikelihood);
	if (validationLikelihood > maxLikelihood) {
	    maxLikelihood = validationLikelihood;
	    maxGrammar = previousGrammar;
	    maxLexicon = previousLexicon;
	}
    
	ParserData pData = new ParserData(maxLexicon, maxGrammar, null, Numberer.getNumberers(), numSubStatesArray, VERTICAL_MARKOVIZATION, HORIZONTAL_MARKOVIZATION, binarization);

	System.out.println("Saving grammar to "+outFileName+".");
	System.out.println("It gives a validation data log likelihood of: "+maxLikelihood);
	if (pData.Save(outFileName)) System.out.println("Saving successful.");
	else System.out.println("Saving failed!");

	//print grammar to MJ-type text file
	//open file
	try{
	    FileWriter grtext_fw = new FileWriter(outFileName+".all.txtgr");
	    //write the grammar productions, PCFG style
	    BufferedWriter grtext = new BufferedWriter(grtext_fw);
	    maxGrammar.writeMJStyle(grtext);
	    maxLexicon.writeMJStyle(grtext);
	    grtext.close();

	    //write the fragments
	    FileWriter grfrag_fw = new FileWriter(outFileName+".fragments.txtgr");
	    //write the grammar productions, PCFG style
	    grtext = new BufferedWriter(grfrag_fw);
	    maxGrammar.writeFragments(grtext);
	    grtext.close();
	} catch(Exception e){
	    System.err.println("Error: " + e.getMessage());
	}
	    

    
	System.exit(0);
    }


    /**
     * @param previousGrammar
     * @param previousLexicon
     * @param grammar
     * @param lexicon
     * @param trainStateSetTrees
     * @return
     */
    public static double doOneEStep(Grammar previousGrammar, Lexicon previousLexicon, Grammar grammar, Lexicon lexicon, StateSetTreeList trainStateSetTrees, boolean updateOnlyLexicon) {
	boolean secondHalf = false;
	ArrayParser parser = new ArrayParser(previousGrammar,previousLexicon);
	double trainingLikelihood = 0;
	int n = 0;
	int nTrees = trainStateSetTrees.size();
	for (Tree<StateSet> stateSetTree : trainStateSetTrees) {
	    secondHalf = (n++>nTrees/2.0); 
	    boolean noSmoothing = true, debugOutput = false;
	    parser.doInsideOutsideScores(stateSetTree,noSmoothing,debugOutput);                    // E Step
	    double ll = stateSetTree.getLabel().getIScore(0);
	    ll = Math.log(ll) + (100*stateSetTree.getLabel().getIScale());//System.out.println(stateSetTree);
	    if ((Double.isInfinite(ll) || Double.isNaN(ll))) {
		if (VERBOSE){
		    System.out.println("Training sentence "+n+" is given "+ll+" log likelihood!");
		    System.out.println("Root iScore "+ stateSetTree.getLabel().getIScore(0)+" scale "+stateSetTree.getLabel().getIScale());
		}
	    }
	    else {
		lexicon.trainTree(stateSetTree, -1, previousLexicon, secondHalf,noSmoothing);
		if (!updateOnlyLexicon) grammar.tallyStateSetTree(stateSetTree, previousGrammar);      // E Step
		trainingLikelihood  += ll;  // there are for some reason some sentences that are unparsable 
	    }
	}
	return trainingLikelihood;
    }


    /**
     * @param maxGrammar
     * @param maxLexicon
     * @param validationStateSetTrees
     * @return
     */
    public static double calculateLogLikelihood(Grammar maxGrammar, Lexicon maxLexicon, StateSetTreeList validationStateSetTrees) {
	ArrayParser parser = new ArrayParser(maxGrammar, maxLexicon);
	int unparsable = 0;
	double maxLikelihood = 1.0;
	for (Tree<StateSet> stateSetTree : validationStateSetTrees) {
	    parser.doInsideScores(stateSetTree,false,false, null);  // Only inside scores are needed here
	    double ll = stateSetTree.getLabel().getIScore(0);
	    ll = Math.log(ll) + (100*stateSetTree.getLabel().getIScale());
	    if (Double.isInfinite(ll) || Double.isNaN(ll)) { 
		unparsable++;
		printBadLLReason(stateSetTree, (SophisticatedLexicon)maxLexicon);
	    }
	    else maxLikelihood += ll;  // there are for some reason some sentences that are unparsable 
	}
	maxLikelihood--;
	//		if (unparsable>0) System.out.print("Number of unparsable trees: "+unparsable+".");
	return maxLikelihood==0.0?Double.NEGATIVE_INFINITY:maxLikelihood;
    }


    /**
     * @param stateSetTree
     */
    public static void printBadLLReason(Tree<StateSet> stateSetTree, SophisticatedLexicon lexicon) {
	System.out.println(stateSetTree.toString());
	boolean lexiconProblem = false;
	List<StateSet> words = stateSetTree.getYield();
	Iterator<StateSet> wordIterator = words.iterator();
	for (StateSet stateSet : stateSetTree.getPreTerminalYield()) {
	    String word = wordIterator.next().getWord();
	    boolean lexiconProblemHere = true;
	    for (int i = 0; i < stateSet.numSubStates(); i++) {
		double score = stateSet.getIScore(i);
		if (!(Double.isInfinite(score) || Double.isNaN(score))) {
		    lexiconProblemHere = false;
		}
	    }
	    /*if (lexiconProblemHere) {
		System.out.println("LEXICON PROBLEM ON STATE " + stateSet.getState()+" word "+word);
		System.out.println("  word "+lexicon.wordCounter.getCount(stateSet.getWord()));
		for (int i=0; i<stateSet.numSubStates(); i++) {
		    System.out.println("  tag "+lexicon.tagCounter[stateSet.getState()][i]);
		    System.out.println("  word/state/sub "+lexicon.wordToTagCounters[stateSet.getState()].get(stateSet.getWord())[i]);
		}
	    }*/
	    lexiconProblem = lexiconProblem || lexiconProblemHere;
	}
	if (lexiconProblem)
	    System.out
		.println("  the likelihood is bad because of the lexicon");
	else
	    System.out
		.println("  the likelihood is bad because of the grammar");
    }
  
  
    /**
     * This function probably doesn't belong here, but because it should be called
     * after {@link #updateStateSetTrees}, Leon left it here.
     * 
     * @param trees Trees which have already had their inside-outside probabilities calculated,
     * as by {@link #updateStateSetTrees}.
     * @return The log likelihood of the trees.
     */
    public static double logLikelihood(List<Tree<StateSet>> trees, boolean verbose) {
	double likelihood = 0, l=0;
	for (Tree<StateSet> tree : trees) {
	    l = tree.getLabel().getIScore(0);
	    if (verbose) System.out.println("LL is "+l+".");
	    if (Double.isInfinite(l) || Double.isNaN(l)){
		System.out.println("LL is not finite.");
	    }
	    else {
		likelihood += l;
	    }
	}
	return likelihood;
    }
  
  
    /**
     * This updates the inside-outside probabilities for the list of trees using the parser's
     * doInsideScores and doOutsideScores methods.
     * 
     * @param trees A list of binarized, annotated StateSet Trees.
     * @param parser The parser to score the trees.
     */
    public static void updateStateSetTrees (List<Tree<StateSet>> trees, ArrayParser parser) {
	for (Tree<StateSet> tree : trees) {
	    parser.doInsideOutsideScores(tree,false,false);
	}
    }


    /**
     * Convert a single Tree[String] to Tree[StateSet]
     * 
     * @param tree
     * @param numStates
     * @param tagNumberer
     * @return
     */
  
    public static short[] initializeSubStateArray(List<Tree<String>> trainTrees,
						  List<Tree<String>> validationTrees, Numberer tagNumberer, short nSubStates){
	//			boolean dontSplitTags) {
	// first generate unsplit grammar and lexicon
	short[] nSub = new short[2];
	nSub[0] = 1;
	nSub[1] = nSubStates;

	// do the validation set so that the numberer sees all tags and we can
	// allocate big enough arrays
	// note: although this variable is never read, this constructor adds the
	// validation trees into the tagNumberer as a side effect, which is
	// important
	StateSetTreeList trainStateSetTrees = new StateSetTreeList(trainTrees, nSub, true, tagNumberer);
	@SuppressWarnings("unused")
	    StateSetTreeList validationStateSetTrees = new StateSetTreeList(validationTrees, nSub, true, tagNumberer);

	StateSetTreeList.initializeTagNumberer(trainTrees, tagNumberer);
	StateSetTreeList.initializeTagNumberer(validationTrees, tagNumberer);
    
	short numStates = (short)tagNumberer.total();
	short[] nSubStateArray = new short[numStates];
  	short two = nSubStates;
  	Arrays.fill(nSubStateArray, two);
  	//System.out.println("Everything is split in two except for the root.");
  	nSubStateArray[0] = 1; // that's the ROOT
	return nSubStateArray;
    }


    private static ConstraintSet readConstraintSet(String file){
	ConstraintSet constraintSet = new ConstraintSet();

	//read in the file
        Collection<Tree<String>> trees = null;
        
	System.out.println("reading constraint set from " + file);
	try {
	    trees = PennTreebankReader.readTrees(file,Charset.defaultCharset());
	} catch (Exception e) {
	    System.err.println(e);
	    System.exit(1);
	}
	Numberer tagnum = Numberer.getGlobalNumberer("tags");
	Numberer wordnum = Numberer.getGlobalNumberer("words");

	Map<Tree<String>,Set<Pair<Tree<String>,Integer>>> cmap = new HashMap<Tree<String>,Set<Pair<Tree<String>,Integer>>>();

	System.out.println(trees.size());
        for (Tree<String> tree : trees) {
	    System.out.println(tree);
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
		Map<Tree<String>,Pair<Tree<String>,Integer>> map = root.sliceSubtree(t, nodeNum);
		for(Map.Entry<Tree<String>,Pair<Tree<String>,Integer>> entry : map.entrySet()){
		    Set<Pair<Tree<String>,Integer>> bottoms = cmap.get(entry.getKey());
		    if(bottoms==null) bottoms = new HashSet<Pair<Tree<String>,Integer>>();
		    bottoms.add(entry.getValue());
		    cmap.put(entry.getKey(),bottoms);
		    //System.out.println("putting " + entry.getKey() + " goes to " + bottoms);
		}
		for(Tree<String> c : t.getChildren()){
		    Q.add(c);
		}
		nodeNum++;
	    }
	    //root = null;
	    constraintSet.add(tree);
        }
	constraintSet.setMap(cmap);
	cmap=null;
	System.gc();

	return constraintSet;
    }
 
}
