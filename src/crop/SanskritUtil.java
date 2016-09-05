package crop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import javax.imageio.ImageIO;

import com.sun.tools.javac.util.List;

import ij.ImagePlus;
import ij.io.FileSaver;
import ij.plugin.ChannelSplitter;
import ij.plugin.filter.Binary;
import ij.plugin.filter.RGBStackSplitter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
// utility functions

public class SanskritUtil {
	// 0. 1D Array process
	public static ArrayList<Integer> findThresInd(int[] data, int len, int thres, int sign, int offset){
		ArrayList<Integer> out = new ArrayList<Integer>();
		for(int i=0;i<len;i++){
			if (data[i]*sign>thres*sign){
				out.add(i+offset);
			}
		}
		return out;
	}
	public static void findThresAssign(int[] data, int len, float thres, int sign, int val){		
		for(int i=0;i<len;i++){
			if (data[i]*sign>thres*sign){
				data[i] = val;
			}
		}		
	}
	public static int[] findMaxDiff(ArrayList<Integer> data){
		int[] out={0,0};
		for(int i=0;i<data.size()-1;i++){
			if (data.get(i+1)-data.get(i)>out[0]){
				out[0] = data.get(i+1)-data.get(i);
				out[1] = i;
			}
		}		
		return out;
	}
	public static int[] makeArr(int st,int lt){
		int[] out = new int[lt-st+1];
		for(int i=0;i<lt-st+1;i++){
			out[i] = st+i;
		}
		return out;
	}
	// 1. 2D matrix operation
	public static float getRegionMean(int[] data, int rowS, int rowE, int colS, int colE, int width, int height, int channel){
		float bg=0;
		int offset;
		switch(channel){
		case 0:		
			for (int y=rowS; y<rowE; y++) {  
				for (int x=colS; x<colE; x++) {  
					bg += (int)(data[y * width + x] & 0xff0000)>>16;
				}  
			}
			break;
		case 1:		
			offset=width*height;
			for (int y=rowS; y<rowE; y++) {  
				for (int x=colS; x<colE; x++) {  
					bg += (int)(data[offset+y * width + x] & 0x00ff00)>>8;
				}  
			}
			break;
		case 2:		
			offset=width*height*2;
			for (int y=rowS; y<rowE; y++) {  
				for (int x=colS; x<colE; x++) {  
					bg += (int)(data[offset+y * width + x] & 0x0000ff);
				}  
			}
			break;
		}		
		bg = bg/((rowE-rowS)*(colE-colS));
		return bg;
	}	
	public static Boolean[] getChannelThres(int[] data, int width, int height, int channel, int thres, Boolean val){
		Boolean[] out = new Boolean[height*width];
		switch(channel){
		case 0:
			for (int y=0; y<height; y++) {  
				for (int x=0; x<width; x++) {  
					if((int)(data[y * width + x] & 0xff0000)>>16>=thres){
						out[y * width + x] = val;
					}else{
						out[y * width + x] = (!val);
					}				
				}
			}
			break;
		case 1:
			for (int y=0; y<height; y++) {  
				for (int x=0; x<width; x++) {  
					if((int)(data[y * width + x] & 0x00ff00)>>8>=thres){
						out[y * width + x] = val;
					}else{
						out[y * width + x] = (!val);
					}				
				}
			}
			break;
		case 2:
			for (int y=0; y<height; y++) {  
				for (int x=0; x<width; x++) {  
					if((int)(data[y * width + x] & 0x0000ff)>=thres){
						out[y * width + x] = val;
					}else{
						out[y * width + x] = (!val);
					}				
				}
			}
			break;
		}

		return out;
	}
	public static Boolean[] resize(Boolean[] data, int width, int height, int ratio){		
		int nwidth= width/ratio;
		int nheight= height/ratio;
		Boolean[] out = new Boolean[nwidth*nheight];
		//System.out.println(nheight+","+nwidth+","+height+","+width);
		for (int y=0; y<nheight; y++) {  
			for (int x=0; x<nwidth; x++) {  
				out[y * nwidth + x] = data[y*ratio * width + x*ratio];
			}  
		}
		return out;
	}	
	public static void threshold(int[] data, float thres, int width, int height, int val){		
		for (int y=0; y<height; y++) {  
			for (int x=0; x<width; x++) {  
				if(data[y * width + x]>=thres){
					data[y * width + x] = val;
				}else{
					data[y * width + x] = 1-val;
				}	        	  
			}  
		}
	}
	public static void thresholdRowMean(Boolean[] data, float thres, int width, int height){		
		for (int y=0; y<height; y++) {  
			int rowSum=0;
			for (int x=0; x<width; x++) {  
				rowSum+=data[y * width + x]?1:0;
			}
			if(rowSum<thres*width){
				for (int x=0; x<width; x++) {  
					data[y * width + x] = false;
				}				
			}	
		}
	}
	public static void fillLine(Boolean[] data, int width, int height, int st,int lt, Boolean val, Boolean doRow){				
		if(doRow){
			for (int y=st; y<lt; y++) {  			
				for (int x=0; x<width; x++) {  
					data[y * width + x] = val;
				}
			}	
		}else{
			for (int y=st; y<lt; y++) {  			
				for (int x=0; x<height; x++) {  
					data[y  + x* width] = val;
				}
			}
		}
	}	
	public static int[] getLineSum(Boolean[] data, int width, int height, int st,int lt, Boolean doRow){		
		int[] lineSum=new int[lt-st]; 
		if(doRow){
			// row sum
			for (int y=st; y<lt; y++) {  
				lineSum[y-st]=0;
				for (int x=0; x<width; x++) {
					if(data[y * width + x]) lineSum[y-st]+=1;
				}
			}
		}else{
			// column sum
			for (int y=st; y<lt; y++) {  
				lineSum[y-st]=0;
				for (int x=0; x<height; x++) {
					if(data[y + x* width]) lineSum[y-st]+=1;
				}
			}
		}
		return lineSum;
	}	



