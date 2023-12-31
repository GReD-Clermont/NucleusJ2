package gred.nucleus.cli;

import fr.igred.omero.Client;
import fr.igred.omero.repository.DatasetWrapper;
import fr.igred.omero.repository.ImageWrapper;
import fr.igred.omero.repository.ProjectWrapper;
import gred.nucleus.autocrop.AutoCropCalling;
import gred.nucleus.autocrop.AutocropParameters;
import gred.nucleus.segmentation.SegmentationCalling;
import gred.nucleus.segmentation.SegmentationParameters;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Console;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;


public class CLIRunActionOMERO {
	/** Logger */
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	/** List of options */
	Options     options = new Options();
	/** Command line */
	CommandLine cmd;
	/** OMERO client information see fr.igred.omero.Client */
	Client      client  = new Client();
	
	/** OMERO password connection */
	char[] mdp;
	
	/** OMERO type of data to analyse : image data dataset tag */
	String dataType;
	
	
	public CLIRunActionOMERO(CommandLine cmd) {
		this.cmd = cmd;
		getOMEROPassword();
	}
	
	
	public void run() throws Exception {
		checkOMEROConnection();
		switch (this.cmd.getOptionValue("action")) {
			case "autocrop":
				runAutoCropOMERO();
				break;
			case "segmentation":
				runSegmentationOMERO();
				break;
			default:
				throw new IllegalArgumentException("Invalid action");
		}
		this.client.disconnect();
	}
	
	
	public static void autoCropOMERO(String inputDirectory,
	                                 String outputDirectory,
	                                 Client client,
	                                 AutoCropCalling autoCrop) throws Exception {
		String[] param = inputDirectory.split("/");
		
		if (param.length >= 2) {
			Long id = Long.parseLong(param[1]);
			if (param[0].equals("image")) {
				ImageWrapper image = client.getImage(id);
				
				int sizeC = image.getPixels().getSizeC();
				
				Long[] outputsDat = new Long[sizeC];
				
				for (int i = 0; i < sizeC; i++) {
					DatasetWrapper dataset = new DatasetWrapper("C" + i + "_" + image.getName(), "");
					outputsDat[i] =
							client.getProject(Long.parseLong(outputDirectory)).addDataset(client, dataset).getId();
				}
				
				autoCrop.runImageOMERO(image, outputsDat, client);
				autoCrop.saveGeneralInfoOmero(client, outputsDat);
			} else {
				List<ImageWrapper> images;
				
				String name = "";
				
				if (param[0].equals("dataset")) {
					DatasetWrapper dataset = client.getDataset(id);
					
					name = dataset.getName();
					
					if (param.length == 4 && param[2].equals("tag")) {
						images = dataset.getImagesTagged(client, Long.parseLong(param[3]));
					} else {
						images = dataset.getImages(client);
					}
				} else if (param[0].equals("tag")) {
					images = client.getImagesTagged(id);
				} else {
					throw new IllegalArgumentException();
				}
				
				int sizeC = images.get(0).getPixels().getSizeC();
				
				Long[] outputsDat = new Long[sizeC];
				
				for (int i = 0; i < sizeC; i++) {
					DatasetWrapper dataset = new DatasetWrapper("raw_C" + i + "_" + name, "");
					outputsDat[i] =
							client.getProject(Long.parseLong(outputDirectory)).addDataset(client, dataset).getId();
				}
				
				autoCrop.runSeveralImageOMERO(images, outputsDat, client);
			}
		} else {
			throw new IllegalArgumentException("Wrong input parameter : "
			                                   + inputDirectory + "\n\n\n"
			                                   + "Example format expected:\n"
			                                   + "dataset/OMERO_ID \n");
		}
	}
	
	
	public void getOMEROPassword() {
		if (this.cmd.hasOption("password")) {
			this.mdp = this.cmd.getOptionValue("password").toCharArray();
		} else {
			System.console().writer().println("Enter password: ");
			Console con = System.console();
			this.mdp = con.readPassword();
		}
	}
	
	
	public void checkOMEROConnection() {
		try {
			client.connect(this.cmd.getOptionValue("hostname"),
			               Integer.parseInt(this.cmd.getOptionValue("port")),
			               this.cmd.getOptionValue("username"),
			               this.mdp,
			               Long.valueOf(this.cmd.getOptionValue("group")));
		} catch (Exception exp) {
			LOGGER.error("OMERO connection error: " + exp.getMessage(), exp);
			System.exit(1);
		}
	}
	
	
	public void runAutoCropOMERO() throws Exception {
		AutocropParameters autocropParameters = new AutocropParameters(".", ".");
		if (this.cmd.hasOption("config")) {
			autocropParameters.addGeneralProperties(this.cmd.getOptionValue("config"));
			autocropParameters.addProperties(this.cmd.getOptionValue("config"));
		}
		AutoCropCalling autoCrop = new AutoCropCalling(autocropParameters);
		try {
			autoCropOMERO(this.cmd.getOptionValue("input"),
			              this.cmd.getOptionValue("output"),
			              this.client,
			              autoCrop);
		} catch (IllegalArgumentException exp) {
			LOGGER.error(exp.getMessage(), exp);
			System.exit(1);
		}
	}
	
	
	public void runSegmentationOMERO() throws Exception {
		SegmentationParameters segmentationParameters = new SegmentationParameters(".", ".");
		if (this.cmd.hasOption("config")) {
			segmentationParameters.addGeneralProperties(this.cmd.getOptionValue("config"));
			segmentationParameters.addProperties(this.cmd.getOptionValue("config"));
		}
		SegmentationCalling otsuModified = new SegmentationCalling(segmentationParameters);
		segmentationOMERO(this.cmd.getOptionValue("input"),
		                  this.cmd.getOptionValue("output"),
		                  this.client,
		                  otsuModified);
	}
	
	
	public void segmentationOMERO(String inputDirectory,
	                              String outputDirectory,
	                              Client client,
	                              SegmentationCalling otsuModified) throws Exception {
		String[] param = inputDirectory.split("/");
		
		if (param.length >= 2) {
			Long id = Long.parseLong(param[1]);
			if (param[0].equals("image")) {
				ImageWrapper image = client.getImage(id);
				
				try {
					String log;
					if (param.length == 3 && param[2].equals("ROI")) {
						log = otsuModified.runOneImageOMERObyROIs(image, Long.parseLong(outputDirectory), client);
					} else {
						log = otsuModified.runOneImageOMERO(image, Long.parseLong(outputDirectory), client);
					}
					otsuModified.saveCropGeneralInfoOmero(client, Long.parseLong(outputDirectory));
					if (!(log.equals(""))) {
						LOGGER.error("Nuclei which didn't pass the segmentation\n{}", log);
					}
				} catch (IOException e) {
					LOGGER.error("An error occurred.", e);
				}
			} else {
				List<ImageWrapper> images;
				
				switch (param[0]) {
					case "dataset":
						DatasetWrapper dataset = client.getDataset(id);
						
						if (param.length == 4 && param[2].equals("tag")) {
							images = dataset.getImagesTagged(client, Long.parseLong(param[3]));
						} else {
							images = dataset.getImages(client);
						}
						break;
					case "project":
						ProjectWrapper project = client.getProject(id);
						
						if (param.length == 4 && param[2].equals("tag")) {
							images = project.getImagesTagged(client, Long.parseLong(param[3]));
						} else {
							images = project.getImages(client);
						}
						break;
					case "tag":
						images = client.getImagesTagged(id);
						break;
					default:
						throw new IllegalArgumentException();
				}
				try {
					String log;
					if ((param.length == 3 && param[2].equals("ROI")) ||
					    (param.length == 5 && param[4].equals("ROI"))) {
						log = otsuModified.runSeveralImagesOMERObyROIs(images, Long.parseLong(outputDirectory), client);
					} else {
						log = otsuModified.runSeveralImagesOMERO(images, Long.parseLong(outputDirectory), client);
					}
					if (!(log.equals(""))) {
						LOGGER.error("Nuclei which didn't pass the segmentation\n{}", log);
					}
				} catch (IOException e) {
					LOGGER.error("An error occurred.", e);
				}
			}
		} else {
			throw new IllegalArgumentException();
		}
	}
	
}
