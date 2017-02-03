package snp.old;

import java.io.*;
import java.util.*;
import java.util.zip.DataFormatException;

import utility.*;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.*;
import org.apache.commons.cli.*;

import snp.ProbeInfo;
import snp.Segment;
import snp.SegmentContainer;
import snp.SegmentContainerMap;
import snp.SegmentContainer.SegmentIterator;
import sun.reflect.Reflection;

//use poisson binomial and optimize cutoff
public class SAIRIC6 extends SAIRIC5 {

	double ampUpperCutoff = 2.5;
	double ampLowerCutoff =  0.2;
	double delLowerCutoff = -2.5;
	double delUpperCutoff =  -0.2;
	double AIUpperCutoff = 0.8;
	double AILowerCutoff = 0.55;
	
	int numberOfTriedCutoffs = 10;
	
	
	public SAIRIC6(SegmentContainerMap baf,
			SegmentContainerMap logr, ProbeInfo pi)
			throws IOException, DataFormatException {
		super(baf, logr, pi);
	}
	
	
	protected  void calculateScoresForAmpDelandAI(){
		scores =  new MyMat(probeList, scoreTypes);
		poissonBinomialP = new MyMat(scoreTypes.subList(0, 4),new ArrayList<String>(sampleSet()));
		
		for(String s: sampleSet()){
			SegmentContainer bafSC = BAF.get(s);
			SegmentContainer logrSC = LogR.get(s);
			SegmentContainer.SegmentIterator bafSCitr  = bafSC.iterator();
			SegmentContainer.SegmentIterator logrSCitr  = logrSC.iterator();
			Segment S = bafSCitr.next();
			Segment S2 = logrSCitr.next();
			for(String p: probeSet()){
				int chr = probeInfo.chr(p);
				int pos = probeInfo.pos(p);
				while(chr > S.chr()){
					S = bafSCitr.next();
				}
				while(pos > S.end()){
					S = bafSCitr.next();
				}
				
				while(chr > S2.chr()){
					S2 = bafSCitr.next();
				}
				while(pos > S2.end()){
					S2 = bafSCitr.next();
				}
				
				double baf = S.value();
				double logr = S2.value();
				//amp
				if(logr > ampCutoff){
					scores.set(p, scoreTypes.get(0), scores.get(p, scoreTypes.get(0))+1);
					poissonBinomialP.set(scoreTypes.get(0), s, poissonBinomialP.get(scoreTypes.get(0), s) + 1);
				}
				//del
				if(logr < delCutoff){
					scores.set(p, scoreTypes.get(1), scores.get(p, scoreTypes.get(1))+1);
					poissonBinomialP.set(scoreTypes.get(1), s, poissonBinomialP.get(scoreTypes.get(1), s) + 1);
				}
					
				//AI
				if(baf>= AICutoff){
					scores.set(p, scoreTypes.get(3), scores.get(p, scoreTypes.get(3))+1);
					poissonBinomialP.set(scoreTypes.get(3), s, poissonBinomialP.get(scoreTypes.get(3), s) + 1);
				}
				
			}
		}
		List <String> tmp = new ArrayList<String>();
		tmp.add("amp");
		tmp.add("del");
		tmp.add("AI");
		for(String s: sampleSet()){
			poissonBinomialP.set("amp", s, poissonBinomialP.get("amp",s)/probeSet().size());
		}
		for(String s: sampleSet()){
			poissonBinomialP.set("del", s, poissonBinomialP.get("del",s)/probeSet().size());
		}
		for(String s: sampleSet()){
			poissonBinomialP.set("AI", s, poissonBinomialP.get("AI",s)/probeSet().size());
		}
	}
	