	// find division line
	public static ArrayList<Integer> findDivLine(Boolean[] data, int width, int height, float dLineThres, int dLineHeight, int numErosion, Boolean doRow){		
		// method 1: find black line in middle region
		int lineSt, lineLt, thres;
		if(doRow){
			lineSt = (int) Math.floor(height*5f/11f);
			lineLt = (int) Math.floor(height*6f/11f);
			thres = (int) (dLineThres*width);
		}else{
			lineSt = (int) Math.floor(width*5f/11f);
			lineLt = (int) Math.floor(width*6f/11f);
			thres = (int) (dLineThres*height);
		}

		int rowLen = lineLt-lineSt;
		// 1.1 simplest case: exist seperating black line				
		int[] rowSum = getLineSum(data, width, height, lineSt, lineLt, doRow);
		ArrayList<Integer> mid_ind = findThresInd(rowSum, rowLen, thres, -1 ,lineSt);		
		if (mid_ind.size()<=dLineHeight){ mid_ind.clear();}
		// 1.2 need some erosion

		if(mid_ind.size()==0){
			// erode in a stripe
			//System.out.println("start erosion");
			//SanskritUtil.arrayToSave("db0.jpg",data,width,height);
			Erosion.Erode(data, width, height, lineSt, lineLt, numErosion, doRow);
			//SanskritUtil.arrayToSave("db.jpg",data,width,height);
			rowSum = getLineSum(data, width, height, lineSt, lineLt, doRow);
			mid_ind = findThresInd(rowSum, rowLen, thres, -1 ,lineSt);
			if(mid_ind.size()<=dLineHeight){
				// tilt the angle a bit				
				float[] angles = {-20,-10, 10, 20};
				int centerNum=3;
				//System.out.println("thres: "+thres);
				SlantLineSum.doCenterAngles(data, width, height, lineSt, lineLt, centerNum, angles, 4, doRow);
				int [] slantSum = SlantLineSum.getMin();
				if(slantSum[0]<thres){
					mid_ind.add(slantSum[1]);
				}
			}
		}
		return mid_ind;
	}

