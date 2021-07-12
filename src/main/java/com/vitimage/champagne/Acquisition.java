package com.vitimage.champagne;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import com.vitimage.common.VitimageUtils;
import com.vitimage.registration.ItkTransform;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.Duplicator;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import math3d.Point3d;

public abstract class Acquisition{
	
	public final static String slash=File.separator;
	public static final int ERROR_VALUE=-1;
	public static final int COORD_OF_MAX_IN_TWO_LAST_SLICES=1;
	public static final int COORD_OF_MAX_ALONG_Z=2;

	public static  AcquisitionType stringToAcquisitionType(String str){
		switch(str) {
		case "MRI_SE_SEQ": return AcquisitionType.MRI_SE_SEQ;
		case "MRI_T1_SEQ": return AcquisitionType.MRI_T1_SEQ;
		case "MRI_T2_SEQ": return AcquisitionType.MRI_T2_SEQ;
		case "MRI_GE3D": return AcquisitionType.MRI_GE3D;
		case "MRI_DIFF_SEQ": return AcquisitionType.MRI_DIFF_SEQ;
		case "MRI_FLIPFLOP_SEQ": return AcquisitionType.MRI_FLIPFLOP_SEQ;
		case "RX": return AcquisitionType.RX;
		case "HISTOLOGY": return AcquisitionType.HISTOLOGY;
		case "PHOTOGRAPH": return AcquisitionType.PHOTOGRAPH;
		case "TERAHERTZ": return AcquisitionType.TERAHERTZ;
		}
		return null;
	}
	
	public static VineType stringToVineType(String str){
		switch(str) {
		case "GRAFTED_VINE": return VineType.GRAFTED_VINE;
		
		case "VINE": return VineType.VINE;
		case "CUTTING": return VineType.CUTTING;
		}
		return null;
	}
	
	public enum ComputingType{
		COMPUTE_ALL,
		EVERYTHING_UNTIL_MAPS,
		EVERYTHING_AFTER_MAPS
	}
	
	public enum AcquisitionType{
		MRI_CLINICAL,
		MRI_SE_SEQ,
		MRI_T1_SEQ,
		MRI_T2_SEQ,
		MRI_DIFF_SEQ,
		MRI_FLIPFLOP_SEQ,
		RX,
		HISTOLOGY,
		PHOTOGRAPH,
		TERAHERTZ,
		MRI_GE3D
	}

	public enum SupervisionLevel{
		AUTONOMOUS,
		GET_INFORMED,
		ASK_FOR_ALL
	}	

	public enum Capillary{
		HAS_CAPILLARY,
		HAS_NO_CAPILLARY
	}

	
	
	public static void main(String[]args) {
		System.out.println("Compil ok");
	}
	
	/**
	 * Parameters
	 */
	public final static double maxShortVal=Math.pow(2,16)-1;
	public ImagePlus hyperEchoes=null;
	protected int hyperSize=1;
	protected ImagePlus normalizedHyperImage;
	protected ImagePlus[]sourceData;
	protected ImagePlus[]computedData;
	protected double valMaxForCalibration;
	protected double valMinForCalibration;	
	protected double valMedForThresholding;
	private Point3d inoculationPoint;
	private ItkTransform transformation;
	public ImagePlus imageForRegistration;
	public AcquisitionType acquisitionType;
	protected Capillary capillary;
	protected String sourcePath;
	protected String operator;
	private String machineBrand;
	protected Date acquisitionDate;
	protected String acquisitionPlace;
	protected double acquisitionDuration=0;
	protected String unit="mm";
	private int dimX;
	private int dimY;
	private int dimZ;
	private double voxSX;
	private double voxSY;
	private double voxSZ;
	private double spacingX;
	private double spacingY;
	private double spacingZ;
	private Point3d referencePoint;
	private ArrayList<String>pathToImages;
	protected double []computedValuesNormalisationFactor;
	protected double sigmaBackground;
	protected double meanBackground;
	protected String title="-";
	protected int timeSerieDay;
	protected String projectName="VITIMAGE";
	protected String acquisitionPosition;
	protected String acquisitionOrientation;
	protected String dataPath="-";
	protected String acquisitionTime;
	protected ImagePlus mask;
	public ComputingType computingType;
	public static double sigmaGaussMapInPixels=0.8;
	/**
	 * Abstract methods
	 */
	
	
	public Point3d getReferencePoint() {
		return referencePoint;
	}
	public void setReferencePoint(Point3d referencePoint) {
		this.referencePoint = referencePoint;
	}
	public void setComputedData(ImagePlus[] computedData) {
		this.computedData = computedData;
	}
	public AcquisitionType getAcquisitionType() {
		return acquisitionType;
	}		
	public SupervisionLevel supervisionLevel;
	
