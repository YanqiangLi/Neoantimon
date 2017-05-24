package snp;

import java.util.*;
import java.io.*;
import org.apache.commons.cli.*;

import sun.reflect.Reflection;
import utility.*;

public class PARTmatrix{
	
	protected MyMat  BAF; 
	protected List<String>probeList;
	protected List<String>sampleList; 
	
	protected Map<String, Double> scores;
	protected Map<String, Double> pvalues;
	protected Map<String, Double> qvalues;
	
	protected Map<String, Double> poissonBinomialP;
	
	protected double AICutoff =  0.5;
	
	
	double AIUpperCutoff = 3.0;
	double AILowerCutoff = -3.0;
	int numberOfTriedCutoffs = 10;
	
	boolean countLess = false;
	public PARTmatrix(){};
	public PARTmatrix(MyMat baf) throws IOException{
		BAF = baf; 
		probeList = new ArrayList<String>(probeSet());
		sampleList = new ArrayList<String>(sampleSet());
	}
	
	protected Set<String> sampleSet(){
		return new HashSet<String>(BAF.getColNames());
	}
	
	protected Set<String> probeSet(){
		return new HashSet<String>(BAF.getRowNames());
	}
	
	protected void setLogRcutoffByPercentile(double LogRcutoffByPercentile){
		List <Double> nullLogR = new ArrayList<Double>();
		for(int i = 0; i < 10000; i++){
			String p = generateRandomProbes();
			String s = generateRandomSamples();
			double baf = BAF.get(p,s);
			nullLogR.add(baf);
	}
	if(LogRcutoffByPercentile > 1){
		LogRcutoffByPercentile /= 100;
	}
	if(!countLess){
		AICutoff = MyFunc.percentile(nullLogR,1-LogRcutoffByPercentile);
	}else{
		AICutoff = MyFunc.percentile(nullLogR,LogRcutoffByPercentile);
	}	
	System.err.println("set cutoff=" + AICutoff);
}

protected String generateRandomProbes(){
	int i = (int) Math.floor(Math.random()*probeList.size());
	return probeList.get(i);
}
protected String generateRandomSamples(){
	int i = (int) Math.floor(Math.random()*sampleList.size());
	return sampleList.get(i);
}
	
	
	protected void calculateScores(){
		scores =  new LinkedHashMap<String, Double>();
		for(String p: probeSet()){
			scores.put(p,0.0);
		}
		poissonBinomialP = new HashMap<String, Double>();
		for(String s: sampleSet()){
			poissonBinomialP.put(s,0.0);
		}
		for(String s: sampleSet()){
			for(String p: probeSet()){
				double baf = BAF.get(p,s);
				if((countLess & baf<= AICutoff)| (!countLess & baf>= AICutoff)){
					scores.put(p, scores.get(p)+1);
					poissonBinomialP.put(s, poissonBinomialP.get(s) + 1);
				}
			}
		}
		
		for(String s: sampleSet()){
			poissonBinomialP.put(s, poissonBinomialP.get(s)/probeSet().size());
		}
		System.err.println("Ratio of velues above cutoff: " + MyFunc.mean(new ArrayList<Double>(poissonBinomialP.values())));
		
	}
	
	
	protected  void calculatePvalues(){
		pvalues =  new LinkedHashMap<String,Double>();
		List <Double> tmp = new ArrayList<Double>(scores.values());
			double M = MyFunc.max(tmp);
			double m = MyFunc.min(tmp);
			List <Integer> kk = new ArrayList<Integer>();
			for(int i=(int) m;i<=(int)M;i++){
				kk.add(i);
			}
			List<Double> pp = new ArrayList<Double>(poissonBinomialP.values());
			List<Double> pv = PART.getPoissonBinomialPvalue(kk,pp);
			
			Map<Integer, Double> kk2pv = new HashMap<Integer, Double>();
			for(int i=0; i<kk.size();i++){
				kk2pv.put(kk.get(i),pv.get(i));
			}
			
			for(String p: probeList){
				int score = (int)(double)scores.get(p);
				double pvalue = kk2pv.get(score);
				pvalues.put(p, pvalue);
			}
		}
	
	protected void optimizeCutoffs(){;
		List <Double> AICutoffs = new ArrayList<Double>();
		double d = (AIUpperCutoff-AILowerCutoff)/(numberOfTriedCutoffs-1);
		for(double c = AILowerCutoff; c < AIUpperCutoff; c+=d){
			AICutoffs.add(c);
		}
		AICutoffs.add(AIUpperCutoff);
		
		double AIMetaScoreMax =  -1.0;
		
		int AIIndex = 0;
		
		for(int i = 0; i< numberOfTriedCutoffs; i++){
			AICutoff = AICutoffs.get(i);
			calculateScores();
			calculatePvalues();
			double AIMetaScore =  metaScore(new ArrayList<Double>(pvalues.values()));
			System.err.println(i + " " +  AICutoff + " " + AIMetaScoreMax + " " +  AIMetaScore);
			if(AIMetaScore > AIMetaScoreMax){
				AIIndex = i;
				AIMetaScoreMax = AIMetaScore;
			}
			
		}
		AICutoff = AICutoffs.get(AIIndex);	
		System.err.println("cutoff=" + AICutoff);
	}
	
	private double  metaScore(List<Double> v){
		List <Double> tmp = new ArrayList<Double>();
		for(int i = 0; i < v.size();i++){
			tmp.add(-Math.log10(v.get(i)));
		}
		return MyFunc.sd(tmp);
	}

