package gred.nucleus.autocrop;

import gred.nucleus.files.Directory;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.TextRoi;
import ij.io.FileSaver;
import ij.plugin.ZProjector;
import loci.formats.FormatException;
import loci.plugins.BF;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;


/**
 * This class creates Z projection file of 3D stack (wide field image) and report boxes for each nucleus cropped by the
 * Autocrop class.
 * <p>
 * It takes the raw images input crop and the list of boxes coordinates generated by the Autocrop class.
 *
 * @author Tristan Dubos and Axel Poulet
 */
public class AnnotateAutoCrop {
	
	/** List of the coordinate boxes of cropped nucleus */
	private final List<String>       boxCoordinates;
	/** the path of the directory where image with boxes is saved */
	private final String             outputDirPath;
	/** Parameters crop analyse */
	private final AutocropParameters autocropParameters;
	/** File to process (Image input) */
	File currentFile;
	/** ImagePlus of the Z projection */
	private ImagePlus zProjection;
	/** The prefix of the names of the output cropped images, which are automatically numbered */
	private String    outputFilesPrefix;
	
	
	/**
	 * Constructor for autocrop
	 *
	 * @param boxesCoordinates   List of coordinates (coordinates of nuclei cropped)
	 * @param imageFile          File of current image analysed
	 * @param outputDirPath      Path to the output folder
	 * @param autocropParameters Autocrop parameters used to crop nuclei
	 * @param prefix             Name of raw image (use for z projection)
	 *
	 * @throws IOException     if imageFile cannot be opened
	 * @throws FormatException if something goes wrong performing a file format operation
	 */
	public AnnotateAutoCrop(List<String> boxesCoordinates,
	                        File imageFile,
	                        String outputDirPath,
	                        String prefix,
	                        AutocropParameters autocropParameters)
	throws IOException, FormatException {
		this(boxesCoordinates, imageFile, outputDirPath, autocropParameters);
		this.outputFilesPrefix = prefix;
		Directory dirOutput = new Directory(this.outputDirPath + "zprojection");
		dirOutput.checkAndCreateDir();
	}
	
	
	/**
	 * Constructor for re-generate projection after segmentation
	 *
	 * @param boxesCoordinates   List of coordinates (coordinates of nuclei cropped)
	 * @param imageFile          File of current image analysed
	 * @param outputDirPath      Path to the output folder
	 * @param autocropParameters Autocrop parameters used to crop nuclei
	 *
	 * @throws IOException     If imageFile cannot be opened
	 * @throws FormatException If something goes wrong performing a file format operation
	 */
	public AnnotateAutoCrop(List<String> boxesCoordinates,
	                        File imageFile,
	                        String outputDirPath,
	                        AutocropParameters autocropParameters)
	throws IOException, FormatException {
		this.autocropParameters = autocropParameters;
		this.currentFile = imageFile;
		this.zProjection =
				BF.openImagePlus(imageFile.getAbsolutePath())[this.autocropParameters.getSlicesOTSUComputing()];
		this.boxCoordinates = boxesCoordinates;
		this.outputDirPath = outputDirPath;
	}
	
	
	/**
	 * Main method to generate Z projection of wide field 3D image. Parameter use are max intensity projection
	 * (projectionMax method) and contrast modification of 0,3.
	 */
	public void runAddBadCrop(List<Integer> box) {
		IJ.run(this.zProjection, "Enhance Contrast", "saturated=0.35");
		IJ.run(this.zProjection, "RGB Color", "");
		ZProjector zProjectionTmp = new ZProjector(this.zProjection);
		
		for (String boxCoordinate : this.boxCoordinates) {
			String[] splitLine = boxCoordinate.split("\\t");
			String[] fileName  = splitLine[0].split(Pattern.quote(File.separator));
			String[] name      = fileName[fileName.length - 1].split("_");
			addBadCropBoxToZProjection(boxCoordinate, Integer.parseInt(name[name.length - 2]));
		}
		String outFileZBox = this.outputDirPath + "_BAD_CROP_LESS.tif";
		saveFile(this.zProjection, outFileZBox);
	}
	
	
	/**
	 * Main method to generate Z projection of wide field 3D image. Parameter use are max intensity projection
	 * (projectionMax method) and contrast modification of 0,3.
	 */
	public void run() {
		ZProjector zProjectionTmp = new ZProjector(this.zProjection);
		this.zProjection = projectionMax(zProjectionTmp);
		adjustContrast(0.3);
		for (String boxCoordinate : this.boxCoordinates) {
			String[] splitLine = boxCoordinate.split("\\t");
			String[] fileName  = splitLine[0].split(Pattern.quote(File.separator));
			String[] name      = fileName[fileName.length - 1].split("_");
			System.out.println(boxCoordinate + "\n" +
			                   splitLine[0] + "\n" +
			                   Integer.parseInt(name[name.length - 2]));
			addBoxCropToZProjection(boxCoordinate, Integer.parseInt(name[name.length - 2]));
		}
		String outFileZBox = this.outputDirPath + File.separator +
		                     "zprojection" + File.separator +
		                     outputFilesPrefix + "_Zprojection.tif";
		System.out.println("outFileZBox " + outFileZBox);
		saveFile(this.zProjection, outFileZBox);
	}
	
	
	/**
	 * Save the ImagePlus Z-projection image
	 *
	 * @param imagePlusInput image to save
	 * @param pathFile       path to save the image
	 */
	public void saveFile(ImagePlus imagePlusInput, String pathFile) {
		FileSaver fileSaver = new FileSaver(imagePlusInput);
		fileSaver.saveAsTiff(pathFile);
	}
	
	
	/**
	 * Method to project 3D stack to 2D images using Max method projection.
	 *
	 * @param project Raw data
	 *
	 * @return Z projection
	 */
	private ImagePlus projectionMax(ZProjector project) {
		project.setMethod(1);
		project.doProjection();
		return project.getProjection();
	}
	
	
	/**
	 * Draw box from coordinate in the Z projection image and add the crop number.
	 *
	 * @param coordinateList List of coordinate of the current box of nucleus crop
	 * @param boxNumber      Number of the crop in the list (used in the output of nucleus crop)
	 */
	private void addBoxCropToZProjection(String coordinateList, int boxNumber) {
		String[] currentBox = coordinateList.split("\t");
		/* withBox calculation */
		
		int withBox = Math.abs(Integer.parseInt(currentBox[2])) - Math.abs(Integer.parseInt(currentBox[1]));
		/* heightBox calculation */
		int heightBox = Math.abs(Integer.parseInt(currentBox[4])) - Math.abs(Integer.parseInt(currentBox[3]));
		/* Line size parameter */
		IJ.setForegroundColor(0, 0, 0);
		IJ.run("Line Width...", "line=4");
		/* Set draw current box*/
		this.zProjection.setRoi(Integer.parseInt(currentBox[1]),
		                        Integer.parseInt(currentBox[3]),
		                        withBox, heightBox);
		IJ.run(this.zProjection, "Draw", "stack");
		
		
		/* Calculation of the coordinate to add nuclei Number */
		int xBorder = Integer.parseInt(currentBox[1]) - 100;
		int yBorder = Integer.parseInt(currentBox[3]) +
		              ((Integer.parseInt(currentBox[4]) - Integer.parseInt(currentBox[3])) / 2) - 20;
		// When the box is in left border the number need
		if (xBorder <= 40) {
			// to be write on the right of the box
			xBorder = Integer.parseInt(currentBox[2]) + 60;
		}
		Font font = new Font("Arial", Font.PLAIN, 30);
		TextRoi left = new TextRoi(xBorder,
		                           yBorder,
		                           Integer.toString(boxNumber),
		                           font);
		this.zProjection.setRoi(left);
		/* Draw the nucleus number aside the box */
		IJ.run(this.zProjection, "Draw", "stack");
	}
	
	
	private void addBadCropBoxToZProjection(String coordinateList, int boxNumber) {
		String[] currentBox = coordinateList.split("\t");
		/* withBox calculation */
		
		int withBox = Math.abs(Integer.parseInt(currentBox[2])) - Math.abs(Integer.parseInt(currentBox[1]));
		/* heightBox calculation */
		int heightBox = Math.abs(Integer.parseInt(currentBox[4])) - Math.abs(Integer.parseInt(currentBox[3]));
		/* Line size parameter */
		
		/* !!!!!!!!!!! on contrast la projection sinon elle est en GRIS ?????? */
		//IJ.run(this.zProjection, "Enhance Contrast", "saturated=0.35");
		//IJ.run(this.zProjection, "RGB Color", "");
		IJ.setForegroundColor(255, 0, 0);
		IJ.run("Line Width...", "line=4");
		/* Set draw current box*/
		this.zProjection.setRoi(Integer.parseInt(currentBox[1]),
		                        Integer.parseInt(currentBox[3]),
		                        withBox, heightBox);
		IJ.run(this.zProjection, "Draw", "stack");
		
		
		/* Calculation of the coordinate to add nuclei Number */
		int xBorder = Integer.parseInt(currentBox[1]) - 100;
		int yBorder = Integer.parseInt(currentBox[3]) +
		              ((Integer.parseInt(currentBox[4]) - Integer.parseInt(currentBox[3])) / 2) - 20;
		// When the box is in left border the number need
		if (xBorder <= 40) {
			// to be write on the right of the box
			xBorder = Integer.parseInt(currentBox[2]) + 60;
		}
		Font font = new Font("Arial", Font.PLAIN, 30);
		
		IJ.run("Colors...", "foreground=red");
		TextRoi left = new TextRoi(xBorder,
		                           yBorder,
		                           Integer.toString(boxNumber),
		                           font);
		this.zProjection.setRoi(left);
		/* Draw the nucleus number aside the box */
		IJ.run(this.zProjection, "Draw", "stack");
	}
	
	
	/**
	 * Method to Contrast the images values and invert the LUT.
	 *
	 * @param contrast Double number for contrast
	 */
	private void adjustContrast(double contrast) {
		IJ.run(this.zProjection, "Enhance Contrast...", "saturated=" + contrast);
		IJ.run(this.zProjection, "Invert LUT", "");
	}
	
}
