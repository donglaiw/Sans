package crop;
import crop.SanskritUtil;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.plugin.ContrastEnhancer;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageConverter;

// good references for ImageJ library
// https://imagej.nih.gov/ij/download/docs/tutorial171.pdf
// http://albert.rierol.net/imagej_programming_tutorials.html
public class SanskritCrop {
	private Boolean mVerbose=true;
	// for cropType 
	private float mBlackBackground=100;
	private int mBlackBackgroundNumRow=5;
	private int mMaskRatio=4;
	
	// for cropBrown
	private int mBMaskThres= 150;
	private float mBMaskThresRowMean = 0.1f;
	private int mBMoreSeg = 4;
	// for cropPenn
	private int mPMaskThres= 150;
	private float mPdLineThres= 0.2f;
	private int mPdLineHeight=2;
	private int mPnumErosion=20;

	private ImagePlus mImage;
	private ImageProcessor mImageProcessor;
	private ContrastEnhancer mContrastEnhancer;	
	private ImageConverter mImageConverter;
	private int mWidth, mHeight; 
	private int[] mPixels;
	private String mImName;
	private String mOutDir;

	public enum CropId {
		BROWN, PENN 
	}
	public SanskritCrop(String outDir, int processRatio) {
		mOutDir = outDir;
		mMaskRatio = processRatio;
	}
	public int getWidth(){return mWidth;}
	public int getHeight(){return mHeight;}
	public int[] getPixels(){return mPixels;}
	public ImageProcessor getIp(){return mImageProcessor;}
	public void setVerbose(Boolean verbose){mVerbose = verbose;}
	public void ReadImage(String imName){
		// read in rgb image (some images are 16 bits)
		mImName = imName;
		mImage = new ImagePlus(imName);		
		mImageConverter = new ImageConverter(mImage);
		mImageConverter.convertToRGB();

		// adjust color range
		mImageProcessor = mImage.getProcessor();
		mContrastEnhancer = new ContrastEnhancer();		
		mContrastEnhancer.stretchHistogram(mImageProcessor, 0.01); 
		mImageProcessor.resetMinAndMax();
		
		//image info
		mWidth = mImageProcessor.getWidth();  
		mHeight = mImageProcessor.getHeight();
		mPixels = (int[])mImageProcessor.getPixels();
	}

	private CropId CropType(){
		float bg = SanskritUtil.getRegionMean(mPixels,0,mBlackBackgroundNumRow,0,mWidth, mWidth, mHeight,0); 
		CropId id = CropId.BROWN;
		print("bg color: "+bg);
		if(bg<=mBlackBackground){
			id = CropId.PENN;
		}
		return id;		
	}
	