	/*
		// method 2: word distribution
		if (mid_line[0]==0){    
			findThresAssign(rowSum,rowLen, 0.1f*width,-1, (int)Math.floor(0.3f*width)+1);
			//letters = find(row<0.7*y & row>0.3*y);
			ArrayList<Integer> letters = findThresInd(rowSum, rowLen, 0.7f*width,-1, 0);			    
			// middle gap
			int[] max_dif = findMaxDiff(letters); //[max_val, ind]
			if (max_dif[0]>min(300,size(tmp,1)*0.3)){
				// curvature estimation (2nd diff)				
				int st=0;int lt=0;
				for (int j=letters.get(max_dif[1]);j<letters.get(max_dif[1]+1);j++){
					if (rowSum[j+1]-rowSum[j]<-0.01*width){
						st=j;
						break;
					}
				}
				for (int j=letters.get(max_dif[1]+1);j>letters.get(max_dif[1]);j--){
					if (rowSum[j]-rowSum[j-1]>-0.01*width){
						lt=j;
						break;
					}
				}
				int[] max_dif = findMaxDiff(rowSum,st,lt); //[max_val, ind]							       
				tmp_ind = floor(3*numel(row)/7):floor(4*numel(rowSum)/7);
				[~,mid_ind]= min(row(tmp_ind));
				mid_ind = tmp_ind(mid_ind)+floor(x/3)-1;
				mid_line =1;
			}			    
			// upper gap
			if (letters.get(letters.size()-1)<0.55f*rowLen){        
				mid_ind = floor(height/3)+max(letters)+50;
				mid_line[0] =1;
			}
			// lower gap
			if (letters.get(0)>0.43*rowLen){
				mid_ind = floor(height/3)+min(letters)-50;
				mid_line[0] =1;        
			}					
		}
	 */

	public static class SlantLineSum{		
		private static int minSum=0;
		private static int minCenter=0;
		private static int minAngleId=0;
		public static int getMinCenter() {return minCenter;}
		public static float getMinAngleId() {return minAngleId;}
		public static int[] getMin() {
			int[] out={minSum,minCenter, minAngleId};
			return out;
		}
		public static int[] doCenterAngles(Boolean[] image, int width, int height, int lineSt, int lineLt, int centerNum, float[] angles, int angleNum, Boolean doRow){
			int [] out = new int[angleNum*centerNum];
			int lineCenter;			
			int cc=0;			
			for(int j=0;j<centerNum; j++){
				lineCenter = (int)(lineSt*(j+1f)/(centerNum+2f)+lineLt*(centerNum+1f-j)/(centerNum+2f));
				for(int i=0;i<angleNum; i++){
					out[cc] = doCenterAngle(image, width, height, lineCenter, angles[i], doRow);
					//System.out.println(lineCenter+"_"+angles[i]+"_"+out[cc]);
					if(out[cc]<minSum){
						minSum = out[cc];
						minCenter = lineCenter;
						minAngleId = i;
					}
					cc++;
				}
			}
			return out;
		}
		public static int doCenterAngle(Boolean[] image, int width, int height, int lineCenter, float angle, Boolean doRow){
			int out=0;
			int halfLine;
			if(doRow){
				halfLine = width/2;
				int cc = lineCenter*width+halfLine;
				out += image[cc]?1:0;
				for(int i=1;i<halfLine;i++){
					out+=image[cc+i-((int)Math.round(i*Math.tan(angle/180)))*width]?1:0;
					out+=image[cc-i+((int)Math.round(i*Math.tan(angle/180)))*width]?1:0;
				}				
				out+=image[cc-halfLine+((int)Math.round(halfLine*Math.tan(angle/180)))*width]?1:0;				
				if(width%2==1){
					out+=image[cc+halfLine-((int)Math.round(halfLine*Math.tan(angle/180)))*width]?1:0;
				}
			}else{
				halfLine = height/2;
				int cc = halfLine*width+lineCenter;
				out += image[cc]?1:0;
				for(int i=1;i<halfLine;i++){
					out+=image[cc+i*width-((int)Math.round(i*Math.tan(angle/180)))]?1:0;
					out+=image[cc-i*width+((int)Math.round(i*Math.tan(angle/180)))]?1:0;
				}				
				out+=image[cc-halfLine*width+((int)Math.round(halfLine*Math.tan(angle/180)))]?1:0;				
				if(width%2==1){
					out+=image[cc+halfLine*width-((int)Math.round(halfLine*Math.tan(angle/180)))]?1:0;
				}				
			}
			return out;
		}			
	}

