package network;
import java.util.*;
import java.util.zip.DataFormatException;
import java.io.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import utility.*;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import sun.reflect.Reflection;


public class Link implements Serializable{
	private static final long serialVersionUID = -3129909979203474489L;
	private boolean L[][];
	private Map <String,Integer> name2index;
	private int n;
	private List <String> name; 
	/*read from sif file*/
	public Link (String infile) throws IOException, DataFormatException{
		name2index = new HashMap<String, Integer>();
		BufferedReader inputStream = new BufferedReader(new FileReader(infile));
		String line;
		Set <String>  name_set = new HashSet<String>();
		String[] tmp = new String[3];
 		while((line = inputStream.readLine()) != null){
			if(line.charAt(0) == '#'){
				continue;				
			}
			 tmp = line.split("\t");
			if(tmp.length != 3){
				throw new DataFormatException("Dist: file format is wrong!");
			}
			name_set.add(tmp[0]);
			name_set.add(tmp[2]);
		}
		name  = new ArrayList<String>(name_set);
		Collections.sort(name);
		int i,j;
		n = name.size();
		for(i=0;i<n;i++){
			name2index.put(name.get(i),i);
		}
		L = new boolean[n][];
		for(i = 0; i < n; i++){
			L[i] = new boolean[i];
		}
		inputStream = new BufferedReader(new FileReader(infile));
		while((line = inputStream.readLine()) != null){
			if(line.charAt(0) == '#'){
				continue;				
			}
			tmp = line.split("\t");
			i = name2index.get(tmp[0]);
			j = name2index.get(tmp[2]);
			if(i > j){
				L[i][j] = true;
			}
			if(j > i){
				L[j][i] = true;
			}
		}
		
	   inputStream.close();
	}
	public List <String> getNodeName(){
		return name;
	}
	
	public Map <String, Integer> getName2index(){
		return name2index;
	}
	
	public void setNodeName(List <String> name){
		this.name= new ArrayList<String>(name);	
		int i, n = name.size();
		name2index.clear();
		for(i=0;i<n;i++){
			name2index.put(name.get(i),i);
		}
		return;
	}
	
	public boolean containsNode(String s){
		return name2index.containsKey(s);
	}
	