	protected  void calculatePvaluesForAmpDelandAI(){
		pvalues =  new MyMat(probeList, scoreTypes);
		List <String> tmp2 = new ArrayList<String>();
		tmp2.add("amp");
		tmp2.add("del");
		tmp2.add("AI");
		for(String s: tmp2){
			List <Double> tmp = scores.getCol(s);
			double M = MyFunc.max(tmp);
			double m = MyFunc.min(tmp);
			List <Integer> kk = new ArrayList<Integer>();
			for(int i=(int) m;i<=(int)M;i++){
				kk.add(i);
			}
			List<Double> pp = poissonBinomialP.getRow(s);
			List<Double> pv = getPoissonBinomialPvalue(kk,pp);
			
			Map<Integer, Double> kk2pv = new HashMap<Integer, Double>();
			for(int i=0; i<kk.size();i++){
				kk2pv.put(kk.get(i),pv.get(i));
			}
			
			for(String p: probeList){
				int score = (int)scores.get(p, s);
				double pvalue = kk2pv.get(score);
				pvalues.set(p, s, pvalue);
			}
		}
	}
	
	
	
	protected void optimizeCutoffs(){
		List <Double> ampCutoffs = new ArrayList<Double>();	 
		double d = (ampUpperCutoff-ampLowerCutoff)/(numberOfTriedCutoffs-1);
		for(double c = ampLowerCutoff; c < ampUpperCutoff; c+=d){
			ampCutoffs.add(c);
		}
		ampCutoffs.add(ampUpperCutoff);
		List <Double> delCutoffs = new ArrayList<Double>();
		d = (delUpperCutoff-delLowerCutoff)/(numberOfTriedCutoffs-1);
		for(double c = delLowerCutoff; c < delUpperCutoff; c+=d){
			delCutoffs.add(c);
		}
		delCutoffs.add(delUpperCutoff);
		List <Double> AICutoffs = new ArrayList<Double>();
		d = (AIUpperCutoff-AILowerCutoff)/(numberOfTriedCutoffs-1);
		for(double c = AILowerCutoff; c < AIUpperCutoff; c+=d){
			AICutoffs.add(c);
		}
		AICutoffs.add(AIUpperCutoff);
		
		double ampMetaScoreMax =  -1.0;
		double delMetaScoreMax =  -1.0;
		double AIMetaScoreMax =  -1.0;
		
		int ampIndex = 0;
		int delIndex = 0;
		int AIIndex = 0;
		
		for(int i = 0; i< numberOfTriedCutoffs; i++){
			ampCutoff = ampCutoffs.get(i);	
			delCutoff = delCutoffs.get(i);	
			AICutoff = AICutoffs.get(i);
			calculateScoresForAmpDelandAI();
			calculatePvaluesForAmpDelandAI();
			double ampMetaScore =  metaScore(pvalues.getCol("amp"));
			double delMetaScore =  metaScore(pvalues.getCol("del"));
			double AIMetaScore =  metaScore(pvalues.getCol("AI"));
			
			if(ampMetaScore > ampMetaScoreMax){
				ampIndex = i;
				ampMetaScoreMax = ampMetaScore;
			}
			if(delMetaScore > delMetaScoreMax){
				delIndex = i;
				delMetaScoreMax = delMetaScore;
			}
			if(AIMetaScore > AIMetaScoreMax){
				AIIndex = i;
				AIMetaScoreMax = AIMetaScore;
			}
		}
		ampCutoff = ampCutoffs.get(ampIndex);
		delCutoff = delCutoffs.get(delIndex);
		AICutoff = AICutoffs.get(AIIndex);	
		System.err.println("ampCutoff=" + ampCutoff);
		System.err.println("delCutoff=" + delCutoff);
		System.err.println("AICutoff=" + AICutoff);
	}
	