	public abstract ImagePlus computeNormalizedHyperImage();
	public abstract void setImageForRegistration();
	public abstract void quickStartFromFile();
	public abstract void processData();
	public abstract void start();

	
	/**
	 * Top level methods and constructor
	 */
	public Acquisition(AcquisitionType acquisitionType,String sourcePath,Capillary cap,SupervisionLevel sup) {		
		this.acquisitionType=acquisitionType;
		this.sourcePath=sourcePath;
		this.capillary=cap;
		this.transformation=new ItkTransform();
		this.supervisionLevel=sup;
	}
	
	public ImagePlus getTransformedRegistrationImage() {
		ImagePlus temp=this.transformation.transformImage(this.imageForRegistration, this.imageForRegistration,false);
		temp.getProcessor().setMinAndMax(this.valMinForCalibration,this.valMaxForCalibration);
		return temp;		
	}
	
	

	
	public ImagePlus[]getSourceDataWithSmoothedCapillary(){

		//Produce the smoothed version of source Data
		if (this.capillary == Capillary.HAS_NO_CAPILLARY)return sourceData;
		ImagePlus img=new Duplicator().run(this.imageForRegistration);
		ImagePlus []retTab=new ImagePlus[sourceData.length];
		ImagePlus []smoothTab=new ImagePlus[sourceData.length];
		for(int sd=0;sd<sourceData.length;sd++) {
			retTab[sd]=new Duplicator().run(this.sourceData[sd]);
			smoothTab[sd]=new Duplicator().run(this.sourceData[sd]);
			smoothTab[sd]=VitimageUtils.convertFloatToShortWithoutDynamicChanges(VitimageUtils.gaussianFiltering(smoothTab[sd],this.voxSX*3, this.voxSX*3, this.voxSX*3));
		}
		ImagePlus imgSliceInput;
		int xMax=this.dimX;
		int yMax=this.dimY;
		int zMax=this.dimZ;
		double diamCap=0.05;
		double valThresh=this.valMedForThresholding;//standard measured values on echo spin images of bionanoNMRI
		double x0RoiCap;
		double y0RoiCap;
		RoiManager rm=RoiManager.getRoiManager();
		ImageStack isRet=new ImageStack(img.getWidth(),img.getHeight(),img.getStackSize());
		Roi capArea=null,capArea2=null;
		ImagePlus imgSlice,imgCon;
		for(int z=1;z<=zMax;z++) {
			//For each slice, look at the capillary area, then replace this area by the smoothed version
			imgSlice=new ImagePlus("", img.getStack().getProcessor(z));
			imgCon=VitimageUtils.connexe(imgSlice, valThresh, 10E10, 0, 10E10,4,2,true);
			imgCon.getProcessor().setMinAndMax(0,255);
			imgCon.setTitle("Z="+z);
			imgCon.show();
			//VitimageUtils.waitFor(1000);
			imgCon.hide();
			IJ.run(imgCon,"8-bit","");
			IJ.setThreshold(imgCon, 255,255);
			for(int dil=0;dil<(diamCap/img.getCalibration().pixelWidth);dil++) IJ.run(imgCon, "Dilate", "stack");
			imgCon.show();
//			VitimageUtils.waitFor(1000000); 
			rm.reset();
			capArea=new ThresholdToSelection().convert(imgCon.getProcessor());
			if(capArea != null)capArea2=capArea;
			else capArea=capArea2;
			rm.add(imgSlice, capArea, 0);							
			FloatPolygon tabPoly=capArea.getFloatPolygon();
			Rectangle rect=tabPoly.getBounds();
			int xMinRoi=(int) (rect.getX());
			int yMinRoi=(int) (rect.getY());
			int xSizeRoi=(int) (rect.getWidth());
			int ySizeRoi=(int) (rect.getHeight());
			int xMaxRoi=xMinRoi+xSizeRoi;
			int yMaxRoi=yMinRoi+ySizeRoi;				
			imgCon.hide();

			//Make the replacement on each echo image
			for(int sd=0;sd<sourceData.length;sd++) {
				short[] valsRet=(short[])(retTab[sd]).getStack().getProcessor(z).getPixels();
				short[] valsSmooth=(short[])(smoothTab[sd]).getStack().getProcessor(z).getPixels();
				for(int xx=xMinRoi;xx<=xMaxRoi;xx++) for(int yy=yMinRoi;yy<yMaxRoi;yy++) if(tabPoly.contains(xx,yy)) valsRet[xMax*yy+xx]=valsSmooth[xMax*yy+xx];
			}
		}
		for(int sd=0;sd<sourceData.length;sd++)smoothTab[sd]=null;
		smoothTab=null;
		return retTab;
	}
	
	
	