	public boolean get(int i, int j){
		if(i >= n || j >= n){
			throw new IndexOutOfBoundsException();
		}
		if(i > j){
			return L[i][j];
		}
		if(i < j){
			return L[j][i];
		}
		return false;
	}
	public boolean get(String s, String t){
		if(name2index.containsKey(s) && name2index.containsKey(t)){
			int i = name2index.get(s);
			int j = name2index.get(t);
			return get(i,j);
		}else{
			return false;
		}
	}
	public void add(String s, String t){
		set(s,t,true);
	}
	public void delete(String s, String t){
		set(s,t,false);
	}
	public void set(String s,String t, boolean b){
		int i = name2index.get(s);
		int j = name2index.get(t);
		if(i > j){
			L[i][j] = b;
		}
		if(i < j){
			L[j][i] = b;
		}
		
	}
	Link (int k){
		n = k;
		L = new boolean[n][];
		int i,j;
		for(i = 0; i < n; i++){
			L[i] = new boolean[i];
		}
		for( i=0;i<n;i++){
			for(j=0;j<i;j++){
				L[i][j] = false;
			}
		}
		name2index = new HashMap<String, Integer>();
		name = new ArrayList<String>();
		for(i=0;i<n;i++){
			name.add( "node" + i+1 );
		}
		for(i=0;i<n;i++){
			name2index.put(name.get(i),i);
		}
	}
	Link (List <String> nodes){
		n=nodes.size();
		L = new boolean[n][];
		int i,j;
		for(i = 0; i < n; i++){
			L[i] = new boolean[i];
		}	
		for(i=0;i<n;i++){
			for(j=0;j<i;j++){
				L[i][j] = false;
		    }
		}
		name2index = new HashMap<String, Integer>();
		name = new ArrayList<String>(nodes);
		for(i=0;i<n;i++){
			name2index.put(name.get(i),i);
		}
	}
	Link(Link link){
	  n = link.n;
	  name2index = new HashMap<String, Integer>(link.name2index);
	  name = new ArrayList<String>(link.name);
	  L = new boolean[n][];
	  int i,j;
	  for(i = 0; i < n; i++){
		 L[i] = new boolean[i];
	  }
	  for(i=0;i<n;i++){
	    for(j=0;j<i;j++){
	      L[i][j] = link.L[i][j];
	    }
	  }
	};	
	public Link multiply(Link link){
		List <String> newName = MyFunc.union(name, link.name);
		Link link2  = new Link(newName);
		
		for(int i = 0, N = newName.size(); i < N; i++){
			for(int j = 0; j < i; j++){
				for(int k = 0; k < N; k++){
					if(get(newName.get(i), newName.get(k)) == true  &&  link.get(newName.get(j), newName.get(k)) == true){
						link2.add(newName.get(i), newName.get(j));
						break;
		    		}
		    	}
		    }
		}
		return link2;
	}
	public static  Link multiply(List <Link> links){
		Link link = links.get(0).add(links.get(1));
		for(int i  = 2; i < links.size(); i++){
		 link = link.multiply(links.get(i));	
		}
		return link;
	}
	public Link add(Link link) {
		List <String> newName = MyFunc.union(name, link.name);
		Link link2  = new Link(newName);
		int i,j;
		for(i = 0; i < newName.size(); i++){
		    for(j = 0; j < i; j++){
		      if(get(newName.get(i),newName.get(j)) == true || link.get(newName.get(i),newName.get(j)) == true){
		    	  link2.add(newName.get(i),newName.get(j));
		      }
		    }
		  }
		  return link2;
	 }
	public static  Link add(List <Link> links){
		Link link = links.get(0).add(links.get(1));
		for(int i  = 2; i < links.size(); i++){
		 link = link.add(links.get(i));	
		}
		return link;
	}
	public List <String> getNeighbors(String node){
		  int i;
		  int j =  name2index.get(node);
		  List <String> neighbor = new ArrayList<String>();
		  for(i = 0; i < j; i++){
		    if(L[j][i] != false){
		      neighbor.add(name.get(i));
		    }
		  }
		  for(i = j+1; i < n; i++){
		    if(L[i][j] != false){
		      neighbor.add(name.get(i));
		    }
		  }
		  return neighbor;
		}
	public boolean havePath(String node1, String node2, int pathlength){
		if(pathlength < 1){
			return false;
		}
		if(pathlength == 1){
			return get(node1, node2);
		}
		Set <String> neighbors = new HashSet<String>(getNeighbors(node1));
		for(int j = 2; j<=pathlength; j++){
			Set <String> newNeighbors = new HashSet<String>();
			for(String s : neighbors){
				newNeighbors.addAll(getNeighbors(s));	
			}
			if(newNeighbors.contains(node2)){
				return true;
			}
			neighbors = newNeighbors;
		}
		return false;
	}
	
	
	public Link getSubLinkontainingTargetAndBridgingNodes(List <String> nodes){
		 List <String> targetNodes = MyFunc.isect(nodes, name);		 
		 List <String> bridgingNodes = new ArrayList <String>();	 
		 for(String s: name){
			 	if(!targetNodes.contains(s)){
			 		int i = 0;
			 		for(String t: targetNodes){
			 			if(get(t,s)){
			 				i++;
			 			}
			 		}
			 		if(i >= 2){
			 			bridgingNodes.add(s);
			 		}		
			 	}
		 }
		targetNodes.addAll(bridgingNodes);	 
		return getSubLink(targetNodes); 
	 }
	
	
	