	public void Crop(String imName) throws IOException{
		// read image
		ReadImage(imName);
		CropId id = CropType();
		switch (id){
		case BROWN: 
			CropBrown();
			break;
		case PENN: 
			CropPenn();
			break;
		}
	}
	private void print(String text){
		if(mVerbose) System.out.println(text);
	}
	public void CropBrown() throws IOException{
		// white background
		print("----Start Crop Brown----");
		print("1. find mask");
		// 1. get binary mask
		Boolean[] imgMask = SanskritUtil.getChannelThres(mPixels, mWidth, mHeight, 2, mBMaskThres, false);	
		SanskritUtil.thresholdRowMean(imgMask,mBMaskThresRowMean,mWidth, mHeight);
		
		print("2. find leaf body");
		// 2. find largest component		
		int dim[]={mWidth,mHeight};
		SanskritUtil.Pair<int[],int[]> out = SanskritUtil.ConnectComponent.bwlabel(imgMask,dim);
		int[] maxLabel = SanskritUtil.ConnectComponent.getMaxcountLabel();
		print("max: "+maxLabel[0]);
		
		print("3. find bounding box");
		// 3. find bounding box
		int[] bbox= SanskritUtil.findBbox(out.getFirst(), mWidth, mHeight, maxLabel[0]);
		// SanskritUtil.printArray(bbox,4);
		if((bbox[3]-bbox[2])<0.8*mWidth){
			// 3.1 if bbox isn't wide enough, need to connect broken pieces
			int numLabel = SanskritUtil.ConnectComponent.getMaxLabel();
			Integer[] topIndex = SanskritUtil.SortArrIndex(out.getSecond(),numLabel);
			int[] bbox2={mHeight,0,mWidth,0};
			for(int i=1;i<1+mBMoreSeg;i++){
				bbox2 = SanskritUtil.findBbox(out.getFirst(), mWidth, mHeight, topIndex[i]);
				SanskritUtil.unoinBbox(bbox,bbox2);
			}
		}
		print("4. output");
		// 4. output
		int numPic=1;
		// 4.1 output xml
		SanskritUtil.outputXml(mImName, mOutDir, bbox, numPic);
		// 4.2 output image
		SanskritUtil.outputIm(mImageProcessor, mImName, mOutDir, bbox, numPic);		
		print("----Done Crop Brown----");
	}
	
	
	public void CropPenn() throws IOException{
		print("----Start Crop Penn----");		
		print("0. classify type by shape");
		int penId=0;
		int numPic=1;
		Boolean doVCrop = true;  
		if(mWidth<mHeight*2 && mHeight<mWidth*2 && mWidth<6700){
			// 0: two pages in vertical or one page
			penId=0;
		}else if(mWidth>mHeight*2 || mWidth>=6700){
			// 1: two pages in horizontal
			penId=1;
			doVCrop = false;
		}else if(mHeight<mWidth*2){
			// 2: one page (already done)
			penId=2;
		}
		print("  type: "+penId);
		print("1. find mask");

		// 1. get binary mask
		// complicated to get connected component ... too many overhead 
		/*
		ImagePlus imp = SanskritUtil.getChannel(mImage,0);
		ImageProcessor ip = imp.getProcessor(); 
		ip.threshold(mPMaskThres);
		*/
		/*
		int[] imgMask = SanskritUtil.getChannelRed(mPixels, mWidth, mHeight);
		SanskritUtil.threshold(imgMask,mPMaskThres,mWidth, mHeight,1);  		
		*/
		
		Boolean[] imgMask = SanskritUtil.getChannelThres(mPixels, mWidth, mHeight,0, mPMaskThres,true);
		Boolean[] imgMask_s = SanskritUtil.resize(imgMask, mWidth, mHeight, mMaskRatio); // downsample for fast processing
		//SanskritUtil.threshold(imgMask,mPMaskThres,mWidth, mHeight,1);
		//FileSaver fs = new FileSaver(imp);fs.saveAsBmp("haha.jpg");System.exit(0);		
		
		// 2. find division line
		print("2. find division line");
		ArrayList<Integer> divLine;

		divLine=SanskritUtil.findDivLine(imgMask, mWidth, mHeight, mPdLineThres, mPdLineHeight, mPnumErosion, doVCrop);
		//divLine=SanskritUtil.findDivLine(ip, mWidth, mHeight, mPdLineThres, mPdLineHeight, mPnumErosion);
		if(divLine.size()>0){
			print("	  two components found");
			// fill a zero row to seperate two pages
			numPic = 2;
			int divRow = divLine.get((int)(Math.floor(divLine.size()*0.5f)));
			//SanskritUtil.fillRow(imgMask, mWidth, divRow,divRow+1,false);
			SanskritUtil.fillLine(imgMask_s, (int)(mWidth/mMaskRatio), (int)(mHeight/mMaskRatio), divRow/mMaskRatio,divRow/mMaskRatio+1,false, doVCrop);
			//SanskritUtil.fillRow(ip, divRow,divRow+1,0);
		}else{
			print("	  one component found");
		}

		//SanskritUtil.arrayToSave("db.jpg",imgMask,mWidth,mHeight);
		//FileSaver fs = new FileSaver(imp);fs.saveAsBmp("haha.jpg");System.exit(0);
		
		// 3. locate connected component
		print("3. locate connected component at speed "+mMaskRatio);
		/**/
		//int dim[]={mWidth,mHeight};
		int dim[]={mWidth/mMaskRatio,mHeight/mMaskRatio};
		SanskritUtil.Pair<int[],int[]> out = SanskritUtil.ConnectComponent.bwlabel(imgMask_s,dim);
		int[] maxLabel = SanskritUtil.ConnectComponent.getMaxcountLabel();		
		print("max: "+maxLabel[0]+","+maxLabel[1]);
		
		print("4. find bounding box");
		// 4. find bounding box
		int[] bbox = new int[numPic*4];
		if(numPic==1){
			bbox= SanskritUtil.findBbox(out.getFirst(), dim[0], dim[1], maxLabel[0]);			
		}else{
			int[] bbox2;
			bbox2= SanskritUtil.findBbox(out.getFirst(), dim[0], dim[1], maxLabel[0]);
			System.arraycopy(bbox2, 0, bbox, 0, 4);			
			bbox2= SanskritUtil.findBbox(out.getFirst(), dim[0], dim[1], maxLabel[1]);
			// enforce the top-down order of bounding box
			if(bbox2[0]<bbox[0]){
				System.arraycopy(bbox, 0, bbox, 4, 4);
				System.arraycopy(bbox2, 0, bbox, 0, 4);
			}else{
				System.arraycopy(bbox2, 0, bbox, 4, 4);
			}
		}		 
		SanskritUtil.resizeBbox(bbox,numPic*4,mMaskRatio);
		// 5. output
		print("5. output");
		// 5.1 output xml
		SanskritUtil.outputXml(mImName, mOutDir, bbox, numPic);
		// 5.2 output image
		SanskritUtil.outputIm(mImageProcessor, mImName, mOutDir, bbox, numPic);		
		print("----Done Crop Penn----");
	}
}
