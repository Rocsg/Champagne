package com.vitimage.champagne;

import com.vitimage.common.TransformUtils;
import com.vitimage.common.VitimageUtils;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;

/**
 * Hello world!
 *
 */
public class PlotIntensityInSegmentation 
{
    public static void main( String[] args )
    {
    	ImageJ ij=new ImageJ();
    	computePlotData2();
    }
    
    public static void computePlotData2() {
    	String[]specs=new String[] {"CEP011_AS1","CEP015_RES2","CEP018_S2","CEP021_APO2"};
    	int []ceps=new int [] {1,5,8,11};
    	String[]strCat=new String[] {"AS","RES","S","APO"};
    	String[]strTissue=new String[] {"Healthy","Degraded","WhiteRot","Bark"};
    	String[]strMod=new String[] {"RX","T1w","T2w","PDw"};
    	double [][][]vals=new double[specs.length][][]; //mod+dist,tissue,cep, data


    	
    	for(int i=0;i<4;i++) {
    		System.out.println("\n"+specs[i]);
    		//Ouvrir l'hyperimage
    		ImagePlus hyper=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+specs[i]+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/hyperimage_THIN.tif");
			IJ.run(hyper,"32-bit","");
			ImagePlus hypRX=new Duplicator().run(hyper,1,1,1,hyper.getNSlices(),2,2);
			ImagePlus hypT1=new Duplicator().run(hyper,1,1,1,hyper.getNSlices(),3,3);
			ImagePlus hypT2=new Duplicator().run(hyper,1,1,1,hyper.getNSlices(),4,4);
			ImagePlus hypPD=new Duplicator().run(hyper,1,1,1,hyper.getNSlices(),5,5);
    		//Ouvrir la segmentation
    		ImagePlus seg=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+specs[i]+"/segmentation.tif");
			IJ.run(seg,"32-bit","");
    		//Ouvrir la distance
    		ImagePlus dist=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+specs[i]+"/imgDistFull.tif");
			IJ.run(dist,"32-bit","");
    		
    		int Z=hyper.getNSlices();
    		int X=hyper.getWidth();
    		int Y=hyper.getHeight();
    		float[][]valSeg=new float[Z][];
    		float[][]valDist=new float[Z][];
    		float[][]valRX=new float[Z][];
    		float[][]valT1=new float[Z][];
    		float[][]valT2=new float[Z][];
    		float[][]valPD=new float[Z][];
    		for(int z=0;z<Z;z++) {
    			valSeg[z]=(float[]) seg.getStack().getPixels(z+1);
    			valDist[z]=(float[]) dist.getStack().getPixels(z+1);
    			valRX[z]=(float[]) hypRX.getStack().getPixels(z+1);
    			valT1[z]=(float[]) hypT1.getStack().getPixels(z+1);
    			valT2[z]=(float[]) hypT2.getStack().getPixels(z+1);
    			valPD[z]=(float[]) hypPD.getStack().getPixels(z+1);
    		}
    		
    		
    		//Compter de chaque
    		int count=0;
    		for(int z=0;z<Z;z++) {
        		for(int x=0;x<X;x++) {
            		for(int y=0;y<Y;y++) {
            			if((x%17==0) && (y%17==0) && (valSeg[z][X*y+x]>0.5) && (!Float.isNaN(valDist[z][X*y+x])) && (valDist[z][X*y+x]>=0) )count++;
            		}
        		}
    		}
    	   	vals[i]=new double[count][11]; //mod+dist,tissue,cep, data
	
    		//Compter de chaque
    		count=0;
    		for(int z=0;z<Z;z++) {
        		for(int x=0;x<X;x++) {
            		for(int y=0;y<Y;y++) {
            			if((x%17==0) && (y%17==0) && (valSeg[z][X*y+x]>0.5) && (!Float.isInfinite(valDist[z][X*y+x])) && (valDist[z][X*y+x]>=0) ) {
            				vals[i][count++]=new double[] {count,valSeg[z][X*y+x]-1,ceps[i],x,y,z,valDist[z][X*y+x],
            						valRX[z][X*y+x],valT1[z][X*y+x],valT2[z][X*y+x],valPD[z][X*y+x]};
//    		        		Pixel# ; classe (0=H, 1=D, 2=WR, 3=BK) ; cep# (1-12) ; coordonnées X ; Y ; Z; distance (mm) from trunk reference (head - 20 cm), RX, T1, T2, PD
            				      
            			}
            		}
        		}
    		}
    		VitimageUtils.writeDoubleTabInExcelFile(vals[i],"/home/fernandr/Bureau/"+specs[i]+".csv");
    		
    	}
    		
     }
 
    public static void computePlotData1() {
    	String[]specs=new String[] {"CEP011_AS1","CEP015_RES2","CEP018_S2","CEP021_APO2"};
    	String[]strCat=new String[] {"AS","RES","S","APO"};
    	String[]strTissue=new String[] {"Healthy","Degraded","WhiteRot"};
    	String[]strMod=new String[] {"RX","T1w","T2w","PDw"};

    	
    	for(int i=0;i<4;i++) {
    		System.out.println("\n"+specs[i]);
 		//Ouvrir l'hyperimage
    		ImagePlus hyper=IJ.openImage("/home/fernandr/Bureau/Traitements/Cep5D/"+specs[i]+"/Source_data/PHOTOGRAPH/Computed_data/3_Hyperimage/hyperimage_THIN.tif");
    		//Ouvrir la segmentation
    		ImagePlus seg=IJ.openImage("/home/fernandr/Bureau/ML_CEP/RESULTS/EXP_6_ON_STACKS/"+specs[i]+"/segmentation.tif");

    		
    		
    		//Pour toutes les categories de tissu
    		for(int indTis=0;indTis<3;indTis++) {
	    		System.out.print("--------------------------------------------\n-> "+strTissue[indTis]+" , stats over ");
	    		//Pour toutes les modalites
        		for(int indMod=0;indMod<4;indMod++) {
	    		
		    		//Seuiller la seg pour get la modalité
        			ImagePlus segTis=VitimageUtils.thresholdImage(seg, indTis+0.9, indTis+1.1);
        			IJ.run(segTis,"32-bit","");
        			
        			//Extraire la modalité de l'hyperimage
        			ImagePlus hypMod=new Duplicator().run(hyper,1,1,1,hyper.getNSlices(),2+indMod,2+indMod);
        			IJ.run(hypMod,"32-bit","");
		    		
		    		//Multiplier l'un par l'autre
        			double []vals=VitimageUtils.getValuesInMask(hypMod,segTis);
        			if(indMod==0)System.out.println(vals.length+" voxels.");
		    		System.out.println(strMod[indMod]+" : value="+VitimageUtils.statistics1D(vals)[0]+" +- "+VitimageUtils.statistics1D(vals)[1]);
		    		//Annoncer la moyenne et l'écart type
		    		
		    		
		    		//Enregistrer les valeurs comme CSV

        		}	    		
    		}    		
    	}
    }
}