	public static class Erosion{
		public static void Erode(Boolean[] image, int width, int height, int lineSt, int lineLt, int numErosion, Boolean doRow){
			int [] manDist = getManhattan(image, width, height, lineSt, lineLt, doRow);
			thresManhattan(image, manDist, width, height, lineSt, lineLt, numErosion, doRow);			
		}	

		private static void thresManhattan(Boolean[] image, int[] manDist, int width, int height, int lineSt, int lineLt, int numErosion, Boolean doRow){
			int imageId=0,outId=0;
			int rowSt=0, rowLt=height, colSt=0, colLt=width;			
			if(doRow){
				rowSt = lineSt;
				rowLt = lineLt;
			}else{
				colSt = lineSt;
				colLt = lineLt;
			}
			int outWidth = colLt-colSt;
			for (int i=rowSt; i<rowLt; i++){
				imageId = i*width+colSt;
				outId = (i-rowSt)*outWidth;
				for (int j=colSt; j<colLt; j++){				
					image[imageId] = (manDist[outId]>numErosion);
					outId++;imageId++;
				}
			}		
		}
		private static int[] getManhattan(Boolean[] image,int width, int height, int lineSt, int lineLt, Boolean doRow){
			// traverse from top left to bottom right
			int imageId=0,outId=0;			
			int rowSt=0, rowLt=height, colSt=0, colLt=width;			
			if(doRow){
				rowSt = lineSt;
				rowLt = lineLt;
			}else{
				colSt = lineSt;
				colLt = lineLt;
			}
			int outWidth = colLt-colSt, outHeight = rowLt-rowSt;
			int[] out = new int[outWidth*outHeight];

			for (int i=rowSt; i<rowLt; i++){
				imageId = i*width+colSt;
				outId = (i-rowSt)*outWidth;
				for (int j=colSt; j<colLt; j++){		        	
					if (!image[imageId]){
						// first pass and pixel was on, it gets a zero
						out[outId] = 0;
					} else {
						// pixel was off
						// It is at most the sum of the lengths of the array
						// away from a pixel that is on
						out[outId] = width+height;
						// or one more than the pixel to the north
						if (i>rowSt) out[outId] = Math.min(out[outId], out[outId-outWidth]+1);
						// or one more than the pixel to the west
						if (j>colSt) out[outId] = Math.min(out[outId], out[outId-1]+1);
					}
					outId++;imageId++;
				}
			}
			// traverse from bottom right to top left		    
			for (int i=outHeight-1; i>=0; i--){				
				outId = i*outWidth+outWidth-1;
				for (int j=outWidth-1; j>=0; j--){
					// either what we had on the first pass
					// or one more than the pixel to the south
					if (i+1<outHeight) out[outId] = Math.min(out[outId], out[outId+outWidth]+1);
					// or one more than the pixel to the east
					if (j+1<outWidth) out[outId] = Math.min(out[outId], out[outId+1]+1);
					outId--;		            
				}
			}
			return out;
		}
	}
	// find connected component in image
	// code from: https://courses.cs.washington.edu/courses/cse576/02au/homework/hw3/ConnectComponent.java
	// modification: int[] ->Boolean[] for speed
	public static class ConnectComponent
	{
		final static int MAX_LABELS= 800000;
		static int next_label;
		static int[] maxCount_label={0,0}; //top two
		static int[] maxCount={0,0}; //top two
		/**
		 * label and re-arrange the labels to make the numbers of label continous
		 * @param zeroAsBg Leaving label 0 untouched
		 */
		public static Pair<int[],int[]> bwlabel(Boolean[] image, int[] d)
		{
			// first-pass 
			next_label = 1;
			// System.out.println(d[0]+"---"+d[1]);
			int[] label= labeling(image,d);
			// second-pass
			int[] stat = new int[next_label+1];
			int[] count = new int[next_label+1];
			for (int i=0;i<image.length;i++) {
				if (label[i]>next_label)
					System.err.println("bigger label than next_label found!");
				stat[label[i]]++; // so that ascending order label	            
			}
			stat[0]=0;              // label 0 will be mapped to 0
			// whether 0 is background or not
			// find max segment	        
			int j = 1;
			maxCount[0]=0;maxCount[1]=0;
			for (int i=0; i<stat.length; i++) {
				count[i]=stat[i];
				if (stat[i]!=0) stat[i]=j++;
				if(count[i]>maxCount[0]){
					maxCount_label[1] = maxCount_label[0];
					maxCount[1] = maxCount[0];
					maxCount_label[0] = stat[i];
					maxCount[0] = count[i];
				}else if(count[i]>maxCount[1]){
					maxCount_label[1] = stat[i];
					maxCount[1] = count[i];
				}
			}
			//System.out.println("From "+next_label+" to "+(j-1)+" regions");
			//System.out.println("max label: "+maxCount_label[0]+" ("+maxCount[0]+")");
			next_label= j-1;
			for (int i=0;i<image.length;i++) label[i]= stat[label[i]];

			Pair<int[],int[]> out = new Pair<int[],int[]>(label,count);
			return out;
		}

