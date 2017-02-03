package tensor;

import java.util.List;

import processing.core.PApplet;
import processing.core.PFont;
import utility.*;

public class Order3TensorViewer  extends PApplet {

	private static final long serialVersionUID = 1L;

	protected Order3Tensor T;  

	protected String outFile;
	protected boolean autoStop = false;
	
	protected float  profileWidth =  500;
	protected float  profileHeight = 500;
	
	protected int rowLabelFontSize = 10;
	protected int colLabelFontSize = 10;
	protected int sliceLabelFontSize = 20;
	//protected float spaceBetweenRowLablelAndProfile = 10;
	protected float spaceBetweenRowLablelAndProfile = 0;
	//protected float rowLabelSpaceWidth = 100;
	protected float rowLabelSpaceWidth = 0;
	//protected float spaceBetweenColLablelAndProfile = 10;
	protected float spaceBetweenColLablelAndProfile = 0;
	//protected float colLabelSpaceWidth = 100;
	protected float colLabelSpaceWidth = 0;
	
	//protected int marginX = 20;
	//protected int marginY = 20;
	protected int marginX = 0;
	protected int marginY = 0;
	
	protected float boxWidth = 0;
	protected float boxHeight = 0;
	
	
	protected int color1 = MyColor.getRGBhex("BLUE");
	protected int color2 = MyColor.getRGBhex("WHITE");
	protected int color3 = MyColor.getRGBhex("RED");
	
	protected int bgColor =  MyColor.getRGBhex("WHITE");
	
	float max;
	float min;
	
	protected void setProfileWidth(double d){
		profileWidth = (float)d;
	}
	protected void setProfileHeight(double d){
		profileHeight = (float)d;
	}

	protected void setRowLabelFontSize(int i){
		rowLabelFontSize = i;	
	}
	protected void setColLabelFontSize(int i){
		colLabelFontSize = i;	
	}
	protected void setSpaceBetweenRowLablelAndProfile(double d){
		spaceBetweenRowLablelAndProfile  = (float)d;
	}
	protected void setSpaceBetweenColLablelAndProfile(double d){
		spaceBetweenColLablelAndProfile  = (float)d;
	}
	protected void setRowLabelSpaceWidth(double d){
		rowLabelSpaceWidth = (float)d;	
	}
	protected void setColLabelSpaceWidth(double d){
		colLabelSpaceWidth = (float)d;	
	}
		
	protected void setMarginX(int i){
		marginX = i;
	}
	protected void setMarginY(int i){
		marginY = i;
	}
	
	protected void setBoxWidth(double d){
		boxWidth = (float)d;
	}
	protected void setBoxHeight(double d){
		boxHeight = (float)d;
	}
	

	
	public void setProfileColor(String  lowColor, String midColor, String highColor){
		color1 = MyColor.getRGBhex(lowColor);
		color2 = MyColor.getRGBhex(midColor);
		color3 = MyColor.getRGBhex(highColor);	
	}
	
	
	public void setProfileColor(String  lowColor, String highColor){
		color1 = MyColor.getRGBhex(lowColor);
		color2 = PApplet.lerpColor(MyColor.getRGBhex(lowColor), MyColor.getRGBhex(highColor), (float) 0.5, RGB);
		color3 = MyColor.getRGBhex(highColor);	
	}
	
	
	
	public void setBgColor(String  color){
		bgColor = MyColor.getRGBhex(color);
	}
	
	
	
	
	
	protected float profileX1;
	protected float profileX2;
	protected float profileY1;
	protected float profileY2;

	protected float Width;
	protected float Height;
	

	protected int ncol;
	protected int nrow;
	protected int nslice;
	
	//protected PFont font = createFont("Arial",20);
	protected PFont font = createFont("Bitstream Vera Sans",20);
	
	protected boolean colorScalingByRow = false;
	protected boolean colorScalingByCol = false;
	protected boolean colorScalingBySlice = true;
	
