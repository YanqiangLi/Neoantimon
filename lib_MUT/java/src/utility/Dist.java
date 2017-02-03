package utility;
import java.util.*;
import java.util.zip.DataFormatException;
import java.io.*;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import tensor.ClusteredOrder3Tensor;


public class Dist implements Serializable{
	private static final long serialVersionUID = -1890276595674423636L;
	private double M[][];
	private Map<String, Integer> name2index;	
	private int n;
	private List<String> name;
	private double diagonalElement = 0;
	public List<String> getNames(){
		return name;
	}
	
	public boolean containsName(String name){
		return name2index.containsKey(name);
	}
	public int size(){
		return n;
	}
	public void setDiagonalElement(double d){
		diagonalElement = d;	
	}
	
	public double get(int i, int j){
		if(i >= n || j >= n ){
			throw new IndexOutOfBoundsException();
		}
		if(i > j){
			return M[i][j];
		}
		if(j > i){
			return M[j][i];
		}
		return diagonalElement;
		
	}
	public double get(String s, String t){
		int i = name2index.get(s);
		int j = name2index.get(t);
		if(i > j){
			return M[i][j];
		}
		if(j > i){
			return M[j][i];
		}
		return diagonalElement;
	}
	public void set(String s, String t, double d){
		int i = name2index.get(s);
		int j = name2index.get(t);
		if(i > j){
			M[i][j] = d;
		}
		if(j > i){
			M[j][i] = d;
		}
	}
	public void set(int i, int j, double d){
		if(i >= n || j >= n){
			throw new IndexOutOfBoundsException();
		}
		if(i > j){
			M[i][j] = d;
		}
		if(j > i){
			M[j][i] = d;
		}
	}
	public Dist(List <String> name){
		n = name.size();
		this.name = new ArrayList<String>(name);
		name2index = new HashMap<String, Integer>();
		int i;
		for(i =0; i< n; i++){
			name2index.put(name.get(i),i);
		}
		M = new double[n][];
		for(i = 0; i < n; i++){
			M[i] = new double[i];
		}
		for(i=0;i<n;i++){
			for(int j=0;j<i;j++){
		      M[i][j] = 0;
		    }	
		}
	}
	
	
	
	
	/*type must be 'e'(euclideanDist), 'c' (pearsonCorrelation),
	 * or 'C' (pearsonCorrelationForNormarizedList)
	 */
	public Dist(MyMat m, char type){
		n = m.rowSize();
		name = new ArrayList<String>(m.getRowNames());
		name2index = new HashMap<String, Integer>();
		int i,j;
		for(i =0; i< n; i++){
			name2index.put(name.get(i),i);
		}
		M = new double[n][];
		for(i = 0; i < n; i++){
			M[i] = new double[i];
		}
		switch(type){
			case 'c':
				MyMat copy_m = new MyMat(m);
				copy_m.normalizeRows();
				for(i = 0; i < n; i++){
					for(j = 0; j < i; j++){
						M[i][j] = MyFunc.pearsonCorrelationForNormarizedList(copy_m.getRow(i),copy_m.getRow(j));
				    }
				}
				setDiagonalElement(1);
				break;
			case 'C':
				for(i = 0; i < n; i++){
					for(j = 0; j < i; j++){
						M[i][j] = MyFunc.pearsonCorrelationForNormarizedList(m.getRow(i),m.getRow(j));
				    }
				}
				setDiagonalElement(1);
				break;
			case 'e':
				for(i = 0; i < n; i++){
					for(j = 0; j < i; j++){
						M[i][j] = MyFunc.euclideanDist(m.getRow(i),m.getRow(j));
				    }
				}
				break;
			default:
				throw new IllegalArgumentException("type must be 'c','C', or 'e'");
		}
	}
	
	
	