	private double  metaScore(List<Double> v){
		List <Double> tmp = new ArrayList<Double>();
		for(int i = 0; i < v.size();i++){
			tmp.add(-Math.log10(v.get(i)));
		}
		return MyFunc.sd(tmp);
	}
	
	
	public static void main(String [] args) throws Exception{
		Options options = new Options();
		options.addOption("a", "ampcutoff", true, "cutoff for amplification call");
		options.addOption("d", "delcutoff", true, "cutoff for deletion call");
		options.addOption("A", "aicutoff", true, "cutoff for AI call");
		options.addOption("n", "ndsize", true, "size of null distributions");
		options.addOption("o", "outfile", true, "output file name");
		options.addOption("t", "thinpi", true, "thin down probe info");
		options.addOption("i", "interval", true, "interval for psuedo probe info");
		options.addOption("c", "cutbyper", true, "amp and del cutoff by percentile");
		options.addOption("s", "score", true, "score file name");
		options.addOption("O", "optcutoff", false, "optimize cutoff");
		HelpFormatter formatter = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
		CommandLine commandLine;
		try{
			commandLine = parser.parse(options, args);
		}catch (Exception e) {
			formatter.printHelp(Reflection.getCallerClass( 1 ).getName() + " [options] BAFSegFile LogRSegFile probeTsvFile", options);
			return ;
		}
		List <String> argList = commandLine.getArgList();
		if(!(argList.size() == 3 | argList.size() == 2)){
			formatter.printHelp(Reflection.getCallerClass( 1 ).getName() + " [options] BAFSegFile LogRSegFile probeTsvFile", options);
			return;
		}
		SegmentContainerMap BAF = new SegmentContainerMap(argList.get(0));
		SegmentContainerMap LogR = new SegmentContainerMap(argList.get(1));
		ProbeInfo PI;
		List<String> sample = new ArrayList<String>(BAF.keySet());
		if(argList.size() == 3 ){
			PI = 	ProbeInfo.getProbeInfoFromTsvFile(argList.get(2));
			if(commandLine.hasOption("t")){
				PI.thinDown(Integer.valueOf(commandLine.getOptionValue("t")));
			}
			PI.filter(BAF.get(sample.get(0)));
		}else{
			int interval = 10000000;
			if(commandLine.hasOption("i")){
				interval = Integer.valueOf(commandLine.getOptionValue("i"));
			}
			PI = ProbeInfo.generatePsuedoProbeInfo(interval);
			PI.filter(BAF.get(sample.get(0)));		
		}
		
		SAIRIC6 SAIRIC = new SAIRIC6(BAF,LogR,PI);
		if(commandLine.hasOption("a")){
			SAIRIC.ampCutoff = Double.valueOf(commandLine.getOptionValue("a"));
		}
		if(commandLine.hasOption("d")){
			SAIRIC.delCutoff = Double.valueOf(commandLine.getOptionValue("d"));
		}
		if(commandLine.hasOption("A")){
			SAIRIC.AICutoff = Double.valueOf(commandLine.getOptionValue("A"));
		}
		if(commandLine.hasOption("p")){
			SAIRIC.nullDistSize = Integer.valueOf(commandLine.getOptionValue("p"));
		}
		if(commandLine.hasOption("c")){
			SAIRIC.setLogRcutoffByPercentile(Double.valueOf(commandLine.getOptionValue("c")));
		}
		if(commandLine.hasOption("O")){
			SAIRIC.optimizeCutoffs();
		}
		
		SAIRIC.perform();
		MyMat tmp =  SAIRIC.getMinusLogQvalues();
		
		Writer os;
		if(commandLine.hasOption("o")){
			 os = new BufferedWriter(new FileWriter(commandLine.getOptionValue("o")));
		}else{
			os = new PrintWriter(System.out);
		}
		os.write("Probe" + "\t" +  "Chrom" + "\t" +  "BasePair" + "\t" + MyFunc.join("\t", SAIRIC.scoreTypes) + "\n");
		for(String s: PI.getProbeSet()){
			os.write(s + "\t" +  PI.chr(s) + "\t" +  PI.pos(s) + "\t" + MyFunc.join("\t", tmp.getRow(s))+ "\n");			
		}
		os.flush();
		if(commandLine.hasOption("s")){
			 os = new BufferedWriter(new FileWriter(commandLine.getOptionValue("s")));
			 tmp = SAIRIC.getScores();
			 os.write("Probe" + "\t" +  "Chrom" + "\t" +  "BasePair" + "\t" + MyFunc.join("\t", SAIRIC.scoreTypes)+ "\n");
			 for(String s: PI.getProbeSet()){
				 os.write(s + "\t" +  PI.chr(s) + "\t" +  PI.pos(s) + "\t" + MyFunc.join("\t", tmp.getRow(s))+ "\n");			
			 }
			 os.flush();
		}
	}

	

}