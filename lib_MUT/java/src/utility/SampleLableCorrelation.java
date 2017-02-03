package utility;

import java.util.*;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import sun.reflect.Reflection;

public class SampleLableCorrelation{
	private MyMat Exp;
	private Map <String, String> sampleLabel;
	private Map <String, Double> corScore;
	private Map <String, Double> Pvalue;
	private Map <String, Double> Qvalue;
	private List <String> genes;
	private int itrForNullDist;
	
	

	private Double PvalueCutoff = null;
	private Double QvalueCutoff = null;	
	private boolean minusLogScale = false;	
	
	
	public void setPvalueCutoff(double d){
		PvalueCutoff = d;	
	}
	public void setQvalueCutoff(double d){
		QvalueCutoff = d;	
	}
	public void outputInMinusLogScale(){
		minusLogScale = true;
	}
	
	private interface CorFunc{
		double get(String rowId);
	}
	private CorFunc corFunc;
	
	//calculate correlation using T statistics
	private class CorFuncForTwoGroups implements CorFunc{
		private String  label1;  
		private String  label2;
		private List <String> group1;
		private List <String> group2;
		public CorFuncForTwoGroups() {
			List <String> tmp = MyFunc.uniq(new ArrayList<String>(sampleLabel.values()));
			Collections.sort(tmp);
			label1 = tmp.get(0);
			label2 = tmp.get(1);
			group1 = new ArrayList<String>();
			group2 = new ArrayList<String>();
			for(Map.Entry<String, String> e: sampleLabel.entrySet()){
				if(e.getValue().equals(label1)){
					group1.add(e.getKey());
				}else{
					if(e.getValue().equals(label2)){
						group2.add(e.getKey());
					}
				}
			}
		}
		public double  get(String rowId){
			List <Double> value1 = new ArrayList<Double>();
			List <Double> value2 = new ArrayList<Double>();
			int i;
			for(i = 0; i < group1.size(); i++){
				value1.add(Exp.get(rowId, group1.get(i)));
			}
			for(i = 0; i < group2.size(); i++){
				value2.add(Exp.get(rowId, group2.get(i)));
			}
			return MyFunc.tStatistic(value1, value2);
		}
	}
    
	//calculate correlation using F statistics in ANOVA test
	private class CorFuncForMultipleGroups implements CorFunc{

		private Map <String, List<String>> group;
		
		public CorFuncForMultipleGroups(){
			group = new HashMap<String, List<String>>();	
			for(Map.Entry<String, String> e: sampleLabel.entrySet()){
				if(group.containsKey(e.getValue())){
					group.get(e.getValue()).add(e.getKey());
				}else{
					List <String> tmp = new ArrayList<String>();
					tmp.add(e.getKey());
					group.put(e.getValue(), tmp);	
					
				}
			}
		}
		
		public double get(String rowId){
			List <List <Double>> X = new ArrayList<List <Double>>();
			for(List <String> s: group.values()){
				List <Double> tmp = new ArrayList<Double>();
				for(String t:s){
					tmp.add(Exp.get(rowId, t));
				}
				X.add(tmp);
			}
			return MyFunc.ANOVAStatistics(X);
		}	
	}
	
	//calculate correlation using Pearson Coefficient
	private class CorFuncForContinuousLabel implements CorFunc{
		private List <String> samples;
		private List <Double> labels;
		public CorFuncForContinuousLabel() {
			List <Double> tmp = new ArrayList<Double>();
			labels = new ArrayList<Double>();
			samples = new ArrayList<String>();
			for(Map.Entry<String, String> e: sampleLabel.entrySet()){
				tmp.add(Double.valueOf(e.getValue()));
				samples.add(e.getKey());
			}
		;
			double m = MyFunc.mean(tmp);
			double sd = MyFunc.sd(tmp);
			for(Double d: tmp){
				labels.add((d - m)/ sd);
			}
		}
		public double get(String rowId){
			List <Double> v = new ArrayList<Double>();
			for(String s: samples){
				v.add(Exp.get(rowId,s));
			}
			double c = MyFunc.pearsonCorrelationForNormarizedList(v, labels);
			if(Double.isNaN(c)){
				//System.err.println(v);
			}
			
			if(c >= 1){
				return Double.MAX_VALUE;
			}else if(c <= -1){
				return -Double.MAX_VALUE;
			}else{	
				return 0.5*Math.log((1+c)/(1-c));
			}	
		}
	}

	public SampleLableCorrelation(MyMat Exp){
		this.Exp = new MyMat(Exp);
		sampleLabel = new HashMap<String, String>();
		genes = new ArrayList<String>(Exp.getRowNames());
		corScore = new HashMap<String, Double>();
		Pvalue = new HashMap<String, Double>();
		Qvalue = new HashMap<String, Double>();
		itrForNullDist = (int)Math.round(100000.0/Exp.rowSize());
	}
	
	public void setItrForNullDist(int i){
		itrForNullDist = i;
	}
	public void setCorFuncForTwoGroups(){
		corFunc = new CorFuncForTwoGroups();
	}
	public void setCorFuncForMultipleGroups(){
		corFunc = new CorFuncForMultipleGroups();
	}
	
	
	public void setCorFuncForContinuousLabel(){
		Exp.normalizeRows();
		corFunc = new CorFuncForContinuousLabel();
	}
	