	protected  void calculateQvalues(){
		qvalues = MyFunc.calculateQvalue(pvalues);
		for(String p: qvalues.keySet()){ 
			qvalues.put(p, qvalues.get(p)>1?1:qvalues.get(p));
		}
	}
	public void perform(){
		System.err.println("calculate scores....");
		calculateScores();
		
		System.err.println("calculate pvalues....");
		calculatePvalues();
		
		System.err.println("calculate qvalues....");
		calculateQvalues();
	}
	
	public Map <String, Double> getQvalues(){
		return qvalues;
	}
	
	public Map <String, Double> getPvalues(){
		return pvalues;
	}
	
	public Map <String, Double> getScores(){
		return scores;
	}
	
	
	public Map <String, Double> getMinusLogQvalues(){
		Map <String, Double> minusLogQvalues = new LinkedHashMap<String, Double>();
		for(String p: probeList){
		double tmp = qvalues.get(p);
		if(tmp==1){
			tmp=0;
		}else{
			tmp = - Math.log10(tmp);
		}
		minusLogQvalues.put(p, tmp);
		}
		return minusLogQvalues;
	}
	public Map <String, Double> getMinusLogPvalues(){
		Map <String, Double> minusLogPvalues = new LinkedHashMap<String, Double>();
		for(String p: probeList){
		double tmp = pvalues.get(p);
		if(tmp==1){
			tmp=0;
		}else{
			tmp = - Math.log10(tmp);
		}
		minusLogPvalues.put(p, tmp);
		}
		return minusLogPvalues;
	}
	

	public static void main(String [] args) throws Exception{
		Options options = new Options();
		options.addOption("c", "cutoff", true, "cutoff for aberration");
		options.addOption("o", "outfile", true, "output file name");
		options.addOption("O", "optcutoff", true, "optimize cutoff with [lower:upper:numberOfCutoffs]");
		options.addOption("l", "less", false, "count less than cutoff");
		options.addOption("C", "cutbyper", true, "cutoff by percentile");
		options.addOption("q", "qcutoff", true, "cutoff for q-value");
		options.addOption("L", "log", false, "log scale");
		HelpFormatter formatter = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
		CommandLine commandLine = null;
		try{
			commandLine = parser.parse(options, args);
		}catch (Exception e) {
			formatter.printHelp(Reflection.getCallerClass( 1 ).getName() + " [options] SegFile  probeTsvFile", options);
			System.exit(1);
		}
		List <String> argList = commandLine.getArgList();
		if(!(argList.size() == 1)){
			formatter.printHelp(Reflection.getCallerClass( 1 ).getName() + " [options] SegFile probeTsvFile", options);
			System.exit(1);
		}
		MyMat BAF = new MyMat(argList.get(0));
		
		double Qcutoff = 0.01;
		if(commandLine.hasOption("L")){
			Qcutoff = 1;
		}
		PARTmatrix PART = new PARTmatrix(BAF);
		if(commandLine.hasOption("c")){
			PART.AICutoff = Double.valueOf(commandLine.getOptionValue("c"));
		}
		if(commandLine.hasOption("l")){
			PART.countLess = true;
		}
		if(commandLine.hasOption("q")){
			Qcutoff = Double.valueOf(commandLine.getOptionValue("q"));
		}
		if(commandLine.hasOption("C")){
			PART.setLogRcutoffByPercentile(Double.valueOf(commandLine.getOptionValue("C")));
		}
	
		if(commandLine.hasOption("O")){
			List <String>tmp = MyFunc.split(":",commandLine.getOptionValue("O"));
			if(tmp.size()==3){
				PART.AIUpperCutoff = Double.valueOf(tmp.get(1));
				PART.AILowerCutoff = Double.valueOf(tmp.get(0));
				PART.numberOfTriedCutoffs = Integer.valueOf(tmp.get(2));
				PART.optimizeCutoffs();
			}else{
				formatter.printHelp(Reflection.getCallerClass( 1 ).getName() + " [options] SegFile  probeTsvFile", options);
				System.exit(1);
			}
		}
	
		PART.perform();
		
		Writer os;
		if(commandLine.hasOption("o")){
			 os = new BufferedWriter(new FileWriter(commandLine.getOptionValue("o")));	 
		}else{
			os = new PrintWriter(System.out);
		}
		os.write("Probe" + "\t" + "pvalue" + "\t" + "qvalue" + "\t" + "rate" + "\n");
			
		if(commandLine.hasOption("L")){
			Map<String, Double> p = PART.getMinusLogPvalues();
			Map<String, Double> q = PART.getMinusLogQvalues();
			for(String s: MyFunc.sortKeysByAscendingOrderOfValues(q)){
				if(q.get(s) > Qcutoff){
					os.write(s  + "\t" + + p.get(s) + "\t" + q.get(s) + "\t"  + PART.scores.get(s)/PART.sampleList.size()  + "\n");			
				}
			}
			os.flush();
		}else{
			Map<String, Double> p = PART.getPvalues();
			Map<String, Double> q = PART.getQvalues();
			for(String s: MyFunc.sortKeysByAscendingOrderOfValues(q)){
				if(q.get(s) < Qcutoff){
					os.write(s  + "\t" + + p.get(s) + "\t" + q.get(s) + "\t"  + PART.scores.get(s)/PART.sampleList.size()  + "\n");			
				}
			}
			os.flush();
		}
	}
}
	
	

