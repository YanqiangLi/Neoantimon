package snp.old;

import java.io.*;
import java.util.*;

import org.apache.commons.cli.*;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.*;

import snp.ProbeInfo;
import snp.SegmentContainerMap;
import sun.reflect.Reflection;
import utility.*;

public class SAIRIC3 extends SAIRIC{
	
	protected List <Double> binomialParameterP;
	
	protected List <Distribution> nullDistributions;
	
	public SAIRIC3(SegmentContainerMap baf,SegmentContainerMap logr, ProbeInfo pi)throws IOException {
		super(baf, logr, pi);
	}
	
	protected int nullDistSize = 100000;
	
	protected List <Double> nullBAF;
	protected List <Double> nullLogR;
	
	protected double LogRcutoffByPercentile = 0;
	
	protected void geneNullBAFandLogR(){
		nullBAF = new ArrayList<Double>();
		nullLogR = new ArrayList<Double>();
		for(int i = 0; i < nullDistSize; i++){
			String p = generateRandomProbes();
			String s = generateRandomSamples();
			int chr = probeInfo.chr(p);
			int pos = probeInfo.pos(p);
			double baf = BAF.get(s).get(chr, pos).value();
			double logr = LogR.get(s).get(chr, pos).value();
			nullBAF.add(baf);
			nullLogR.add(logr);
		}
		if(LogRcutoffByPercentile >0){
			if(LogRcutoffByPercentile > 1){
				LogRcutoffByPercentile /= 100;
			  }
			ampCutoff = MyFunc.percentile(nullLogR,1-LogRcutoffByPercentile);
			delCutoff = MyFunc.percentile(nullLogR,LogRcutoffByPercentile);
			System.err.println("ampCutoff=" + ampCutoff);
			System.err.println("delCutoff=" + delCutoff);
		}
	}
	
	private void calculateBinomialParameterP(){
		binomialParameterP = new ArrayList<Double>();
		for(int i = 0; i < scoreTypes.size();i++){
			binomialParameterP.add(0.0);
		}
		geneNullBAFandLogR();
		
		for(int i = 0; i < nullDistSize; i++){
			double baf = nullBAF.get(i);
			double logr = nullLogR.get(i);
			//amp
			if(logr > ampCutoff){
				binomialParameterP.set(0,binomialParameterP.get(0)+1);
			}
			//del
			if(logr < delCutoff){
				binomialParameterP.set(1,binomialParameterP.get(1)+1);
			}
			//neut
			if(logr >= delCutoff & logr <= ampCutoff){
				binomialParameterP.set(2,binomialParameterP.get(2)+1);
			}	
			//AI
			if(baf>= AICutoff){
				binomialParameterP.set(3,binomialParameterP.get(3)+1);
			}
			//ampAI
			if(baf>= AICutoff & logr > ampCutoff){
				binomialParameterP.set(4,binomialParameterP.get(4)+1);
			}
			//delAI
			if(baf>= AICutoff & logr < delCutoff){
				binomialParameterP.set(5,binomialParameterP.get(5)+1);
			}
			//neutAI
			if(baf>= AICutoff & logr >= delCutoff & logr <= ampCutoff){
				binomialParameterP.set(6,binomialParameterP.get(6)+1);
			}
		}
		binomialParameterP.set(4,binomialParameterP.get(4)/ binomialParameterP.get(0));
		binomialParameterP.set(5,binomialParameterP.get(5)/ binomialParameterP.get(1));
		binomialParameterP.set(6,binomialParameterP.get(6)/ binomialParameterP.get(2));
		for(int i = 0; i < 4;i++){
			binomialParameterP.set(i,binomialParameterP.get(i)/ nullDistSize);
		}
		
		for(int i = 0; i < scoreTypes.size();i++){
			System.err.println(scoreTypes.get(i) + ": " +  binomialParameterP.get(i));
		}
	}
	
	protected void prepearNullDistributions(){
		nullDistributions = new ArrayList<Distribution>();
		calculateBinomialParameterP();
		for(int i = 0; i < scoreTypes.size();i++){
		double p = binomialParameterP.get(i);
		int n = sampleList.size();
		Distribution D;
		D = new BinomialDistributionImpl(n,p);
		nullDistributions.add(D);
	}
}
		
	protected  void calculatePvalues(){
		pvalues =  new MyMat(probeList, scoreTypes);
		for(int i = 0; i < 4; i++){
			for(String p: probeList){
				double score = scores.get(p, scoreTypes.get(i));
				double pvalue = 1;
				try {
					pvalue = 1- nullDistributions.get(i).cumulativeProbability(score);
				} catch (MathException e) {
					e.printStackTrace();
				} 
				pvalues.set(p, scoreTypes.get(i), pvalue);
			}
		}
		
		for(int i = 4; i < 7; i++){
		for(String p: probeList){
			double score = scores.get(p, scoreTypes.get(i));
			double pvalue = 1;
			int n = (int) scores.get(p, scoreTypes.get(i-4));
			if(n==0){
				pvalues.set(p, scoreTypes.get(i), pvalue);
				continue;
			}
			BinomialDistributionImpl BD = (BinomialDistributionImpl) nullDistributions.get(i);
			BD.setNumberOfTrials(n);
			
			try {
				pvalue = 1- BD.cumulativeProbability(score);
			} catch (MathException e) {
				e.printStackTrace();
			} 
			pvalues.set(p, scoreTypes.get(i), pvalue);
		}
		}
	}
		
	
	public void perform(){
		System.err.println("calculate scores....");
		calculateScores();
		System.err.println("prepare null distributions....");
		prepearNullDistributions();
		System.err.println("calculate pvalues....");
		calculatePvalues();
		System.err.println("calculate qvalues....");
		calculateQvalues();

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
			
			SAIRIC3 SAIRIC = new SAIRIC3(BAF,LogR,PI);
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
				SAIRIC.LogRcutoffByPercentile = Double.valueOf(commandLine.getOptionValue("c"));
			}
			
			SAIRIC.perform();
			MyMat tmp =  SAIRIC.getMinusLogQvalues();
			
			PrintWriter os;
			if(commandLine.hasOption("o")){
				 os = new PrintWriter(new FileWriter(commandLine.getOptionValue("o")));
			}else{
				os = new PrintWriter(System.out);
			}
			os.println("Probe" + "\t" +  "Chrom" + "\t" +  "BasePair" + "\t" + MyFunc.join("\t", SAIRIC.scoreTypes));
			for(String s: PI.getProbeSet()){
				os.println(s + "\t" +  PI.chr(s) + "\t" +  PI.pos(s) + "\t" + MyFunc.join("\t", tmp.getRow(s)));			
			}
			os.flush();
		}
			
	
}