		/**
		 * return the max label in the labeling process.
		 * the range of labels is [0..max_label]
		 */
		public static int getMaxLabel() {return next_label;}
		public static int[] getMaxcountLabel() {return maxCount_label;}

		/**
		 * Label the connect components
		 * If label 0 is background, then label 0 is untouched;
		 * If not, label 0 may be reassigned
		 * [Requires]
		 *   0 is treated as background
		 * @param image data
		 * @param d dimension of the data
		 * @param zeroAsBg label 0 is treated as background, so be ignored
		 */
		private static int[] labeling(Boolean[] image, int[] d)    
		{
			int w= d[0], h= d[1];
			int[] rst= new int[w*h];
			int[] parent= new int[MAX_LABELS];
			int[] labels= new int[MAX_LABELS];
			// region label starts from 1;
			// this is required as union-find data structure
			int next_region = 1;
			for (int y = 0; y < h; ++y ){
				for (int x = 0; x < w; ++x ){
					if (!image[y*w+x]) continue;
					int k = 0;
					boolean connected = false;
					// if connected to the left
					if (x > 0 && image[y*w+x-1]) {
						k = rst[y*w+x-1];
						connected = true;
					}
					// if connected to the up
					if (y > 0 && image[(y-1)*w+x] &&
							(connected = false || rst[(y-1)*w+x] < k )) {
						k = rst[(y-1)*w+x];
						connected = true;
					}
					if ( !connected ) {
						k = next_region;
						next_region++;
					}
					rst[y*w+x]= k;
					// if connected, but with different label, then do union
					if ( x> 0 && image[y*w+x-1]  && rst[y*w+x-1]!= k )
						uf_union( k, rst[y*w+x-1], parent );
					if ( y> 0 && image[(y-1)*w+x] && rst[(y-1)*w+x]!= k )
						uf_union( k, rst[(y-1)*w+x], parent );
					//System.out.println(x+","+y+","+next_region);
				}
			}

			// Begin the second pass.  Assign the new labels
			// if 0 is reserved for background, then the first available label is 1
			next_label = 1;
			for (int i = 0; i < w*h; i++ ) {
				if (image[i]) {           
					rst[i] = uf_find( rst[i], parent, labels );                
					// The labels are from 1, if label 0 should be considered, then
					// all the label should minus 1					
				}
			}
			next_label--;   // next_label records the max label			

			//System.out.println(next_label+" regions");

			return rst;
		}
		private static void uf_union( int x, int y, int[] parent)
		{
			while ( parent[x]>0 )
				x = parent[x];
			while ( parent[y]>0 )
				y = parent[y];
			if ( x != y ) {
				if (x<y)
					parent[x] = y;
				else parent[y] = x;
			}
		}
		/**
		 * This function is called to return the root label
		 * Returned label starts from 1 because label array is inited to 0 as first
		 * [Effects]
		 *   label array records the new label for every root
		 */
		private static int uf_find( int x, int[] parent, int[] label)
		{
			while ( parent[x]>0 )
				x = parent[x];
			if ( label[x] == 0 )
				label[x] = next_label++;
			return label[x];
		}
	}