	public ImagePlus getImageForRegistrationWithoutCapillary() {
		ImagePlus img=new Duplicator().run(this.imageForRegistration);
		if (this.capillary == Capillary.HAS_NO_CAPILLARY)return img;
		ImagePlus imgSliceInput;
		int xMax=this.dimX;
		int yMax=this.dimY;
		int zMax=this.dimZ;
		double diamCap=0.6;
		double valThresh=this.valMedForThresholding;//standard measured values on echo spin images of bionanoNMRI
		double x0RoiCap;
		double y0RoiCap;
		RoiManager rm=RoiManager.getRoiManager();
		ImageStack isRet=new ImageStack(img.getWidth(),img.getHeight(),img.getStackSize());
		for(int z=1;z<=zMax;z++) {
			ImagePlus imgSlice=new ImagePlus("", img.getStack().getProcessor(z));
			ImagePlus imgCon=VitimageUtils.connexe(imgSlice, valThresh, 10E10, 0, 10E10,4,2,true);
			//imgCon.show();
			imgCon.getProcessor().setMinAndMax(0,255);
			IJ.run(imgCon,"8-bit","");
			IJ.setThreshold(imgCon, 255,255);
			for(int dil=0;dil<(diamCap/img.getCalibration().pixelWidth);dil++) IJ.run(imgCon, "Dilate", "stack");
			rm.reset();
			Roi capArea=new ThresholdToSelection().convert(imgCon.getProcessor());	
			rm.add(imgSlice, capArea, 0);							
			FloatPolygon tabPoly=capArea.getFloatPolygon();
			Rectangle rect=tabPoly.getBounds();
			int xMinRoi=(int) (rect.getX());
			int yMinRoi=(int) (rect.getY());
			int xSizeRoi=(int) (rect.getWidth());
			int ySizeRoi=(int) (rect.getHeight());
			int xMaxRoi=xMinRoi+xSizeRoi;
			int yMaxRoi=yMinRoi+ySizeRoi;				
			imgSliceInput=new ImagePlus("", img.getStack().getProcessor(z));
			if(imgSliceInput.getType()==ImagePlus.GRAY32) {
				float[] valsImg=(float[])(imgSliceInput).getProcessor().getPixels();
				//Remplacer les pixels de la zone du capillaire par des pixels copiés depuis le coin en haut à gauche de l'image 
				for(int xx=xMinRoi;xx<=xMaxRoi;xx++) for(int yy=yMinRoi;yy<yMaxRoi;yy++) if(tabPoly.contains(xx,yy)) valsImg[xMax*yy+xx]=valsImg[xMax*(yy-yMinRoi+7)+(xx-xMinRoi+7)];
				isRet.setProcessor(imgSliceInput.getProcessor(),z);
			}
			else if(imgSliceInput.getType()==ImagePlus.GRAY16) {
				short[] valsImg=(short[])(imgSliceInput).getProcessor().getPixels();
				//Remplacer les pixels de la zone du capillaire par des pixels copiés depuis le coin en haut à gauche de l'image 
				for(int xx=xMinRoi;xx<=xMaxRoi;xx++) for(int yy=yMinRoi;yy<yMaxRoi;yy++) if(tabPoly.contains(xx,yy)) valsImg[xMax*yy+xx]=valsImg[xMax*(yy-yMinRoi+7)+(xx-xMinRoi+7)];
				isRet.setProcessor(imgSlice.getProcessor(),z);
			}
			else if(imgSliceInput.getType()==ImagePlus.GRAY8) {
				byte[] valsImg=(byte[])(imgSliceInput).getProcessor().getPixels();
				//Remplacer les pixels de la zone du capillaire par des pixels copiés depuis le coin en haut à gauche de l'image 
				for(int xx=xMinRoi;xx<=xMaxRoi;xx++) for(int yy=yMinRoi;yy<yMaxRoi;yy++) if(tabPoly.contains(xx,yy)) valsImg[xMax*yy+xx]=valsImg[xMax*(yy-yMinRoi+7)+(xx-xMinRoi+7)];
				isRet.setProcessor(imgSlice.getProcessor(),z);
			}
			else IJ.log("Remove capillary : image type not handled ("+imgSliceInput.getType()+")");
		}
		ImagePlus res=new ImagePlus("Result_"+img.getShortTitle()+"_no_cap.tif",isRet);
		VitimageUtils.adjustImageCalibration(res, this.imageForRegistration);
		return res;	
	}
	
