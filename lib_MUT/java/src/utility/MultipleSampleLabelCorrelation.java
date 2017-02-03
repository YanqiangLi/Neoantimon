package utility;

import java.util.*;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import sun.reflect.Reflection;

public class MultipleSampleLabelCorrelation {
	private MyMat expressionProfiles;
	private StringMat sampleLabels;
	
	private MyMat Pvalue;
	private MyMat Qvalue;
	private MyMat corScore;
	
	private boolean pvalueWithGPD = false;
	private Integer itrForNullDist = null;
	private Double PvalueCutoff = null;
	private Double QvalueCutoff = null;	
	private boolean minusLogScale = false;		
	
	private boolean supressPcalculation  = false;
	
	
	public MultipleSampleLabelCorrelation(MyMat expressionProfiles, StringMat sampleLabels){
		if(!MyFunc.isect(expressionProfiles.getColNames(), sampleLabels.getColNames()).isEmpty()){
			this.expressionProfiles = expressionProfiles;
			this.sampleLabels = sampleLabels;
		}
		else if(!MyFunc.isect(expressionProfiles.getRowNames(), sampleLabels.getColNames()).isEmpty()){
			this.expressionProfiles = new MyMat(expressionProfiles);
			this.expressionProfiles.transpose();
			this.sampleLabels = sampleLabels;
		}
		else if(!MyFunc.isect(expressionProfiles.getColNames(), sampleLabels.getRowNames()).isEmpty()){
			this.expressionProfiles = expressionProfiles;
			this.sampleLabels = new StringMat(sampleLabels);
			this.sampleLabels.transpose();
		}
		else if(!MyFunc.isect(expressionProfiles.getRowNames(), sampleLabels.getRowNames()).isEmpty()){
			this.expressionProfiles = new MyMat(expressionProfiles);
			this.expressionProfiles.transpose();
			this.sampleLabels = new StringMat(sampleLabels);
			this.sampleLabels.transpose();
		}
		else {
			throw new MyException("expressionProfile and sampleLabels must have common rows or columns!");	
		}
		Pvalue = new MyMat(this.expressionProfiles.getRowNames(), this.sampleLabels.getRowNames());
		Qvalue = new MyMat(this.expressionProfiles.getRowNames(), this.sampleLabels.getRowNames());	
		corScore = new MyMat(this.expressionProfiles.getRowNames(), this.sampleLabels.getRowNames());	
	}
	
	
	
	public void setPvalueCutoff(double d){
		PvalueCutoff = d;	
	}
	public void setQvalueCutoff(double d){
		QvalueCutoff = d;	
	}
	public void outputInMinusLogScale(){
		minusLogScale = true;
	}
	
	public void useGPDforPvalueCalculation(){
		pvalueWithGPD = true;
	}
	
	public void setItrForNullDist(int i){
		itrForNullDist = i;
	}
	
	public void calculate(){
		for(String sampleLabelId: sampleLabels.getRowNames()){
			Map <String, String> sampleLabelMap = sampleLabels.getRowMap(sampleLabelId);
			SampleLableCorrelation SLC = new SampleLableCorrelation(expressionProfiles);
			if(itrForNullDist!=null){
				SLC.setItrForNullDist(itrForNullDist);
			}
			try{
			SLC.setSampleLabel(sampleLabelMap);
			} catch (Exception e){
				continue;
			}
			SLC.calculateCorScore();
			Map <String, Double> corScoreMap = SLC.getCorScore();
			for(Map.Entry<String, Double> e: corScoreMap.entrySet()){
				corScore.set(e.getKey(), sampleLabelId, e.getValue());
			}
			if(supressPcalculation){
				continue;
			}
			if(pvalueWithGPD){
				SLC.calculatePvalueWithPDB();
			}else{
				SLC.calculatePvalue();
			}
			Map <String, Double> PvalueMap = SLC.getPvalue();
			for(Map.Entry<String, Double> e: PvalueMap.entrySet()){
				Pvalue.set(e.getKey(), sampleLabelId, e.getValue());
			}
			SLC.calculateQvalue();
			/*
			Map <String, Double> QvalueMap = SLC.getQvalue();
			for(Map.Entry<String, Double> e: QvalueMap.entrySet()){
				Qvalue.set(e.getKey(), sampleLabelId, e.getValue());
			}
			*/
		}
		if(!supressPcalculation){
			Map <String, Double> PvalueMap = Pvalue.asMap();
			Map <String, Double> QvalueMap = MyFunc.calculateStoreyQvalue(PvalueMap);
			//Map <String, Double> QvalueMap = MyFunc.calculateQvalue(PvalueMap);
			for(Map.Entry<String, Double> e: QvalueMap.entrySet()){
				List <String> tmp = MyFunc.split("\t", e.getKey());
				
				Qvalue.set(tmp.get(0), tmp.get(1), e.getValue());
			}
		}
	}
	
	
	
	private void chatch() {
		// TODO Auto-generated method stub
		
	}