	protected int sliceIndex = 0;
	protected int slicedOrder = 3;
	
	
	public Order3TensorViewer(){};
	
	public Order3TensorViewer(Order3Tensor T){
		this.T = T;
		setSlicedOder(3);
		max = (float) T.max();
		min = (float) T.min();
	}	
	
	public void setSlicedOder(int i){
		boxWidth = 0;
		boxHeight = 0;
		if(i == 1){
			slicedOrder = 1;
			nrow = T.getDimOfOrder2();
			ncol = T.getDimOfOrder3();
			nslice = T.getDimOfOrder1();
		}
		if(i == 2){
			slicedOrder = 2;
			nrow = T.getDimOfOrder3();
			ncol = T.getDimOfOrder1();
			nslice = T.getDimOfOrder2();
		}
		if(i == 3){
			slicedOrder = 3;
			nrow = T.getDimOfOrder1();
			ncol = T.getDimOfOrder2();
			nslice = T.getDimOfOrder3();
		}
	}

	protected  void calculateSize(){
		if(boxWidth != 0){
			profileWidth = Math.round(boxWidth*ncol);	
		}
		if(boxHeight != 0){
			profileHeight = Math.round(boxHeight*nrow);
		}
		
		profileX1 = marginX + rowLabelSpaceWidth + spaceBetweenRowLablelAndProfile;  
		profileX2 = profileX1 + profileWidth;
		Width  =  profileX2 + marginX;
		boxWidth = (profileX2-profileX1)/ncol;
		
		profileY1 = marginY + colLabelSpaceWidth + spaceBetweenColLablelAndProfile;
		profileY2 = profileY1 + profileHeight;
		Height =  profileY2  +  + marginY;
		boxHeight = (profileY2-profileY1)/nrow;
		
	}
	
	
	
	public void scaleColorByRow(){
		colorScalingByRow = true;
		colorScalingByCol = false;
	}
	
	public void scaleColorByColumn(){
		colorScalingByRow = false;
		colorScalingByCol = true;
	}
	
	
	public void setOutFile(String outFile){
		this.outFile = outFile;
	}
	
	public void useAutoStop(){
		autoStop = true;
	}

	
	
	public void setup(){
		calculateSize();
		//size(Math.round(Width), Math.round(Height), PDF, "out.pdf");
		size(Math.round(Width), Math.round(Height));
	}
	

	protected void drawSliceLable(){
		textSize(sliceLabelFontSize);
		textAlign(RIGHT, BOTTOM);
		text(getSliceLable(), (float)(marginX + rowLabelSpaceWidth), (float)(marginY + colLabelSpaceWidth));
	}
	
	protected String getSliceLable(){
		String label = "";
		if(slicedOrder == 1){
			label = T.getName1().get(sliceIndex);
		}else if(slicedOrder == 2){
			label = T.getName2().get(sliceIndex);
		}else if(slicedOrder == 3){
			label = T.getName3().get(sliceIndex);
		}
		return label;
	}
	
	
	protected MyMat getSlice(){
		if(slicedOrder == 1){
		return T.getOrder1Slice(sliceIndex);
		}else if(slicedOrder == 2){
			return T.getOrder2Slice(sliceIndex);
		}else if(slicedOrder == 3){
			return T.getOrder3Slice(sliceIndex);
		}else{
			return null;
		}
	}
	
	
	
	public void draw(){
		if(outFile != null){
			beginRecord(PDF, outFile + "."  + sliceIndex + "." + getSliceLable() +  ".pdf");
		}
		textFont(font);
		background(bgColor);
		drawProfile();
		//drawRowlabels();
		//drawCollabels();
		//drawSliceLable();
		if(outFile != null){
			endRecord();
			sliceIndex++;
		}
		if(autoStop & sliceIndex == nslice){
			this.stop();
		}
	}
	
	public void mousePressed(){
		sliceIndex++;
		if(sliceIndex == nslice){
			sliceIndex = 0;
		}	
	}
	
	
	protected  void drawProfile(){
		drawProfile(getSlice());
	}
	