	// 2. bounding box process
	public static void resizeBbox(int[] data, int len, float ratio){
		for (int x=0; x<len; x++) {
			data[x] = (int)(data[x]*ratio);
		}
	}
	public static int[] findBbox(int[] data, int width, int height, int label){
		int[] bbox ={height,0,width,0};
		for (int y=0; y<height; y++) {  
			for (int x=0; x<width; x++) {  
				if(data[y * width + x]==label){
					bbox[0] = Math.min(bbox[0],y);
					bbox[1] = Math.max(bbox[1],y);
					bbox[2] = Math.min(bbox[2],x);
					bbox[3] = Math.max(bbox[3],x);
				}
			}  
		}  
		return bbox;
	}
	public static void unoinBbox(int[] bb1, int[] bb2){
		bb1[0] = Math.min(bb1[0],bb2[0]);
		bb1[1] = Math.max(bb1[1],bb2[1]);
		bb1[2] = Math.min(bb1[2],bb2[2]);
		bb1[3] = Math.max(bb1[3],bb2[3]);		
	}



	// 3. result		
	// output xml
	public static int findLastDigit(String filename){					
		int j = filename.length()-1;
		while (j >=0 && Character.isDigit(filename.charAt(j))) j--;
		return j+1;
	}

	public static void outputIm(ImageProcessor ip, String inputName, String outputDir, int[] bb, int numPic) throws IOException{
		String outputName = outputDir+inputName.substring(inputName.lastIndexOf('/'),inputName.lastIndexOf('.'))+"_crop";
		for(int ii=0;ii<numPic;ii++){
			BufferedImage out_im = cropImage(ip, bb[2+ii*4], bb[ii*4], bb[3+ii*4]-bb[2+ii*4]+1, bb[1+ii*4]-bb[ii*4]+1);
			ImageIO.write(out_im, "jpg", new File(outputName+(ii+1)+".jpg"));
			System.out.println("Output image: "+outputName+(ii+1)+".jpg");
		}
	}

	public static void outputXml(String inputName, String outputDir, int[] bbox, int numPic) throws FileNotFoundException{		
		// filename -> create needed strings		
		String urlName = inputName.substring(inputName.lastIndexOf('/')+1);
		String urlNameNoType = urlName.substring(0,urlName.lastIndexOf('.'));
		String pageIdName="";			
		if(urlNameNoType.charAt(urlNameNoType.length()-1)=='r' || urlNameNoType.charAt(urlNameNoType.length()-1)=='v'){			
			pageIdName = urlNameNoType.substring(findLastDigit(urlNameNoType.substring(0,urlNameNoType.length()-2)),urlNameNoType.length()-1);			
			urlNameNoType = urlNameNoType.substring(0,urlNameNoType.lastIndexOf('_'));			
		}else{
			pageIdName = urlNameNoType.substring(findLastDigit(urlNameNoType));
		}
		
		String outputName = outputDir+ urlNameNoType+".xml";

		//System.out.println(outputName+","+urlName+","+pageIdName);
		
		int pageId = Integer.parseInt(pageIdName);
		String[] pageNumber={"",""};
		if(pageId>1){
			pageNumber[0]=(pageId-1)+"v";
			pageNumber[1]=(pageId)+"r";
		}else{
			pageNumber[0]=(pageId)+"r";
			pageNumber[1]=(pageId)+"r";
		}

		// if to append
		//PrintWriter out = new PrintWriter(new FileOutputStream(new File(outputName), true));

		// start to output
		PrintWriter out = new PrintWriter(outputName);		
		out.println("<surface>");
		out.println(" <zones>");
		out.println("   <graphic xml:id=\"b"+pageIdName+"\" url=\""+urlName+"\">");
		if(numPic==1){
			out.println("    <desc>f. " +pageNumber[0]+ "</desc>");
		}else{
			out.println("    <desc>f. " +pageNumber[0]+", f. " +pageNumber[1]+ "</desc>");
		}
		out.println("   </graphic>");
		out.println(" </zones>");
		for(int ii=0;ii<numPic;ii++){
			out.println(" <zones ulx=\""+ bbox[ii*4]+ "\" uly=\""+ bbox[ii*4+2]+ "\" lrx=\""+ bbox[ii*4+1]+ "\" lry=\"" + bbox[ii*4+3]+ "\">");			
			out.println("   <graphic xml:id=\"f" +pageNumber[ii] + "\" url=\"" + urlNameNoType + "_crop" + (ii+1) +".jpg\">");
			out.println("    <desc>f. "+ pageNumber[ii]+ "</desc>");
			out.println("   </graphic>");
			out.println(" </zones>");
		}
		out.println("</surface>");
		out.close();
		System.out.println("Output xml: "+outputName);
	}

