package utility;

import java.util.*;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.cli.*;

import sun.reflect.Reflection;

public class PoibinRNA {
	NormalDistribution ND;
	double mu;
	double sigma;
	double gamma;
	
	public PoibinRNA(){
		ND = new NormalDistribution();
	}
	
	public void setParameters(List <Double> p){
		mu = 0;
		sigma = 0;
		gamma= 0;
		for(double d: p){
			mu += d;
			sigma += d*(1-d);
			gamma += d*(1-d)*(1-2*d);
		}
		sigma = Math.pow(sigma, 0.5);
		gamma /= Math.pow(sigma, 3);
	}
	
	public double getCdf(int k){
		double x = (k + 0.5 -mu)/sigma;
		double c = G(x);
		if(c>1){c=1.0;};
		if(c<0){c=0.0;};
		return c;
	}
	
	private  double G(double x){
		return ND.cumulativeProbability(x) + gamma * (1 - Math.pow(x, 2)) * ND.density(x) /6;
	}

	public double getCdf(int k, List <Double> p){
		setParameters(p);
		return  getCdf(k);
	}
	
	public static void main(String [] args) throws Exception{
		Options options = new Options();
		HelpFormatter formatter = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
		CommandLine commandLine = null;
		
		try{
			commandLine = parser.parse(options, args);
		}catch (Exception e) {
			formatter.printHelp(Reflection.getCallerClass( 1 ).getName() + " k p1 p2 ... ", options);
			System.exit(1);
		}
		List <String> L = commandLine.getArgList();
		if(L.size() < 2){
			formatter.printHelp(Reflection.getCallerClass( 1 ).getName() + " k p1 p2 ...", options);
			System.exit(1);
		}
		
		PoibinRNA PB = new PoibinRNA();
		int k = Integer.valueOf(L.get(0));
		List <Double> p = new ArrayList<Double>();
		for(int i = 1; i < L.size(); i++){
			p.add(Double.valueOf(L.get(i)));
		}
		System.out.println(PB.getCdf(k, p));
	}
}