	public Link getSubLink(List <String> nodes){
		 nodes = MyFunc.isect(nodes, name);
		 Link link = new Link(nodes);
		 for(int i = 0; i < nodes.size(); i++){
			 for(int j = 0; j < i; j++){
				link.set(nodes.get(i), nodes.get(j), get(nodes.get(i), nodes.get(j)));
			 }
		 }
		 return link;
	 }
	public static Link  getLinkFromNodePairList(List <String[]> nodePairs){
		 List <String> nodes = new ArrayList<String>();
		 for(int i = 0; i < nodePairs.size(); i++){
			 nodes.add(nodePairs.get(i)[0]);
			 nodes.add(nodePairs.get(i)[1]);
		 }
		 Link link = new Link(nodes);
		 for(int i = 0; i < nodePairs.size(); i++){
			 link.add(nodePairs.get(i)[0], nodePairs.get(i)[1]);
		 }
		 return link; 
	 }
	public Link getSubLinkContainingTargetNodesAndNeighbors(List <String> targetNodes, int pathLength){
		 List <String> neighbor = new ArrayList<String>(targetNodes);
		  List <String> member = new ArrayList<String>(targetNodes);
		  List < String[] >linkList = new ArrayList<String[]>();
		  int k =0;
		  int i,j;
		  while(k < pathLength){
		    List <String> tmp =new ArrayList<String>();
		    for(i=0;i<neighbor.size();i++){
		      List <String> tmp2 = getNeighbors(neighbor.get(i));
		      for(j=0;j<tmp2.size();j++){
		    	  member.add(tmp2.get(j));
		    	  tmp.add(tmp2.get(j));
		    	  String[]  tmp3  = new String[2];
		    	  tmp3[0] = neighbor.get(i);
		    	  tmp3[1] = tmp2.get(j);
		    	  linkList.add(tmp3);
		      	}
		    }
		    neighbor = tmp;
		    k++;
		  }
		  
		  member = MyFunc.uniq(member);
		  Link subL = new Link(member);
		  for(i=0;i<linkList.size();i++){
		    subL.add((linkList.get(i))[0],(linkList.get(i))[1]);
		  }
		  return subL;
	 }
	public Link getSubLinkContainingNodesithMinDegreeOfLinks(int minDegree){
		 List <String> member = new ArrayList<String>(name);
		 minDegree = Math.max(minDegree, 1);
		 Link subL;
		  while(true){
			 subL = getSubLink(member);
		    List <String> member2 = new ArrayList<String>();;
		    for(int i=0;i<member.size();i++){
		      if(subL.getNeighbors(member.get(i)).size() >= minDegree){
		    	  member2.add(member.get(i));
		      }
		    }
		    if(member.size() == member2.size()){
		      break;
		    }
		    member = member2;
		  }
		  return subL;
	 }
	public void printDataInSifFormat(String outfile, String linkType) throws IOException{
		 PrintWriter os = new PrintWriter(new FileWriter(outfile));
			int i,j;
			for(i=0; i<n; i++){
				for(j=0;j<i;j++){
					if(get(i,j) == true){
						os.println(name.get(i) + "\t"  +  linkType  +  "\t"  + name.get(j));
					}
				}
			}
			os.flush();
			os.close();
	 }
	
	public void printDataInSifFormat(String linkType) throws IOException{
			int i,j;
			for(i=0; i<n; i++){
				for(j=0;j<i;j++){
					if(get(i,j) == true){
						System.out.println(name.get(i) + "\t"  +  linkType  +  "\t"  + name.get(j));
					}
				}
			}
	 }
	
	public UndirectedGraph<String, DefaultEdge> getUndirectedGraph(){
		UndirectedGraph<String, DefaultEdge> g =  new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);
		for(String s: name){
			g.addVertex(s);
		}
		for(String s: name){
			for(String t: name){
				if(s.compareTo(t) > 0){
					g.addEdge(s,t);
				}
			}
		}
		return g;	
	}
	
	public static void main(String [] args) throws Exception{
		Options options = new Options();
		options.addOption("t", "target", true,  "target node file for subnetwork extraction");
		options.addOption("T", "Target", true,  "target nodes separated by ':'");
		options.addOption("l", "pathlength", true,  "path length for subnetwork extraction (default:1)");
		options.addOption("L", "linktype", true, "link type in the sif format (default: pp)");
		options.addOption("m", "mindegree", true, "minimun degree of nodes to be output");
		
		HelpFormatter formatter = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
		CommandLine commandLine;
		try{
			commandLine = parser.parse(options, args);
		}catch (Exception e) {
			formatter.printHelp(Reflection.getCallerClass( 1 ).getName() + " [options]  linkFile", options);
			return ;
		}
		List <String> argList = commandLine.getArgList();
		if(argList.size() != 1){
			formatter.printHelp(Reflection.getCallerClass( 1 ).getName() + " [options] linkFile", options);
			return;
		}
		Link L = new Link(argList.get(0));
		List <String> targetNode = new ArrayList<String>();
		int pathLength = 1;
		String linkType = "pp";
		if(commandLine.hasOption("t")){
			targetNode = MyFunc.readStringList2(commandLine.getOptionValue("t"));
			targetNode = MyFunc.isect(targetNode, L.getNodeName());
		}
		if(commandLine.hasOption("T")){
			targetNode = MyFunc.split(":",(commandLine.getOptionValue("T")));
			targetNode = MyFunc.isect(targetNode, L.getNodeName());
		}
		if(commandLine.hasOption("l")){
			pathLength = Integer.valueOf(commandLine.getOptionValue("l"));
		}
		if(commandLine.hasOption("L")){
			pathLength = Integer.valueOf(commandLine.getOptionValue("l"));
		}
		
		if(targetNode.size() > 1){
			L = L.getSubLinkContainingTargetNodesAndNeighbors(targetNode, pathLength);		
		}
		
		if(commandLine.hasOption("m")){
			L = L.getSubLinkContainingNodesithMinDegreeOfLinks(Integer.valueOf(commandLine.getOptionValue("m")));
		}
		
		L.printDataInSifFormat(linkType);
	
	}

	
	
	
	 
}