	public ImagePlus getTransformedRegistrationImage(ImagePlus imgReference) {
		return this.transformation.transformImage(imgReference, this.imageForRegistration,false);		
	}
	
	public void computeNormalisationValues() {
		this.computedValuesNormalisationFactor=new double[this.computedData.length];
		int []coordinates=this.detectCapillaryPosition(this.dimZ/2);
		for(int i=0;i<this.computedData.length;i++) {
			this.computedValuesNormalisationFactor[i]=getCapillaryValue(this.computedData[i],coordinates,4,4);
		}
	}



	public double getCapillaryValue(ImagePlus img) {
		return getCapillaryValue(img,detectCapillaryPositionImg(this.dimZ/2,img),7,7);
	}

	
	public void freeMemory() {
		imageForRegistration=null;
		normalizedHyperImage=null;
		for(int i=0;i<sourceData.length;i++) sourceData[i]=null;
		for(int i=0;i<computedData.length;i++) computedData[i]=null;
		sourceData=null;
		computedData=null;
		mask=null;
		System.gc();
	}
	
	
	public long getHyperImageModificationTime() {
		File f=new File(this.sourcePath+slash+ "Computed_data"+slash+"3_HyperImage"+slash+"hyperImage.tif");
		long val=0;
		if(f.exists())val=f.lastModified();
		return val;		
	}

	
	public static double getCapillaryValue(ImagePlus img, int[]coordinates,int rayXY,int rayZ) {
		System.out.println("");
		System.out.println("Start");
		int zMin=coordinates[2]-rayZ;
		int zMax=coordinates[2]+rayZ;
		if(zMin<0)zMin=0;
		if(zMax>=img.getStackSize())zMax=img.getStackSize()-1;
		double val=0;
		int nbSlices=0;
		for(int z=zMin;z<=zMax;z++) {
			val+=VitimageUtils.meanValueofImageAround(img,coordinates[0],coordinates[1],z,rayXY);
			nbSlices++;
		}
		return (val/nbSlices);
	}
	

	
	public double computeRiceSigmaFromBackgroundValues() {
		return VitimageUtils.computeRiceSigmaFromBackgroundValuesStatic(meanBackground,sigmaBackground);
	}
	
	public int[] detectCapillaryPositionImg(int Z,ImagePlus img2) {
		ImagePlus img=new Duplicator().run(img2);
		RoiManager rm=RoiManager.getRoiManager();
		double valThresh=this.valMedForThresholding;//standard measured values on echo spin images of bionanoNMRI
		ImagePlus imgSlice=new ImagePlus("", img.getStack().getProcessor(Z));
		VitimageUtils.imageChecking(imgSlice,"Detect capillary, Before connect group");
		ImagePlus imgCon=VitimageUtils.connexe(imgSlice, valThresh, 10E10, 0, 10E10,4,2,true);
		VitimageUtils.imageChecking(imgCon,"After selecting the second area up to "+valThresh);
		imgCon.getProcessor().setMinAndMax(0,255);
		IJ.run(imgCon,"8-bit","");
		IJ.setThreshold(imgCon, 255,255);
		rm.reset();
		Roi capArea=new ThresholdToSelection().convert(imgCon.getProcessor());	
		rm.add(imgSlice, capArea, 0);							
		Rectangle rect=capArea.getFloatPolygon().getBounds();
		System.out.println("Capillary position detected is :"+(rect.getX() + rect.getWidth()/2.0)+" , "+
															 (rect.getY() + rect.getHeight()/2.0)+" , "+
														 	 Z);
		
		//Check
		img.getProcessor().resetMinAndMax();
		ImagePlus imgView=new Duplicator().run(img);
		IJ.run(imgView,"8-bit","");
		ImagePlus capCenterImg=VitimageUtils.drawCircleInImage(imgView, 0.7, (int) (rect.getX() + rect.getWidth()/2.0),(int) (rect.getY() + rect.getHeight()/2.0) ,Z,255);		
		ImagePlus res3=VitimageUtils.compositeOf(img,capCenterImg,"Capillary exact position");
		VitimageUtils.imageChecking(res3,18,23,2,"Capillary exact position",3,false);
		res3.close();
		imgView.close();
		
		return new int[] {(int) (rect.getX() + rect.getWidth()/2.0) , (int) (rect.getY() + rect.getHeight()/2.0) , Z , (int)rect.getWidth(),(int)rect.getHeight()};  
	}
	

