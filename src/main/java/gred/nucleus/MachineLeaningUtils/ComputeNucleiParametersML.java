package gred.nucleus.MachineLeaningUtils;

import gred.nucleus.FilesInputOutput.Directory;
import gred.nucleus.FilesInputOutput.OutputTexteFile;
import gred.nucleus.core.Measure3D;
import gred.nucleus.plugins.PluginParameters;
import gred.nucleus.utils.Histogram;
import ij.ImagePlus;
import ij.ImageStack;
import inra.ijpb.binary.BinaryImages;
import inra.ijpb.label.LabelImages;
import loci.plugins.BF;

import java.io.File;
import java.util.ArrayList;

public class ComputeNucleiParametersML {
    String m_rawImagesInputDirectory;
    String m_segmentedImagesDirectory;

    /**
     * Constructor
     * @param rawImagesInputDirectory path to raw images
     * @param segmentedImagesDirectory path to list of segmented images from machine learning associated to raw
     * @throws Exception
     */
    public ComputeNucleiParametersML(String rawImagesInputDirectory, String segmentedImagesDirectory) throws  Exception{
        this.m_rawImagesInputDirectory=rawImagesInputDirectory;
        this.m_segmentedImagesDirectory=segmentedImagesDirectory;
    }

    /**
     * Run parameters computation parameters see Measure3D
     * @throws Exception
     */
    public void run() throws Exception{
        PluginParameters pluginParameters= new PluginParameters(this.m_rawImagesInputDirectory,this.m_segmentedImagesDirectory);
        Directory directoryInput = new Directory(pluginParameters.getOutputFolder());
        directoryInput.listImageFiles(pluginParameters.getOutputFolder());
        directoryInput.checkIfEmpty();
        ArrayList<File> segImages =directoryInput.m_listeOfFiles;
        String outputCropGeneralInfoOTSU=pluginParameters.getAnalyseParameters()+getColnameResult();
        for (short i = 0; i < segImages.size(); ++i) {
            File currentFile = segImages.get(i);
            System.out.println("current File "+currentFile.getName());
            ImagePlus Raw = new ImagePlus(pluginParameters.getInputFolder()+directoryInput.getSeparator()+currentFile.getName());
            ImagePlus[] Segmented = BF.openImagePlus(pluginParameters.getOutputFolder()+currentFile.getName());
            // TODO TRANSFORMATION FACTORISABLE AVEC METHODE DU DESSUS !!!!!
            Segmented[0]=generateSegmentedImage(Segmented[0],1);
            Segmented[0] = BinaryImages.componentsLabeling(Segmented[0], 26,32);
            LabelImages.removeBorderLabels(Segmented[0]);
            Segmented[0]=generateSegmentedImage(Segmented[0],1);
            Histogram histogram = new Histogram ();
            histogram.run(Segmented[0]);
            if (histogram.getNbLabels() > 0) {
                Measure3D mesure3D = new Measure3D(Segmented, Raw, pluginParameters.getXcalibration(Raw), pluginParameters.getYcalibration(Raw), pluginParameters.getZcalibration(Raw));
                outputCropGeneralInfoOTSU += mesure3D.nucleusParameter3D() + "\n";
            }
        }

        OutputTexteFile resultFileOutputOTSU=new OutputTexteFile(pluginParameters.getOutputFolder()
                +directoryInput.getSeparator()
                +"result_Segmentation_Analyse.csv");
        resultFileOutputOTSU.SaveTexteFile( outputCropGeneralInfoOTSU);

    }

    /**
     *
     * @return columns names for results
     */
    public static String getColnameResult(){
        return "NucleusFileName\tVolume\tFlatness\tElongation\tSphericity\tEsr\tSurfaceArea\tSurfaceAreaCorrected\tSphericityCorrected\tMeanIntensity\tStandardDeviation\tMinIntensity\tMaxIntensity\n";
    }

    /**
     * Filter connected connected component if more then 1 nuclei
     * @param imagePlusInput
     * @param threshold
     * @return
     */
    public static ImagePlus generateSegmentedImage (ImagePlus imagePlusInput,
                                                    int threshold)  {
        ImageStack imageStackInput = imagePlusInput.getStack();
        ImagePlus imagePlusSegmented = imagePlusInput.duplicate();

        imagePlusSegmented.setTitle(imagePlusInput.getTitle());
        ImageStack imageStackSegmented = imagePlusSegmented.getStack();
        for(int k = 0; k < imagePlusInput.getStackSize(); ++k) {
            for (int i = 0; i < imagePlusInput.getWidth(); ++i) {
                for (int j = 0; j < imagePlusInput.getHeight(); ++j) {
                    double voxelValue = imageStackInput.getVoxel(i, j, k);
                    if (voxelValue > 1) {
                        imageStackSegmented.setVoxel(i, j, k, 255);
                        imageStackInput.getVoxel(i, j, k);
                    }
                    else {
                        imageStackSegmented.setVoxel(i, j, k, 0);
                    }
                }
            }
        }
        return imagePlusSegmented;

    }
}