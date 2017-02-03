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


// use poisson binomial 
public class SAIRIC5 extends SAIRIC4 {

	public SAIRIC5(SegmentContainerMap baf,SegmentContainerMap logr, ProbeInfo pi)throws IOException, DataFormatException {
		super(baf, logr, pi);
	}
	
	protected MyMat poissonBinomialP; //[amp, del, neut, AI] x sample 
	protected MyMat normalStatistics;  //[ampAI, delAI, neutAI] x [mean, sd, shapiroP]
	
	
	protected void calculateScores(){
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
					S2 = logrSCitr.next();
				}
				while(pos > S2.end()){
					S2 = logrSCitr.next();
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
				//neut
				if(logr >= delCutoff & logr <= ampCutoff){
					scores.set(p, scoreTypes.get(2), scores.get(p, scoreTypes.get(2))+1);
					poissonBinomialP.set(scoreTypes.get(2), s, poissonBinomialP.get(scoreTypes.get(2), s) + 1);
				}	
				//AI
				if(baf>= AICutoff){
					scores.set(p, scoreTypes.get(3), scores.get(p, scoreTypes.get(3))+1);
					poissonBinomialP.set(scoreTypes.get(3), s, poissonBinomialP.get(scoreTypes.get(3), s) + 1);
				}
				//ampAI
				if(baf>= AICutoff & logr > ampCutoff){
					scores.set(p, scoreTypes.get(4), scores.get(p, scoreTypes.get(4))+1);
				}
				//delAI
				if(baf>= AICutoff & logr < delCutoff){
					scores.set(p, scoreTypes.get(5), scores.get(p, scoreTypes.get(5))+1);
				}
				//neutAI
				if(baf>= AICutoff & logr >= delCutoff & logr <= ampCutoff){
					scores.set(p, scoreTypes.get(6), scores.get(p, scoreTypes.get(6))+1);
				}
			}
		}
		for(String p: probeSet()){
			for(int i = 4; i < 7;i++){
				if(scores.get(p, scoreTypes.get(i-4))!=0){
					scores.set(p, scoreTypes.get(i), scores.get(p, scoreTypes.get(i))/scores.get(p, scoreTypes.get(i-4)));
				}else{
					scores.set(p, scoreTypes.get(i),Double.NaN);
				}
			}
		}
		for(String s: sampleSet()){
			poissonBinomialP.set("amp", s, poissonBinomialP.get("amp",s)/probeSet().size());
		}
		for(String s: sampleSet()){
			poissonBinomialP.set("del", s, poissonBinomialP.get("del",s)/probeSet().size());
		}
		
		for(String s1: scoreTypes.subList(2, 4)){
			for(String s2: sampleSet()){
				poissonBinomialP.set(s1, s2, poissonBinomialP.get(s1,s2)/probeSet().size());
			}
		}
	}
	
	
	protected void caluculateNormalStatistics(){
		List <String> tmp = new ArrayList<String>();
		tmp.add("mean");
		tmp.add("sd");
		tmp.add("shapiroP");
		normalStatistics = new MyMat(scoreTypes.subList(4, 7), tmp);
		for(String s: scoreTypes.subList(4, 7)){
			List<Double> v = nullScores.getCol(s);
			normalStatistics.set(s, "mean", MyFunc.mean(v));
			normalStatistics.set(s, "sd", MyFunc.sd(v));
			
			if(v.size() > 5000){
				v = v.subList(0, 4999);
			}
			normalStatistics.set(s, "shapiroP", MyFunc.shapiroTest(v));
		}
	}

	protected  void calculatePvalues(){
		pvalues =  new MyMat(probeList, scoreTypes);
		for(String s: scoreTypes.subList(0, 4)){
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
		for(String s: scoreTypes.subList(4, 7)){
			Distribution D = new NormalDistributionImpl(normalStatistics.get(s,"mean"), normalStatistics.get(s,"sd"));
			for(String p: probeList){
				Double score = scores.get(p, s);
				double pvalue = 1;
				if(!score.isNaN()){
					try {
						pvalue = 1- D.cumulativeProbability(score);
					} catch (MathException e) {
						e.printStackTrace();
					} 
				}
				pvalues.set(p, s, pvalue);
			}
		}
	}
	
	
	public static List<Double> getPoissonBinomialPvalue(List<Integer> kk, List<Double> pp){
		   String tmpFile = "tmp" + Math.round(Math.random()*100000000); 
		   try {
			   PrintWriter os;
			   os = new PrintWriter(new FileWriter(tmpFile + ".kk"));
			   for(int i = 0, n = kk.size(); i < n; i++){
				   os.println(kk.get(i).toString());
			   }
			   os.flush();
			   os.close();
			   os = new PrintWriter(new FileWriter(tmpFile + ".pp"));
			   for(int i = 0, n = pp.size(); i < n; i++){
				   os.println(pp.get(i).toString());
			   }
			   os.flush();
			   os.close();
		   
			   os = new PrintWriter(new FileWriter(tmpFile + ".R"));
			   os.println("kk<-scan(\"" +  tmpFile + ".kk" + "\")");
			   os.println("pp<-scan(\"" +  tmpFile + ".pp" + "\")");
			   os.println("library(poibin)");
			   os.println("pv<-1-ppoibin(kk=kk, pp=pp, method = \"DFT-CF\")");
			   os.println("pv[pv<=0]<-min(pv[pv>0])");
			   os.println("write(pv, ncolumns=1, file=\"" + tmpFile + ".pv" + "\")");
			   os.flush();
			   os.close();
			   
			   MyFunc.runRscript(tmpFile + ".R");
			   List<Double> p = MyFunc.readDoubleList(tmpFile + ".pv");
		   
			   File f = new File(tmpFile + ".kk");
			   if(!f.delete()){
				   f.deleteOnExit();
		   		}
			   f = new File(tmpFile + ".pp");
			   if(!f.delete()){
				   f.deleteOnExit();
			   	}
			   f = new File(tmpFile + ".R");
			   if(!f.delete()){
				   f.deleteOnExit();
			   	}
			   f = new File(tmpFile + ".pv");
			   if(!f.delete()){
				   f.deleteOnExit();
			   }
			   return p;
		   } catch (Exception e) {
			   	throw  new ArithmeticException();
		   }
	   }
	   
	
	public void perform(){
		System.err.println("calculate scores....");
		calculateScores();
		//System.err.println(scores);
		
		System.err.println("calculate null scores....");
		calculateNullScores();
		//System.err.println(nullScores);
		
		System.err.println("calculate normal statistics....");
		caluculateNormalStatistics();
		System.err.println("");
		System.err.println(normalStatistics);
		
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
		options.addOption("s", "score", true, "score file name");
		HelpFormatter formatter = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
		CommandLine commandLine = null;
		try{
			commandLine = parser.parse(options, args);
		}catch (Exception e) {
			formatter.printHelp(Reflection.getCallerClass( 1 ).getName() + " [options] BAFSegFile LogRSegFile probeTsvFile", options);
			System.exit(1);
		}
		List <String> argList = commandLine.getArgList();
		if(!(argList.size() == 3 | argList.size() == 2)){
			formatter.printHelp(Reflection.getCallerClass( 1 ).getName() + " [options] BAFSegFile LogRSegFile probeTsvFile", options);
			System.exit(1);
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
			int interval = 10000;
			if(commandLine.hasOption("i")){
				interval = Integer.valueOf(commandLine.getOptionValue("i"));
			}
			PI = ProbeInfo.generatePsuedoProbeInfo(interval);
			PI.filter(BAF.get(sample.get(0)));	
		}
		
		SAIRIC5 SAIRIC = new SAIRIC5(BAF,LogR,PI);
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