	public static ImagePlus areaOfPertinentMRIMapComputation (ImagePlus img2,double sigmaGaussMapInPixels){
		double voxVolume=VitimageUtils.getVoxelVolume(img2);
		int nbThreshObjects=100;
		double vX=img2.getCalibration().pixelWidth;
		double []val=Acquisition.caracterizeBackgroundOfImage(img2);
		double mu=val[0];
		double sigma=val[1];
		double thresh=mu+3*sigma;
		ImagePlus img=VitimageUtils.gaussianFiltering(img2,vX*sigmaGaussMapInPixels,vX*sigmaGaussMapInPixels,vX*sigmaGaussMapInPixels);
		System.out.println("Mu fond="+mu+" , Sigma fond="+sigma+" , Thresh="+thresh );
		ImagePlus imgMask0=VitimageUtils.getBinaryMask(img,thresh);
		ImagePlus imgMask1=VitimageUtils.connexe(imgMask0,1,256, nbThreshObjects*voxVolume,10E10, 26, 0,false);//L objet
		System.out.println(nbThreshObjects);
		ImagePlus imgMask2=VitimageUtils.getBinaryMask(imgMask1,1);
		return imgMask2;
	}

	
	/*deprecated*/
	public static int[] detectCapillaryPosition(ImagePlus imgReg,int Z) {
		ImagePlus img=new Duplicator().run(imgReg);
		int zMax=img.getStackSize();
		int tailleMorpho=4;
		ImagePlus imgMask=areaOfPertinentMRIMapComputation(img,MRI_T1_Seq.sigmaGaussMapInPixels);
		IJ.run(imgMask,"8-bit","");
		//Protection to ensure non-even connections at the top or the back of the image, due to bias in magnet field
		imgMask.getStack().getProcessor(1).set(0);
		imgMask.getStack().getProcessor(2).set(0);
		imgMask.getStack().getProcessor(3).set(0);
		imgMask.getStack().getProcessor(4).set(0);
		imgMask.getStack().getProcessor(zMax).set(0);
		imgMask.getStack().getProcessor(zMax-1).set(0);
		imgMask.getStack().getProcessor(zMax-2).set(0);
		imgMask.getStack().getProcessor(zMax-3).set(0);
		for(int t =0;t<tailleMorpho;t++) IJ.run(imgMask,"Dilate","stack");
		for(int t =0;t<tailleMorpho;t++) IJ.run(imgMask,"Erode","stack");
		ImagePlus imgCap=VitimageUtils.connexe(imgMask,1,256,0,10E10, 6, 2,false);//Le cap
		IJ.run(imgCap,"8-bit","");

		//Mise en slice et Retrait du bouzin pas beau
		imgCap=new Duplicator().run(imgCap,Z,Z);
		IJ.run(imgCap,"Erode","");
		IJ.run(imgCap,"Erode","");
		
		
		//Preparation seuillage
		IJ.setThreshold(imgCap, 255,255);
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
//		imgCap.show();
		
		Roi capArea=new ThresholdToSelection().convert(imgCap.getProcessor());	
		rm.add(imgCap, capArea, 0);							
		Rectangle rect=capArea.getFloatPolygon().getBounds();
		System.out.println("Capillary position detected is :"+(rect.getX() + rect.getWidth()/2.0)+" , "+
															 (rect.getY() + rect.getHeight()/2.0)+" , "+
														 	 Z);
		rm.close();
		return new int[] {(int) (rect.getX() + rect.getWidth()/2.0) , (int) (rect.getY() + rect.getHeight()/2.0) , Z , (int)rect.getWidth(),(int)rect.getHeight()};  
	}

	
	
