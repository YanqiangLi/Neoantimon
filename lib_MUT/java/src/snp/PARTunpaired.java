package snp;


import java.util.*;
import java.io.*;
import org.apache.commons.cli.*;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.*;

import sun.reflect.Reflection;
import utility.MyFunc;
import utility.MyMat;


public class PARTunpaired {
	protected SegmentContainerMap  BAF;
	protected SegmentContainerMap  BAF2;

	protected ProbeInfo probeInfo;
	protected List<String>sampleList; 
	
	
	protected Map<String, Double> scores;
	protected Map<String, Double> pvalues;
	protected Map<String, Double> qvalues;
	
	
	protected Set<String> probeSet;
	protected Double PnFilterCutoff = null;
	
	protected MyMat Po_ps;
	protected Map<String,Double> Po_s;//for each sample 
	protected Map<String,Double> Pt_s;//for each sample 
	protected Map<String,Double> Pn_p;//for each probe
	protected double Pn;
	
	protected double AICutoff =  0.55;
	boolean countLess = false;

	public PARTunpaired(SegmentContainerMap baf, SegmentContainerMap baf2,  ProbeInfo pi) throws IOException{
		BAF = baf;
		BAF2 = baf2;
		probeInfo = pi; 
		probeSet = new LinkedHashSet<String>(probeInfo.getProbeSet());
		sampleList = new ArrayList<String>(sampleSet());
	}
	
	protected Set<String> sampleSet(){
		return BAF.keySet();
	}
	protected Set<String> sampleSet2(){
		return BAF2.keySet();
	}
	
	protected Set<String> probeSet(){
		return probeSet;
	}
	