	// output image
	public static BufferedImage cropImage(ImageProcessor ip, int uLx, int uLy, int width, int height){
		ip.setRoi(uLx,uLy,width,height);
		ImageProcessor ip2 = ip.crop();	    
		BufferedImage croppedImage = ip2.getBufferedImage();
		return croppedImage;
	}


	// 4. i/o

	public static void arrayToSave(String output, Boolean[] data, int width, int height){
		ImagePlus imp = arrayToGrayImage(data, width, height);  
		FileSaver fs = new FileSaver(imp);
		fs.saveAsBmp(output);   		
	}
	public static ImagePlus arrayToGrayImage(Boolean[] data, int width, int height){
		ImageProcessor ip = new ByteProcessor(width, height);  
		// set each pixel value to whatever, between -128 and 127  
		for (int y=0; y<height; y++) {  
			for (int x=0; x<width; x++) {  
				// Editing pixel at x,y position  
				ip.putPixel(x,y, data[y * width + x]?255:0);
			}  
		}  
		ImagePlus imp = new ImagePlus("", ip);  
		return imp;
	}
	public static void arrayToSave(String output, int[] data, int width, int height){
		ImagePlus imp = arrayToGrayImage(data, width, height);  
		FileSaver fs = new FileSaver(imp);
		fs.saveAsBmp(output);   		
	}
	public static ImagePlus arrayToGrayImage(int[] data, int width, int height){
		ImageProcessor ip = new ByteProcessor(width, height);  
		// set each pixel value to whatever, between -128 and 127  
		for (int y=0; y<height; y++) {  
			for (int x=0; x<width; x++) {  
				ip.putPixel(x,y, data[y * width + x]);
			}  
		}  
		ImagePlus imp = new ImagePlus("", ip);  
		return imp;
	}

	// 5. misc
	public static void printArray(int[] data, int num){		
		for (int y=0; y<num; y++) System.out.print(data[y]+",");
		System.out.println();
	}
	public static void printArrayMaxMin(int[] data, int num){		
		int tmpM=0,tmpm=0;
		for (int y=0; y<num; y++) {  
			tmpM=Math.max(tmpM,data[y]);
			tmpm=Math.min(tmpm,data[y]);
		}
		System.out.println("max: "+tmpM+", min: "+tmpm);
	}
	public static class Pair<U, V> {
		private U first;
		private V second;
		public Pair(U first, V second) {
			this.first = first;
			this.second = second;
		}
		public U getFirst() { return first;}
		public V getSecond() {return second;}
	}
	public static class Triple<U, V, W> {
		private U first;
		private V second;
		private W third;
		public Triple(U first, V second, W third) {
			this.first = first;
			this.second = second;
			this.third = third;
		}
		public U getFirst() { return first;}
		public V getSecond() {return second;}
		public W getThird() {return third;}
	}
	//http://stackoverflow.com/questions/4859261/get-the-indices-of-an-array-after-sorting
	public static class ArrayIndexComparator implements Comparator<Integer>{
		private final Integer[] array;
		private final Integer len;
		public ArrayIndexComparator(int[] inArray,int inLen)
		{
			len = inLen;
			array = new Integer[inLen];
			for (int i = 0; i < len; i++) array[i]=inArray[i];
		}

		public Integer[] createIndexArray(){
			Integer[] indexes = new Integer[len];
			for (int i = 0; i < len; i++) indexes[i] = i;	        
			return indexes;
		}
		@Override
		public int compare(Integer index1, Integer index2){
			// Autounbox from Integer to int to use as array indexes
			return array[index1].compareTo(array[index2]);
		}
	}

	public static Integer[] SortArrIndex(int[] arr,int len){
		SanskritUtil.ArrayIndexComparator comparator = new SanskritUtil.ArrayIndexComparator(arr,len);
		Integer[] indexes = comparator.createIndexArray();
		Arrays.sort(indexes, comparator);
		return indexes;
	}

}