	public int[] detectCapillaryPosition(int Z) {
		ImagePlus img=new Duplicator().run(this.imageForRegistration);
		int zMax=img.getStackSize();
		int tailleMorpho=4;
		ImagePlus imgMask=areaOfPertinentMRIMapComputation(img,this.sigmaGaussMapInPixels);
		IJ.run(imgMask,"8-bit","");
		//Protection to ensure non-even connections at the top or the back of the image, due to bias in magnet field
		imgMask.getStack().getProcessor(1).set(0);
		imgMask.getStack().getProcessor(2).set(0);
		imgMask.getStack().getProcessor(3).set(0);
		imgMask.getStack().getProcessor(4).set(0);
		imgMask.getStack().getProcessor(zMax).set(0);
		imgMask.getStack().getProcessor(zMax-1).set(0);
		imgMask.getStack().getProcessor(zMax-2).set(0);
		imgMask.getStack().getProcessor(zMax-3).set(0);
		for(int t =0;t<tailleMorpho;t++) IJ.run(imgMask,"Dilate","stack");
		for(int t =0;t<tailleMorpho;t++) IJ.run(imgMask,"Erode","stack");
		ImagePlus imgCap=VitimageUtils.connexe(imgMask,1,256,0,10E10, 6, 2,false);//Le cap
		IJ.run(imgCap,"8-bit","");

		//Mise en slice et Retrait du bouzin pas beau
		imgCap=new Duplicator().run(imgCap,Z,Z);
		IJ.run(imgCap,"Erode","");
		IJ.run(imgCap,"Erode","");
		
		
		//Preparation seuillage
		IJ.setThreshold(imgCap, 255,255);
		RoiManager rm=RoiManager.getRoiManager();
		rm.reset();
//		imgCap.show();
		
		Roi capArea=new ThresholdToSelection().convert(imgCap.getProcessor());	
		rm.add(imgCap, capArea, 0);							
		Rectangle rect=capArea.getFloatPolygon().getBounds();
		System.out.println("Capillary position detected is :"+(rect.getX() + rect.getWidth()/2.0)+" , "+
															 (rect.getY() + rect.getHeight()/2.0)+" , "+
														 	 Z);
		rm.close();
		return new int[] {(int) (rect.getX() + rect.getWidth()/2.0) , (int) (rect.getY() + rect.getHeight()/2.0) , Z , (int)rect.getWidth(),(int)rect.getHeight()};  
	}
	
	public static double[] caracterizeBackgroundOfImage(ImagePlus img) {
		int samplSize=Math.min(15,img.getWidth()/10);
		int x0=samplSize;
		int y0=samplSize;
		int x1=img.getWidth()-samplSize;
		int y1=img.getHeight()-samplSize;
		int z01=img.getStackSize()/2;
		double[][] vals=new double[4][2];
		vals[0]=VitimageUtils.valuesOfImageAround(img,x0,y0,z01,samplSize/2);
		vals[1]=VitimageUtils.valuesOfImageAround(img,x0,y1,z01,samplSize/2);
		vals[2]=VitimageUtils.valuesOfImageAround(img,x1,y0,z01,samplSize/2);
		vals[3]=VitimageUtils.valuesOfImageAround(img,x1,y1,z01,samplSize/2);		
		//System.out.println("");
		double [][]stats=new double[4][2];
		double []globStats=VitimageUtils.statistics2D(vals);
		//System.out.println("Background statistics averaged on the four corners = ( "+globStats[0]+" , "+globStats[1]+" ) ");
		for(int i=0;i<4;i++) {
			stats[i]=(VitimageUtils.statistics1D(vals[i]));
			//System.out.println("  --> Stats zone "+i+" =  ( "+stats[i][0]+" , "+stats[i][1]+")");
			if( (Math.abs(stats[i][0]-globStats[0])/globStats[0]>0.3)){
				System.out.println("Warning : noise computation  There should be an object in the supposed background\nthat can lead to misestimate background values. Detected at slice "+samplSize/2+"at "+
							(i==0 ?"Up-left corner" : i==1 ? "Down-left corner" : i==2 ? "Up-right corner" : "Down-right corner")+
							". Mean values of squares="+globStats[0]+". Outlier value="+vals[i][0]+" you should inspect the image and run again.");
				//img.show();
			}
		}
		return new double[] {globStats[0],globStats[1]};
	}
	