	public String toString(){
		StringBuffer S = new StringBuffer("expression profile\tsample label\tCor score\tP value\tQ value\n");
		Map <String, Double> PvalueMap = Pvalue.asMap();
		Map <String, Double> QvalueMap = Qvalue.asMap();
		Map <String, Double> corScoreMap = corScore.asMap();
		List<String> keys =  MyFunc.sortKeysByAscendingOrderOfValues(PvalueMap);
		for(String s: keys){
			double p; 
			if(minusLogScale){
				p = (PvalueMap.get(s)==0)?Double.MAX_VALUE: -Math.log10(PvalueMap.get(s));
			}else{
				p = PvalueMap.get(s);
			}
			if(PvalueCutoff != null && (minusLogScale?(p < PvalueCutoff):(p > PvalueCutoff))){
				continue;
			}
			double q; 
			if(minusLogScale){
				q = (QvalueMap.get(s)==0)?Double.MAX_VALUE: -Math.log10(QvalueMap.get(s));
			}else{
				q = QvalueMap.get(s);
			}
			if(QvalueCutoff != null &&  (minusLogScale?(q < QvalueCutoff):(q > QvalueCutoff))){
				continue;
			}
			List <String>  tmp = new ArrayList<String>();			
			tmp.add(s);
			tmp.add(Double.toString(corScoreMap.get(s)));
			tmp.add(Double.toString(p));
			tmp.add(Double.toString(q));
			S.append(MyFunc.join("\t", tmp) + "\n");
		}
		return S.toString();
	}
	
	
	public MyMat getCorScoreMatrix(){
			return corScore;
	}
	
	
	public MyMat getPvalueMatrix(){
		if(minusLogScale){
			MyMat tmp = new MyMat(Pvalue.getRowNames(), Pvalue.getColNames());
			for(String s: Pvalue.getRowNames()){
				for(String t: Pvalue.getColNames()){
					tmp.set(s, t,-Math.log10(Pvalue.get(s, t)));
				}
			}
			return tmp;
		}else{
			return Pvalue;
		}
	}
	
	public MyMat getQvalueMatrix(){
		if(minusLogScale){
			MyMat tmp = new MyMat(Qvalue.getRowNames(), Qvalue.getColNames());
			for(String s: Qvalue.getRowNames()){
				for(String t: Qvalue.getColNames()){
					tmp.set(s, t,-Math.log10(Qvalue.get(s, t)));
				}
			}
			return tmp;			
		}else{
			return Qvalue;
		}
	}
	
	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.addOption("l", "minuslog", false, "output in minus log scale");
		options.addOption("p", "pcutoff", true, "p-value cutoff");
		options.addOption("q", "pcutoff", true, "q-value cutoff");
		options.addOption("g", "gpd", false, "use GPD for p-value calculation");
		options.addOption("i", "itrnull", true, "# of iteration for null dist genetation");
		options.addOption("P", "pmat", false, "get p-value matrix");
		options.addOption("Q", "qmat", false, "get q-value matrix");
		options.addOption("c", "cmat", false, "get correlation score matrix");
		HelpFormatter formatter = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
		
		
		CommandLine commandLine;
		try{
			commandLine = parser.parse(options, args);
		}catch (Exception e) {
			formatter.printHelp(Reflection.getCallerClass( 1 ).getName() + " [options] exp_file sample_label_file", options);
			return ;
		}
		List <String> argList = commandLine.getArgList();
		if(argList.size() != 2 ){
			formatter.printHelp(Reflection.getCallerClass( 1 ).getName() + " [options] exp_file sample_lable_file", options);
			return;
		}
		
		MultipleSampleLabelCorrelation SLC= new MultipleSampleLabelCorrelation(new MyMat(argList.get(0)), new StringMat(argList.get(1)));
		
		if(commandLine.hasOption("l")){
			SLC.outputInMinusLogScale();
		}
		if(commandLine.hasOption("p")){
			SLC.setPvalueCutoff(Double.valueOf(commandLine.getOptionValue("p")));
		}
		if(commandLine.hasOption("q")){
			SLC.setQvalueCutoff(Double.valueOf(commandLine.getOptionValue("q")));
		}
		if(commandLine.hasOption("i")){
			SLC.setItrForNullDist(Integer.valueOf(commandLine.getOptionValue("i")));
		}
		if(commandLine.hasOption("g")){
			SLC.useGPDforPvalueCalculation();
		}
		
		if(commandLine.hasOption("c")){
			SLC.supressPcalculation = true;
			SLC.calculate();
			System.out.print(SLC.getCorScoreMatrix());
		}else{
			SLC.calculate();
			if(commandLine.hasOption("P")){
				System.out.print(SLC.getPvalueMatrix());
			}else{
				if(commandLine.hasOption("Q")){
					System.out.print(SLC.getQvalueMatrix());
				}else{
					System.out.print(SLC);
				}
			}
		}
	}		
	
	
	
	
}