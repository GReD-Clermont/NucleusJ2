package gred.nucleus.plugins;

import fr.igred.omero.Client;
import fr.igred.omero.exception.AccessException;
import fr.igred.omero.exception.OMEROServerError;
import fr.igred.omero.exception.ServiceException;
import fr.igred.omero.repository.DatasetWrapper;
import fr.igred.omero.repository.ImageWrapper;
import fr.igred.omero.roi.ROIWrapper;
import gred.nucleus.autocrop.CropFromCoordinates;
import gred.nucleus.dialogs.CropFromCoordinatesDialog;
import gred.nucleus.files.FilesNames;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.io.FileSaver;
import ij.plugin.PlugIn;
import loci.formats.FormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class CropFromCoordinates_ implements PlugIn {
	/**
	 * Logger
	 */
	private DatasetWrapper toCropDataset;
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	CropFromCoordinatesDialog cropFromCoordinatesDialog;
	
	
	public static void cropFromCoordinates(String coordinateDir) throws IOException, FormatException {
		
		CropFromCoordinates test = new CropFromCoordinates(coordinateDir);
		test.runCropFromCoordinate();
	}
	
	
	@Override
	public void run(String arg) {
		if (IJ.versionLessThan("1.32c")) {
			return;
		}
		cropFromCoordinatesDialog = new CropFromCoordinatesDialog();
		while (cropFromCoordinatesDialog.isShowing()) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				LOGGER.error("An error occurred.", e);
			}
		}
		
		if (cropFromCoordinatesDialog.isStart()) {
			if (cropFromCoordinatesDialog.isOmeroEnabled()) {
				
				try {
					runOMERO();
				} catch (AccessException e) {
					throw new RuntimeException(e);
				} catch (ServiceException e) {
					throw new RuntimeException(e);
				} catch (ExecutionException e) {
					throw new RuntimeException(e);
				}
			} else {
				String file = cropFromCoordinatesDialog.getLink();
				if (file == null || file.equals("")) {
					IJ.error("Input file or directory is missing");
				} else {
					try {
						LOGGER.info("Begin Autocrop from coordinate process ");
						
						cropFromCoordinates(file);
						
						LOGGER.info("Autocrop from coordinate process has ended successfully");
					} catch (Exception e) {
						LOGGER.info("Autocrop from coordinate process has failed");
						LOGGER.error("An error occurred.", e);
					}
				}
			}
		}
	}
	
	public Client checkOMEROConnection(String hostname,
	                                   String port,
	                                   String username,
	                                   char[] password,
	                                   String group) {
		Client client = new Client();
		try {
			client.connect(hostname,
			               Integer.parseInt(port),
			               username,
			               password,
			               Long.valueOf(group));
		} catch (Exception exp) {
			IJ.error("Invalid connection values");
			return null;
		}
		return client;
	}
	
	private void cropImageFromOMERO(Client client, ImageWrapper image,ImageWrapper imageToCrop, DatasetWrapper outputDataset)
	throws AccessException, ServiceException, ExecutionException, IOException, OMEROServerError {
		List<ROIWrapper> rois = image.getROIs(client);
		for (ROIWrapper roi : rois) {
			// Get the roi names
			String ROIName = roi.getName();
			// Get the name of the Image To Crop
			String imageToCropName = imageToCrop.getName();
			// Get the image to crop
			ImagePlus imp       = imageToCrop.toImagePlus(client, roi);
			// Save Crop File
			FileSaver fileSaver = new FileSaver(imp);
			String sortie = fileSaver.toString();
			// Save the crop as TIF
			fileSaver.saveAsTiff(sortie);
			// generate a temporary file
			String         resultPath       = sortie;
			File           resultFile       = new File(resultPath);
			// Remove file extension
			FilesNames outPutFilesNames = new FilesNames(imageToCropName);
			String     prefix           = outPutFilesNames.prefixNameFile();
			// Rename the temporary file same as toCrop Image name
			File toCropNewName = new File(prefix+"_"+ROIName);
			resultFile.renameTo(toCropNewName);
			String toCropFile = toCropNewName.toString();
			// Import Cropped Image to the Dataset
			outputDataset.importImages(client, toCropFile);
			// Delete temp file
			Files.deleteIfExists(toCropNewName.toPath());
		}
	}
	
	public void runOMERO() throws AccessException, ServiceException, ExecutionException {
		// Check connection
		String hostname = cropFromCoordinatesDialog.getHostname();
		String port     = cropFromCoordinatesDialog.getPort();
		String username = cropFromCoordinatesDialog.getUsername();
		String password = cropFromCoordinatesDialog.getPassword();
		String group    = cropFromCoordinatesDialog.getGroup();
		String output   = cropFromCoordinatesDialog.getOutputProject();
		
		Prefs.set("omero.host", hostname);
		Prefs.set("omero.port", port);
		Prefs.set("omero.user", username);
		
		Client client   = checkOMEROConnection(hostname, port, username, password.toCharArray(), group);
		
		// Handle the source according to the type given
		String sourceDataType = cropFromCoordinatesDialog.getDataType();
		String ToCropdataType = cropFromCoordinatesDialog.getDataType();
		Long   inputID  = Long.valueOf(cropFromCoordinatesDialog.getSourceID());
		Long   inputToCropID  = Long.valueOf(cropFromCoordinatesDialog.getToCropID());
		DatasetWrapper outputds = client.getDataset(Long.parseLong(output));
		
		try {
			if (sourceDataType.equals("Image") && ToCropdataType.equals("Image") ) {
				ImageWrapper image      = client.getImage(inputID);
				ImageWrapper imageToCrop      = client.getImage(inputToCropID);
			
				try {
					LOGGER.info("Begin Autocrop from coordinate process ");
					cropImageFromOMERO(client, image, imageToCrop, outputds); // Run cropFromCoordinates
					LOGGER.info("Autocrop from coordinate process has ended successfully");
				} catch (Exception e) {
					LOGGER.info("Autocrop from coordinate process has failed");
					LOGGER.error("An error occurred.", e);
				}
				
			} else {
				List<ImageWrapper> sourceImages = null;
				List<ImageWrapper> toCropImages = null;
				String             sourceImageName   = "";
				ImageWrapper sourceImage;
				if (sourceDataType.equals("Dataset") && ToCropdataType.equals("Dataset")) {
					
					DatasetWrapper sourceDataset = client.getDataset(inputID);
					toCropDataset = client.getDataset(inputToCropID);
					sourceImages = sourceDataset.getImages(client);
					
				} else if (sourceDataType.equals("Tag")) {
					sourceImages = client.getImagesTagged(inputID);
				}
				try {
					LOGGER.info("Begin Autocrop from coordinate process ");
					for (int i =0; i< sourceImages.size(); i++) {
						
						sourceImage = sourceImages.get(i) ;
						sourceImageName = sourceImage.getName();
						toCropImages = toCropDataset.getImages(client,sourceImageName);
						if(!toCropImages.isEmpty()){
							ImageWrapper toCropImage= toCropImages.get(0);
							cropImageFromOMERO(client, sourceImage,toCropImage, outputds); // Run cropFromCoordinates
						}
					}
					LOGGER.info("Autocrop from coordinate process has ended successfully");
				} catch (Exception e) {
					LOGGER.info("Autocrop from coordinate process has failed");
					LOGGER.error("An error occurred.", e);
				}
			}
		} catch (ServiceException se) {
			IJ.error("Unable to access to OMERO service");
		} catch (AccessException ae) {
			IJ.error("Cannot access " + sourceDataType + "with ID = " + inputID + ".");
		} catch (Exception e) {
			LOGGER.error("An error occurred.", e);
		}
	}
	
}
