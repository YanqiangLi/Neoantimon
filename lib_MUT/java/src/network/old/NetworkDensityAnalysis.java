package network.old;

import java.util.*;

import network.MySimpleGraph;

import utility.*;

public class NetworkDensityAnalysis {
	protected  MySimpleGraph network;
	protected Map <String, List<String>> geneSets;
	protected  List <String> allGenes;
	protected  Map <String, Double> Zscores;
	protected int itrForZscoreCalculation = 100;
	public NetworkDensityAnalysis(MySimpleGraph G){
		network = new MySimpleGraph(G);
		geneSets = new HashMap<String, List<String>>();
		allGenes = new ArrayList<String>(network.vertexSet());
		Zscores = new HashMap<String, Double>(Zscores);
	}
	public void setGeneSets(Map<String, List<String>> geneSets){
 		for(String s: geneSets.keySet()){
			List <String> tmp = new ArrayList<String>(geneSets.get(s));
			this.geneSets.put(s,MyFunc.isect(tmp,allGenes));
		}
	}
	public interface DensityFunction{
		int get(List <String> genes);
	}
	private int pathLengthCutoff = 3;
	DensityFunction densityFunction = new DensityFunction(){
		public int get(List <String> genes){
			int score = 0;
			for(String s: genes){
				int tmp = 0;
				for(String t: genes){
					if(network.getDistanceBetweenTowNodes(s, t) <= pathLengthCutoff){
						tmp++;
					}
				}
				if(tmp > score){
					score = tmp;
				}
			}
			return score;
		}
	};
	
	public void calculateZscores(){
		for(String s: geneSets.keySet()){
			List <String> genes = geneSets.get(s);
			List <Double> nullDist = new ArrayList<Double>();
			for(int i=0; i< itrForZscoreCalculation; i++){ 
				nullDist.add((double)densityFunction.get(MyFunc.sample(allGenes, genes.size())));
			}
			double nullMean = MyFunc.mean(nullDist);
			double nullSd = MyFunc.sd(nullDist);
			double z = (densityFunction.get(genes) - nullMean)/nullSd;
			Zscores.put(s, z);
		}
	}
	
	public Map <String, Double> getZscores(){
		return Zscores;
	}
	
	
	
	
}