	protected  void drawProfile(MyMat M){
		if(colorScalingByRow){
			for(int i = 0; i < nrow; i++){
				List <Double> row = M.getRow(i);
				max = (float)MyFunc.max(row);
				min = (float)MyFunc.min(row);
				for(int j = 0; j < ncol; j++){
					float p = (float) ((M.get(i, j)-min)/(max-min));
					int color = MyColor.lerpColor(color1 ,color2,color3, p, RGB);
					fill(color);
					noStroke();
					rect((profileX1+j*boxWidth),(profileY1+i*boxHeight),boxWidth,boxHeight);
				}
			}
			return;
		}
		if(colorScalingByCol){
			for(int j = 0; j < ncol; j++){
				List <Double> col = M.getCol(j);
				max = (float)MyFunc.max(col);
				min = (float)MyFunc.min(col);
				for(int i = 0; i < nrow; i++){
					float p = (float) ((M.get(i, j)-min)/(max-min));
					int color = MyColor.lerpColor(color1 ,color2,color3, p, RGB);
					fill(color);
					noStroke();
					rect((profileX1+j*boxWidth),(profileY1+i*boxHeight),boxWidth,boxHeight);
				}
			}
			return;
		}
		//if(colorScalingBySlice){
			//max = (float)MyFunc.max(M.asList());
			//min = (float)MyFunc.min(M.asList());
		//}else{
			//max = (float)MyFunc.max(T.asList());
			//min = (float)MyFunc.min(T.asList());
		//}
		for(int i = 0; i < nrow; i++){
			for(int j = 0; j < ncol; j++){
				float p = (float) ((M.get(i, j)-min)/(max-min));
				int color = MyColor.lerpColor(color1 ,color2,color3, p, RGB);
				fill(color);
				noStroke();
				rect((profileX1+j*boxWidth),(profileY1+i*boxHeight),boxWidth,boxHeight);
			}
		}
	}	
	
	
	protected void calculateRowLabelSpaceWidth(MyMat M){
		textFont(font);
		textSize(rowLabelFontSize);
		float max  =  -(float)Double.MAX_VALUE;
		for(String s: M.getRowNames()){
			float tmp = textWidth(s);
			if(tmp  > max){
				max = tmp;
			}
		}
		rowLabelSpaceWidth = max;
	}
	protected void calculateColLabelSpaceWidth(MyMat M){
		textFont(font);
		textSize(colLabelFontSize);
		float max  =  -(float)Double.MAX_VALUE;
		for(String s: M.getColNames()){
			float tmp = textWidth(s);
			if(tmp  > max){
				max = tmp;
			}
		}
		colLabelSpaceWidth = max;
	}
	
	
	protected void drawRowlabels(){
		drawRowlabels(getSlice());
	}
	
	
	protected void drawRowlabels(MyMat M){
		List <String> rowname = M.getRowNames();
		textSize(rowLabelFontSize);
		textAlign(RIGHT, CENTER);
		fill(0);
		for(int i = 0; i < nrow; i++){
			text(rowname.get(i), marginX + rowLabelSpaceWidth, (float)(profileY1 + (i+0.5)*boxHeight));
		}
	
	}
	
	protected void drawCollabels(){
		drawCollabels(getSlice());
	}
	
	protected void drawCollabels(MyMat M){
		List <String> colname = M.getColNames();
		textSize(colLabelFontSize);
		textAlign(LEFT,CENTER);
		fill(0);
		for(int i = 0; i < ncol; i++){
			float x = (float)(profileX1 + (i+0.5)*boxWidth);
			float y = marginY + colLabelSpaceWidth;
			rotatedText(colname.get(i),x,y, HALF_PI*3);
		}
	}

	protected void rotatedText(String s, float x, float y, float angle){
		translate(x, y);
		rotate(angle);
		text(s, 0, 0);
		rotate(-angle);
		translate(-x, -y);
	}
	
	
	
	
}