	public void setSampleLabel(Map <String, String> sampleLabel){
		List <String> sample  =  Exp.getColNames();
		for(String s: sample){
			if(sampleLabel.containsKey(s) && !sampleLabel.get(s).equals("")){
				this.sampleLabel.put(s, sampleLabel.get(s));
			}		
		}
		if(this.sampleLabel.isEmpty()){
			throw new MyException("SampleLabel is empty!");
		}
		Set <String> tmp = new HashSet<String> (this.sampleLabel.values());
		if(MyFunc.canBeDouble(tmp)){
			setCorFuncForContinuousLabel();
			return;
		}
		if(tmp.size() == 2 ){
			setCorFuncForTwoGroups();
		}else{
			setCorFuncForMultipleGroups();
		}
	}
	public void calculateCorScore(){
		if(sampleLabel.isEmpty()){
			throw new MyException("SampleLabel is empty!");
		}
		int i;
		for(i = 0; i < genes.size(); i++){	
			corScore.put(genes.get(i), corFunc.get(genes.get(i)));			
		}
	}
	 
	private  List <Double> getNullDist(){
		List <Double> v = new ArrayList<Double>();
		int i,j;
		for(j=0; j < itrForNullDist; j++){
			Exp.shuffleCols();
			for(i = 0; i < genes.size(); i++){	
			v.add(Math.abs(corFunc.get(genes.get(i))));			
			}
		}
		return v;
	}
	
	public void calculatePvalue(){
		List <Double> nullDist = getNullDist();
		Collections.sort(nullDist);
		Collections.reverse(nullDist);
		double i;
		for( Map.Entry<String, Double> e: corScore.entrySet()){
			for(i=0;i<nullDist.size() && nullDist.get((int)i) > Math.abs(e.getValue());i++){}
			if(i==0){
				i=1;
			}
			double P = i / nullDist.size();
			Pvalue.put(e.getKey(), P);
		}
	}
	
	public void calculatePvalueWithPDB(){
		List <Double> nullDist = getNullDist();
		PvalueCalculatorWithGPD P = new PvalueCalculatorWithGPD(nullDist);
		for( Map.Entry<String, Double> e: corScore.entrySet()){
			Pvalue.put(e.getKey(), P.getPvalue(e.getValue()));
		}
	}
	public void calculateQvalue(){
		Qvalue = MyFunc.calculateStoreyQvalue(Pvalue);
		//Qvalue = MyFunc.calculateQvalue(Pvalue);
	}
	
	
	public Map <String, Double> getPvalue(){
		return Pvalue;
	}
	
	public Map <String, Double> getQvalue(){
		return Qvalue;
	}
	public Map<String, Double> getCorScore(){
		return corScore;
	}
	
	public String toString(){
		StringBuffer S = new StringBuffer("\tCor score\tP value\tQ value\n");
		List<String> keys =  MyFunc.sortKeysByDescendingOrderOfValues(corScore);
		for(String s: keys){
			double p = minusLogScale?-Math.log10(Pvalue.get(s)):Pvalue.get(s);
			if(Double.isInfinite(p)){
				p = Double.MAX_VALUE;
			}
			if(PvalueCutoff != null && (minusLogScale?(p < PvalueCutoff):(p > PvalueCutoff))){
				continue;
			}
			double q = minusLogScale?-Math.log10(Qvalue.get(s)):Qvalue.get(s);	
			if(Double.isInfinite(q)){
				q = Double.MAX_VALUE;
			}
			if(QvalueCutoff != null &&  (minusLogScale?(q < QvalueCutoff):(q > QvalueCutoff))){
				continue;
			}
			List <String>  tmp = new ArrayList<String>();
			tmp.add(s);
			tmp.add(Double.toString(corScore.get(s)));
			tmp.add(Double.toString(p));
			tmp.add(Double.toString(q));
			S.append(MyFunc.join("\t", tmp) + "\n");
		}
		return S.toString();
	}
	
	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.addOption("l", "minuslog", false, "output in minus log scale");
		options.addOption("p", "pcutoff", true, "overlap p-value cutoff");
		options.addOption("q", "pcutoff", true, "overlap q-value cutoff");
		options.addOption("r", "regress", false, "use regression for p-value calculation");
		options.addOption("i", "itrnull", true, "# of iteration for null dist genetation");
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
		if(argList.size() != 2){
			formatter.printHelp(Reflection.getCallerClass( 1 ).getName() + " [options] exp_file sample_lable_file", options);
			return;
		}
		
		SampleLableCorrelation SLC = new SampleLableCorrelation(new MyMat(argList.get(0)));
		SLC.setSampleLabel(MyFunc.readStringStringMap(argList.get(1)));
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
		SLC.calculateCorScore();
		if(commandLine.hasOption("r")){
			SLC.calculatePvalueWithPDB();
		}else{
			SLC.calculatePvalue();
		}
		SLC.calculateQvalue();
		System.out.print(SLC);
	}		
	
}