package network.old;

import java.util.*;
import java.util.zip.DataFormatException;
import java.io.*;

import network.Link;
import utility.*;

public class ProteinComplexEnrichment {
	private Link link;
	private Map <String, List <String>> geneSetMap;
	private List <String> bgGenes;
	private int pathLength;
	private Map <String, List <String[]> >linkedPair; 
	private Map <String, Double> Zscore;
	private Map <String, Link> subLink;
	private int itrForZscoreCalculation;
	private int pathLengthForSubLink;
	private int minDegreeForSubLink;
	/* read Link data from sif file*/
	public ProteinComplexEnrichment(String infile) throws IOException , DataFormatException {
		link = new Link(infile);
		geneSetMap = new HashMap<String, List<String>>();
		bgGenes = new ArrayList<String>(link.getNodeName());
		pathLength = 1;
		itrForZscoreCalculation = 100;
		pathLengthForSubLink = 1;
		minDegreeForSubLink = 2;
	}
	public void setItrForZscoreCalculation(int i){
		if(i > 30){
			itrForZscoreCalculation = i;
		}
	}
	public void setPathLengthForSubLink(int i){
		if(i > 0){
			pathLengthForSubLink = i;
			
		}
	}
	public void setMinDegreeForSubLink(int i){
		if(i > 0){
			minDegreeForSubLink = 1;
		}
	}
	public void setBgGenes(List <String> Gene){
		bgGenes = new ArrayList<String>(Gene);
		if(!geneSetMap.isEmpty()){
			Map <String, List<String>> tmp = new HashMap<String, List<String>>();	
			for(Map.Entry<String, List<String>> e: geneSetMap.entrySet()){
				List <String> tmp2 = MyFunc.isect(bgGenes, e.getValue());
				if(!tmp2.isEmpty()){
					tmp.put(e.getKey(),tmp2);
				}			
			}
			geneSetMap = tmp;
		}
	}
	public void setGeneSetMap(Map <String, List <String> > geneSetMap){
		if(bgGenes.isEmpty()){
			this.geneSetMap = geneSetMap;
		}else{
			Map <String, List<String>> tmp = new HashMap<String, List<String>>();	
			for(Map.Entry<String, List<String>> e: geneSetMap.entrySet()){
				List <String> tmp2 = MyFunc.isect(bgGenes, e.getValue());
				if(!tmp2.isEmpty()){
					tmp.put(e.getKey(),tmp2);
				}			
			}
			this.geneSetMap = tmp;
		}
	}
	public void increasePathLength(){
		pathLength++;
	}
	public void setPathLength(int k){
		pathLength = k;
	}
	public  List <String[]> getLinkedPair(List <String> genes){
		int i,j;
		List <String[]> linkedPair = new ArrayList<String[]>();
		for(i = 0; i < genes.size(); i++){
			for(j = 0; j < i; j++){
				if(link.havePath(genes.get(i), genes.get(j),pathLength)){
					String[] tmp = new String[2];
					tmp[0] = genes.get(i);
					tmp[1] = genes.get(j);
					linkedPair.add(tmp);	
				}
			}
		}
		return linkedPair;
	}
	public  Map <String, Double> getZscore(){
		return Zscore;
	}
	public  Map <String, Link> getPPI(){
		return subLink;
	}
	
}