	public void caracterizeBackgroundStatic(ImagePlus img) {
		int samplSize=Math.min(10+20,this.dimX()/10);
		int x0=samplSize;
		int y0=samplSize;
		int x1=this.dimX()-samplSize;
		int y1=this.dimY()-samplSize;
		int z01=this.dimZ()/2;
		double[][] vals=new double[4][2];
		vals[0]=VitimageUtils.valuesOfImageAround(this.imageForRegistration,x0,y0,z01,samplSize/2);
		vals[1]=VitimageUtils.valuesOfImageAround(this.imageForRegistration,x0,y1,z01,samplSize/2);
		vals[2]=VitimageUtils.valuesOfImageAround(this.imageForRegistration,x1,y0,z01,samplSize/2);
		vals[3]=VitimageUtils.valuesOfImageAround(this.imageForRegistration,x1,y1,z01,samplSize/2);		
		System.out.println("");
		double [][]stats=new double[4][2];
		double []globStats=VitimageUtils.statistics2D(vals);
		System.out.println("Background statistics averaged on the four corners = ( "+globStats[0]+" , "+globStats[1]+" ) ");
		for(int i=0;i<4;i++) {
			stats[i]=(VitimageUtils.statistics1D(vals[i]));
			System.out.println("  --> Stats zone "+i+" =  ( "+stats[i][0]+" , "+stats[i][1]+")");
			if( (Math.abs(stats[i][0]-globStats[0])/globStats[0]>0.3)){
				System.out.println("Warning : noise computation of "+this.getSourcePath()+" "+this.getTitle()+
							" There should be an object in the supposed background\nthat can lead to misestimate background values. Detected at slice "+samplSize/2+"at "+
							(i==0 ?"Up-left corner" : i==1 ? "Down-left corner" : i==2 ? "Up-right corner" : "Down-right corner")+
							". Mean values of squares="+globStats[0]+". Outlier value="+vals[i][0]+" you should inspect the image and run again.");
				this.imageForRegistration.show();
			}
		}
		this.meanBackground=globStats[0];
		this.sigmaBackground=globStats[1];
		if(this.supervisionLevel != SupervisionLevel.AUTONOMOUS)System.out.println("Background caracterised : mean="+this.meanBackground+" sigma="+this.sigmaBackground);
	}
	

