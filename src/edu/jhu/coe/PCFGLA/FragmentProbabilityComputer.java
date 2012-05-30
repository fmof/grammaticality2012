package edu.jhu.coe.PCFGLA;

import edu.jhu.coe.syntax.BerkeleyCompatibleFragment;
import edu.jhu.coe.util.CounterMap;

public class FragmentProbabilityComputer{

    private LatentStatistics latentStatistics;
    private CounterMap<Integer,Integer> unnormalizedSymbolCounter;
    private double gamma;

    public FragmentProbabilityComputer(LatentStatistics LS, double gamma){
	latentStatistics = LS;
	this.gamma = gamma;
    }

    public FragmentProbabilityComputer(LatentStatistics LS, CounterMap<Integer,Integer> uSymCounter, double gamma){
	latentStatistics = LS;
	unnormalizedSymbolCounter = uSymCounter;
	this.gamma = gamma;
    }

    private double boundBy(double x, double bound){
	return x>0?x:bound;
    }

    public double computeForFragment(double prevProb, short rootTag, short rootSig, BerkeleyCompatibleFragment X){
	//System.out.println(latentStatistics.getUpperCouplingFractionalCount(X, gamma));
	if(X==null) return prevProb;
	double subsumerCount = latentStatistics.getUpperCouplingFractionalCount(X,gamma);
	double numerator = latentStatistics.getValidBottomCountOf(X);
	double denominator = latentStatistics.getGeneralValidBottomCountOf(rootTag, rootSig);
	double subsumedCount = (denominator==0.0 || numerator<=0.0)?0.0:((numerator)/(denominator));
	double sub = Math.abs(subsumerCount - subsumedCount);
	//System.err.print("computing: "+ prevProb +", "+numerator +", "+ denominator+", "+ subsumerCount +", "+ subsumedCount+":: ");
	//System.err.print("computing: "+ prevProb +", "+ subsumerCount +", "+ subsumedCount+":: ");
	double retNumerator = prevProb * boundBy(sub,1);
	//System.err.println(retNumerator);
	return retNumerator;
    }

    public double computeForPreterminal(double prevProb, short rootTag, short rootSig){
	double frac = gamma * latentStatistics.getGeneralValidBottomCountOf(rootTag,rootSig);
	return frac==0?prevProb:(frac*prevProb);
    }

    public double computeForComposition(double prevProb, short rootTag, short rootSig, BerkeleyCompatibleFragment X, BerkeleyCompatibleFragment Y){
	//System.out.println("computing prob for top="+X+" and bottom="+Y);
	//System.out.println("\t"+latentStatistics.getCoupledFractionalCount(X,Y));
	//double numerator = gamma * prevProb * latentStatistics.getCoupledFractionalCount(X,Y);
	//double numerator = gamma * prevProb * unnormalizedSymbolCounter.getCount((int)rootTag, (int)rootSig) * latentStatistics.getCoupledFractionalCount(X,Y);
	//double denominator = latentStatistics.getCountOfNTSymbol(rootTag, rootSig) - (gamma * latentStatistics.getGeneralValidBottomCountOf(rootTag, rootSig));
	return gamma*prevProb*latentStatistics.getCoupledFractionalCount(X,Y);
    }

    public double computeForFragment2200Mar4(double prevProb, short rootTag, short rootSig, BerkeleyCompatibleFragment X){
	//System.out.println(latentStatistics.getUpperCouplingFractionalCount(X, gamma));
	double subsumerCount = latentStatistics.getUpperCouplingFractionalCount(X,gamma);
	double numerator = latentStatistics.getUnigramFragmentCount(X) - latentStatistics.getValidBottomCountOf(X);
	double denominator = latentStatistics.getCountOfNTSymbol(rootTag, rootSig) - (gamma * latentStatistics.getGeneralValidBottomCountOf(rootTag, rootSig));
	double subsumedCount = (denominator==0.0 || numerator<=0.0)?0.0:((numerator)/(denominator));
	double sub = Math.abs(subsumerCount - subsumedCount);
	double retNumerator = prevProb * boundBy(sub,1);
	return retNumerator;
    }

    public double computeForComposition2200Mar4(double prevProb, short rootTag, short rootSig, BerkeleyCompatibleFragment X, BerkeleyCompatibleFragment Y){
	//System.out.println("computing prob for top="+X+" and bottom="+Y);
	//System.out.println("\t"+latentStatistics.getCoupledFractionalCount(X,Y));
	double numerator = gamma * prevProb * latentStatistics.getCoupledFractionalCount(X,Y);
	//double numerator = gamma * prevProb * unnormalizedSymbolCounter.getCount((int)rootTag, (int)rootSig) * latentStatistics.getCoupledFractionalCount(X,Y);
	//double denominator = latentStatistics.getCountOfNTSymbol(rootTag, rootSig) - (gamma * latentStatistics.getGeneralValidBottomCountOf(rootTag, rootSig));
	return gamma*prevProb*latentStatistics.getCoupledFractionalCount(X,Y);
    }

    public double computeForFragmentOLD(double prevProb, short rootTag, short rootSig, BerkeleyCompatibleFragment X){
	//System.out.println(latentStatistics.getUpperCouplingFractionalCount(X, gamma));
	double oldprevProb = prevProb;
	prevProb = latentStatistics.getUnigramFragmentCount(X);
	double numerator = (prevProb * latentStatistics.getUpperCouplingFractionalCount(X, gamma)) - (gamma * latentStatistics.getValidBottomCountOf(X));
	//double numerator = (prevProb * unnormalizedSymbolCounter.getCount((int)rootTag, (int)rootSig) * latentStatistics.getUpperCouplingFractionalCount(X, gamma)) - (gamma * latentStatistics.getValidBottomCountOf(X));
	double denominator = latentStatistics.getCountOfNTSymbol(rootTag, rootSig) - (gamma * latentStatistics.getGeneralValidBottomCountOf(rootTag, rootSig));
	System.out.println("for fragment " + X + ", computing uni frag : " + numerator + " ;; " + denominator);
	System.out.println("\tfragment " + X + " prevProb = " + prevProb + " noramlization = " + unnormalizedSymbolCounter.getCount((int)rootTag, (int)rootSig) + " first part = " + prevProb * unnormalizedSymbolCounter.getCount((int)rootTag, (int)rootSig) * latentStatistics.getUpperCouplingFractionalCount(X, gamma));
	return denominator==0?oldprevProb:(numerator/denominator);
    }

    public double computeForCompositionOLD(double prevProb, short rootTag, short rootSig, BerkeleyCompatibleFragment X, BerkeleyCompatibleFragment Y){
	System.out.println("computing prob for top="+X+" and bottom="+Y);
	System.out.println("\t"+latentStatistics.getCoupledFractionalCount(X,Y));
	prevProb = latentStatistics.getUnigramFragmentCount(X);
	double numerator = gamma * prevProb * latentStatistics.getCoupledFractionalCount(X,Y);
	//double numerator = gamma * prevProb * unnormalizedSymbolCounter.getCount((int)rootTag, (int)rootSig) * latentStatistics.getCoupledFractionalCount(X,Y);
	double denominator = latentStatistics.getCountOfNTSymbol(rootTag, rootSig) - (gamma * latentStatistics.getGeneralValidBottomCountOf(rootTag, rootSig));
	return denominator==0.0?-4.0:(numerator/denominator);
    }


}