	protected void calculateScoresAndParameters(){
		Pn_p = new HashMap<String, Double>();
		for(String s: probeSet()){
			Pn_p.put(s,0.0);
		}
		for(String s: sampleSet2()){
			SegmentContainer bafSC = BAF2.get(s);
			SegmentContainer.SegmentIterator bafSCitr  = bafSC.iterator();
			Segment S = bafSCitr.next();
			for(String p: probeSet()){
				int chr = probeInfo.chr(p);
				int pos = probeInfo.pos(p);
				while(chr > S.chr()){
					S = bafSCitr.next();
				}
				while(pos > S.end()){
					S = bafSCitr.next();
				}
				
				double baf = S.value();
				
				if((countLess & baf<= AICutoff)| (!countLess & baf>= AICutoff)){
					Pn_p.put(p, Pn_p.get(p) + 1);
				}
			}
		}
		for(String s: probeSet()){
			Pn_p.put(s, Pn_p.get(s)/sampleSet2().size());
		}
		
		
		if(PnFilterCutoff != null){
			Set<String> tmp = new LinkedHashSet<String>(probeSet());
			for(String s: tmp){
				if(Pn_p.get(s) > PnFilterCutoff){
					probeSet().remove(s);	
					Pn_p.remove(s);
				}
			}
		}
		
		
		
		Pn =  MyFunc.mean(new ArrayList<Double>(Pn_p.values()));

		
		scores =  new LinkedHashMap<String, Double>();
		for(String p: probeSet()){
			scores.put(p,0.0);
		}
		Po_s = new HashMap<String, Double>();
		for(String s: sampleSet()){
			Po_s.put(s,0.0);
		}
		for(String s: sampleSet()){
			SegmentContainer bafSC = BAF.get(s);
			SegmentContainer.SegmentIterator bafSCitr  = bafSC.iterator();
			Segment S = bafSCitr.next();
			for(String p: probeSet()){
				int chr = probeInfo.chr(p);
				int pos = probeInfo.pos(p);
				while(chr > S.chr()){
					S = bafSCitr.next();
				}
				while(pos > S.end()){
					S = bafSCitr.next();
				}
				
				double baf = S.value();
				
				if((countLess & baf<= AICutoff)| (!countLess & baf>= AICutoff)){
					scores.put(p, scores.get(p)+1);
					Po_s.put(s, Po_s.get(s) + 1);
				}
			}
		}
		for(String s: sampleSet()){
			Po_s.put(s, Po_s.get(s)/probeSet().size());
		}
		
		
		Pt_s = new HashMap<String, Double>();
		for(String s: sampleSet()){
			double tmp = (Po_s.get(s) -Pn)/(1-Pn);
			if(tmp>0){
				Pt_s.put(s, tmp);
			}else{
				Pt_s.put(s, 0.0);
			}
		}
		
		//System.err.println(Pn);
		//System.err.println(Pn_p);
		//System.err.println(Pt_s);
		//System.err.println(Pt_s);
		//System.exit(1);
		
		Po_ps = new MyMat(new ArrayList<String>(probeSet()), new ArrayList<String>(sampleSet()));
		for(String s: sampleSet()){
			for(String p: probeSet()){
				double tmp = Pt_s.get(s) + Pn_p.get(p) * (1-Pt_s.get(s));
				Po_ps.set(p, s, tmp);
			}
		}
	}
	
	
	public static List<Double> getPoissonBinomialPvalue(List<Double> kk, MyMat pp){
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
			   
			   for(int i = 0, n = pp.rowSize(); i < n; i++){
				   os.println(MyFunc.join("\t", pp.getRow(i)));
			   }
			   os.flush();
			   os.close();
			  
			   os = new PrintWriter(new FileWriter(tmpFile + ".R"));
			   os.println("kk<-scan(\"" +  tmpFile + ".kk" + "\")");
			   os.println("pp<-read.table(\"" +  tmpFile + ".pp" + "\")");
			   os.println("library(poibin)");
			   os.println("PPOIBIN<-function(x){ppoibin(kk=x[1], pp=x[2:length(x)], method = \"DFT-CF\")}");
			   os.println("pv<-1-apply(cbind(kk,pp),1,PPOIBIN)");
			   os.println("pv[pv<=0]<-min(pv[pv>0])");
			   os.println("pv[pv>=1]<-1");
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
	protected  void calculatePvalues(){
		List<Double> pv = getPoissonBinomialPvalue(new ArrayList<Double>(scores.values()), Po_ps);
		pvalues =  new HashMap<String, Double>();
		List <String> probe = new ArrayList<String>(probeSet()); 
		for(int i = 0; i < probe.size(); i++){
			pvalues.put(probe.get(i),pv.get(i));
		}		
	}
	
	protected  void calculateQvalues(){
		qvalues = MyFunc.calculateQvalue(pvalues);
		for(String p: qvalues.keySet()){ 
			qvalues.put(p, qvalues.get(p)>1?1:qvalues.get(p));
		}
	}
	public void perform(){
		System.err.println("calculate scores....");
		calculateScoresAndParameters();
		
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
		for(String p: probeSet){
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
		for(String p: probeSet){
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
		options.addOption("p", "pncutoff", true, "cutoff for Pn parameter");
		options.addOption("o", "outfile", true, "output file name");
		options.addOption("t", "thinpi", true, "thin down probe info");
		options.addOption("n", "nump", true, "# of psuedo probes");
		options.addOption("l", "less", false, "count less than cutoff");
		HelpFormatter formatter = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
		CommandLine commandLine = null;
		try{
			commandLine = parser.parse(options, args);
		}catch (Exception e) {
			formatter.printHelp(Reflection.getCallerClass( 1 ).getName() + " [options] SegFile SegFile2 probeTsvFile", options);
			System.exit(1);
		}
		List <String> argList = commandLine.getArgList();
		if(!(argList.size() == 2 | argList.size() == 3)){
			formatter.printHelp(Reflection.getCallerClass( 1 ).getName() + " [options] SegFile SegFile2 probeTsvFile", options);
			System.exit(1);
		}
		SegmentContainerMap BAF = new SegmentContainerMap(argList.get(0));
		SegmentContainerMap BAF2 = new SegmentContainerMap(argList.get(1));
		ProbeInfo PI;
		List<String> sample = new ArrayList<String>(BAF.keySet());
		if(argList.size() == 3 ){
			PI = 	ProbeInfo.getProbeInfoFromTsvFile(argList.get(2));
			if(commandLine.hasOption("t")){
				PI.thinDown(Integer.valueOf(commandLine.getOptionValue("t")));
			}
			PI.filter(BAF.get(sample.get(0)));
		}else{
			int n = 1000;
			if(commandLine.hasOption("n")){
				n = Integer.valueOf(commandLine.getOptionValue("n"));
			}
			PI = BAF.generatePsuedoProbeInfo(n);	
		}
		
		PARTunpaired PART = new PARTunpaired(BAF,BAF2,PI);
		if(commandLine.hasOption("c")){
			PART.AICutoff = Double.valueOf(commandLine.getOptionValue("c"));
		}
		if(commandLine.hasOption("p")){
			PART.PnFilterCutoff = Double.valueOf(commandLine.getOptionValue("p"));
		}
		if(commandLine.hasOption("l")){
			PART.countLess = true;
		}
		PART.perform();
		
		Writer os;
		if(commandLine.hasOption("o")){
			 os = new BufferedWriter(new FileWriter(commandLine.getOptionValue("o")));	 
		}else{
			os = new PrintWriter(System.out);
		}
			os.write("Probe" + "\t" +  "Chrom" + "\t" +  "BasePair"  + "\t" + "score" + "\t" + "Pn_p" + "\t" + "pvalue" + "\t" + "qvalue" + "\n");
		for(String s: PART.probeSet()){
			os.write(s + "\t" +  PI.chr(s) + "\t" +  PI.pos(s) + "\t" + PART.getMinusLogPvalues().get(s) + "\t" + PART.getMinusLogQvalues().get(s) + "\n");			
			//os.write(s + "\t" +  PI.chr(s) + "\t" +  PI.pos(s) + "\t" + PART.scores.get(s)/PART.sampleList.size() + "\t" + PART.Pn_p.get(s) + "\t" + PART.getMinusLogPvalues().get(s) + "\t" + PART.getMinusLogQvalues().get(s)+ "\n");			
		}
		os.flush();
	}
	
	
	
}