	public void caracterizeBackground() {
		int samplSize=Math.min(10+20,this.dimX()/10);
		int x0=samplSize;
		int y0=samplSize;
		int x1=this.dimX()-samplSize;
		int y1=this.dimY()-samplSize;
		int z01=this.dimZ()/2;
		double[][] vals=new double[4][2];
		vals[0]=VitimageUtils.valuesOfImageAround(this.imageForRegistration,x0,y0,z01,samplSize/2);
		vals[1]=VitimageUtils.valuesOfImageAround(this.imageForRegistration,x0,y1,z01,samplSize/2);
		vals[2]=VitimageUtils.valuesOfImageAround(this.imageForRegistration,x1,y0,z01,samplSize/2);
		vals[3]=VitimageUtils.valuesOfImageAround(this.imageForRegistration,x1,y1,z01,samplSize/2);		
		System.out.println("");
		double [][]stats=new double[4][2];
		double []globStats=VitimageUtils.statistics2D(vals);
		System.out.println("Background statistics averaged on the four corners = ( "+globStats[0]+" , "+globStats[1]+" ) ");
		for(int i=0;i<4;i++) {
			stats[i]=(VitimageUtils.statistics1D(vals[i]));
			System.out.println("  --> Stats zone "+i+" =  ( "+stats[i][0]+" , "+stats[i][1]+")");
			if( (Math.abs(stats[i][0]-globStats[0])/globStats[0]>0.3)){
				System.out.println("Warning : noise computation of "+this.getSourcePath()+" "+this.getTitle()+
							" There should be an object in the supposed background\nthat can lead to misestimate background values. Detected at slice "+samplSize/2+"at "+
							(i==0 ?"Up-left corner" : i==1 ? "Down-left corner" : i==2 ? "Up-right corner" : "Down-right corner")+
							". Mean values of squares="+globStats[0]+". Outlier value="+vals[i][0]+" you should inspect the image and run again.");
				this.imageForRegistration.show();
			}
		}
		this.meanBackground=globStats[0];
		this.sigmaBackground=globStats[1];
		if(this.supervisionLevel != SupervisionLevel.AUTONOMOUS)System.out.println("Background caracterised : mean="+this.meanBackground+" sigma="+this.sigmaBackground);
	}
	

/**
 * Low level methods
 * @return
 */
	public int getTimeSerieDay() {
		return this.timeSerieDay;
	}
	public String getProjectName() {
		return this.projectName;
	}
	public String getTitle() {
		return(this.title);
	}
	public void setTitle(String title) {
		this.title=title;
	}
	public final void setVoxelSizes(double voxSX,double voxSY,double voxSZ) {
		this.voxSX=voxSX;
		this.voxSY=voxSY;
		this.voxSZ=voxSZ;		
	}
	
	
	public final void setVoxelSizes(double []voxS) {
		this.voxSX=voxS[0];
		this.voxSY=voxS[1];
		this.voxSZ=voxS[2];		
	}

	
	public ImagePlus[] getComputedData() {
		return this.computedData;
	}
	public void addTransform(ItkTransform trAdd) {
		this.transformation.addTransform(trAdd);
	}
	public ItkTransform getTransform() {
		return this.transformation;
	}
	public final void setDimensions(int dimX,int dimY, int dimZ) {
		this.dimX=dimX;
		this.dimY=dimY;
		this.dimZ=dimZ;		
	}
	public int dimX() {
		return dimX;
	}
	public void setInoculationPoint(Point3d pt) {
		this.inoculationPoint=pt;
	}
	public Point3d getInoculationPoint() {
		return this.inoculationPoint;
	}	
	public final void setSpacing(double spacingX,double spacingY,double spacingZ) {
		this.spacingX=spacingX;
		this.spacingY=spacingY;
		this.spacingZ=spacingZ;		
	}
	public void setDimX(int dimX) {
		this.dimX = dimX;
	}
	public int dimY() {
		return dimY;
	}
	public void setDimY(int dimY) {
		this.dimY = dimY;
	}
	public int dimZ() {
		return dimZ;
	}
	public void setDimZ(int dimZ) {
		this.dimZ = dimZ;
	}
	public double voxSX() {
		return voxSX;
	}
	public void setVoxSX(double voxSX) {
		this.voxSX = voxSX;
	}
	public double voxSY() {
		return voxSY;
	}
	public void setVoxSY(double voxSY) {
		this.voxSY = voxSY;
	}
	public double voxSZ() {
		return voxSZ;
	}
	public void setVoxSZ(double voxSZ) {
		this.voxSZ = voxSZ;
	}
	public double getSpacingX() {
		return spacingX;
	}
	public void setSpacingX(double spacingX) {
		this.spacingX = spacingX;
	}
	public double getSpacingY() {
		return spacingY;
	}
	public void setSpacingY(double spacingY) {
		this.spacingY = spacingY;
	}
	public double getSpacingZ() {
		return spacingZ;
	}
	public void setSpacingZ(double spacingZ) {
		this.spacingZ = spacingZ;
	}
	public ImagePlus[] getSourceData() {
		return sourceData;
	}
	public void setSourceData(ImagePlus[] sourceData) {
		this.sourceData = sourceData;
	}
	public String getOperator() {
		return operator;
	}
	public void setOperator(String operator) {
		this.operator = operator;
	}
	public String getMachineBrand() {
		return machineBrand;
	}
	public void setMachineBrand(String machineBrand) {
		this.machineBrand = machineBrand;
	}
	public Date getAcquisitionDate() {
		return acquisitionDate;
	}
	public void setAcquisitionDate(Date acquisitionDate) {
		this.acquisitionDate = acquisitionDate;
	}
	public String getAcquisitionPlace() {
		return acquisitionPlace;
	}
	public void setAcquisitionPlace(String acquisitionPlace) {
		this.acquisitionPlace = acquisitionPlace;
	}
	public String getSourcePath() {
		return sourcePath;
	}
	public ImagePlus getImageForRegistration() {
		return new Duplicator().run(this.imageForRegistration);
	}
	
	public void printStartMessage() {
		System.out.println("");
		System.out.println("");
		System.out.println("####################################");
		System.out.println("######## Starting new acquisition ");
		String str=""+this.getAcquisitionType()+"";
		System.out.println("######## "+str+"");
		System.out.println("####################################");		
	}
			
}