	public Dist(String infile) throws IOException, DataFormatException{
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
			name_set.add(tmp[1]);
		}
		name  = new ArrayList<String>(name_set);
		Collections.sort(name);
		int i,j;
		n = name.size();
		for(i=0;i<n;i++){
			name2index.put(name.get(i),i);
		}
		M = new double[n][];
		boolean seen[][] = new boolean[n][];
		for(i = 0; i < n; i++){
			M[i] = new double[i];
			seen[i] = new boolean[i];
			
		}
		inputStream = new BufferedReader(new FileReader(infile));
		while((line = inputStream.readLine()) != null){
			if(line.charAt(0) == '#'){
				continue;				
			}
			tmp = line.split("\t");
			i = name2index.get(tmp[0]);
			j = name2index.get(tmp[1]);
			if(i > j){
				M[i][j] = (Double.valueOf(tmp[2]));
				seen[i][j] = true;
			}
			if(j > i){
				M[j][i] = (Double.valueOf(tmp[2]));
				seen[j][i] = true;
			}
		}
		for(i=0;i<n;i++){
		    for(j=0;j<i;j++){
		      if(seen[i][j] == false){
		    	  throw new DataFormatException("Dist: file format is wrong!");
		      }
		    }
		  }
	   inputStream.close();
	}
	 
	public Dist(Dist D){
	  n = D.n;
	  name2index = new HashMap<String, Integer>(D.name2index);
	  name = new ArrayList<String>(D.name);
	  int i,j;
	  for(i = 0; i < n; i++){
			M[i] = new double[i];
	  }
	  for(i=0;i<n;i++){
	    for(j=0;j<i;j++){
	      M[i][j] = D.M[i][j];
	    }
	  }
	}
	
	public List<Double> asList(){
		List <Double> v = new ArrayList<Double>();
		int i,j;
		for(i=0;i<n;i++){
			for(j=0;j<i;j++){
				v.add(M[i][j]);
			}
		}	
		return v;
	}
	
	public void print(String outfile) throws IOException {
		PrintWriter os = new PrintWriter(new FileWriter(outfile));
		int i,j;
		for(i=0; i<n; i++){
			for(j=0;j<i;j++){
				os.println(name.get(i) + "\t" + name.get(j) + "\t" + get(i,j));
			}
		}
		os.close();
	}
	
	public void print() throws IOException {
		PrintWriter os = new PrintWriter(System.out);
		int i,j;
		for(i=0; i<n; i++){
			for(j=0;j<i;j++){
				os.println(name.get(i) + "\t" +  name.get(j) + "\t" + get(i,j));
			}
		}
		os.close();
	}
	public String toString(){
		StringBuffer S = new StringBuffer();
		int i,j;
		for(i=0; i<n; i++){
			for(j=0;j<i;j++){
				S.append(name.get(i) + "\t" +  name.get(j) + "\t" + get(i,j) + "\n");
			}
		}
		return S.toString();
	}
	
	public String toStringInTabFormat(){
		StringBuffer S = new StringBuffer();
		int i,j;
		for(i=0; i<n; i++){
			S.append("\t" + name.get(i));
		}
		S.append("\n");
		for(i=0; i<n; i++){
			S.append(name.get(i));
			for(j=0;j<n;j++){
				S.append("\t" + get(i,j));
			}
			S.append("\n");
		}
		return S.toString();
	}
	
	public Dist getSubDist(List <String> names){
		Dist D = new Dist(names);
		for(String s: names){
			for(String t: names){
				if(s.compareTo(t) > 0){
					D.set(s,t,get(s,t));
				}
			}
		}
		return D;
	}
	
	public void printAsBinary(String outfile) throws FileNotFoundException, IOException{
		ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(outfile)));
		out.writeObject(this);
		out.close();
	}
	public static Dist readFromBinary(String infile) throws FileNotFoundException, IOException, ClassNotFoundException{
		ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(infile)));
		Dist D =  (Dist)in.readObject();
		in.close();
		return D;
	}
	public static void main(String [] args) throws Exception{
		Options options = new Options();
		options.addOption("c", "cor", false,  "correlation");
		options.addOption("b", "bin", true,  "write binary");
		HelpFormatter formatter = new HelpFormatter();
		CommandLineParser parser = new BasicParser();
		CommandLine commandLine;
		try{
			commandLine = parser.parse(options, args);
		}catch (Exception e) {
			formatter.printHelp("Dist [options] expfile", options);
			return;
		}
		List <String> argList = commandLine.getArgList();
		if(argList.size() != 1){
			formatter.printHelp("Dist [options] expfile", options);
			return;
		}
		Dist D;
		if(commandLine.hasOption("c")){
			D = new Dist(new MyMat(argList.get(0)), 'c');	
			
		}else{
			D = new Dist(new MyMat(argList.get(0)), 'e');	
		}
		if(commandLine.hasOption("b")){
			D.printAsBinary(commandLine.getOptionValue("b"));
		}else{
			D.print();
		}
	}

	

	
	
	
	
	
	
}