package gred.nucleus.cli;

import gred.nucleus.mains.Version;
import org.apache.commons.cli.*;


/** Generic class to handle command line option */
public class CLIActionOptions {
	
	/** NucleusJ version */
	private static final String NJ_version    = Version.get();
	/** Path to input folder */
	public               Option m_inputFolder = Option.builder("in")
	                                                  .longOpt("input")
	                                                  .required()
	                                                  .type(String.class)
	                                                  .numberOfArgs(1)
	                                                  .build();
	/** Path to config file */
	public               Option m_configFile  = Option.builder("c")
	                                                  .longOpt("config")
	                                                  .type(String.class)
	                                                  .desc("Path to config file\n" +
	                                                        "To generate config file example in current folder:\n" +
	                                                        "java -jar NucleusJ_2-" +
	                                                        NJ_version +
	                                                        ".jar -h configFileExample")
	                                                  .numberOfArgs(1)
	                                                  .build();
	/** List of available actions */
	public               Option m_action      = Option.builder("a")
	                                                  .longOpt("action")
	                                                  .required()
	                                                  .type(String.class)
	                                                  .desc("Action available:\n" +
	                                                        "autocrop : crop wide field images\n" +
	                                                        "segmentation : nucleus segmentation\n")
	                                                  .numberOfArgs(1)
	                                                  .build();
	/** OMERO activate */
	public               Option m_omero       = Option.builder("ome")
	                                                  .longOpt("omero")
	                                                  .type(boolean.class)
	                                                  .desc("Use of NucleusJ2.0 in OMERO\n")
	                                                  .build();
	/** List of options */
	Options           m_options = new Options();
	/** Command line */
	CommandLine       m_cmd;
	/** Command line parser */
	CommandLineParser m_parser  = new DefaultParser();
	
	
	/**
	 * Constructor with argument
	 *
	 * @param argument   List of command line argument
	 */
	public CLIActionOptions(String[] argument) {
		this.m_options.addOption(this.m_inputFolder);
		this.m_options.addOption(this.m_configFile);
		this.m_options.addOption(this.m_action);
		this.m_options.addOption(this.m_omero);
		try {
			this.m_cmd = this.m_parser.parse(this.m_options, argument, true);
		} catch (ParseException exp) {
			System.out.println(exp.getMessage() + "\n");
			System.out.println(getHelperInfo());
			System.exit(1);
		}
	}
	
	
	/** @return : helper info */
	public String getHelperInfo() {
		return "More details for available actions:\n" +
		       "java -jar NucleusJ_2-" + NJ_version + ".jar -h \n" +
		       "java -jar NucleusJ_2-" + NJ_version + ".jar -help \n\n" +
		       "More details for a specific action:\n" +
		       "java -jar NucleusJ_2-" + NJ_version + ".jar -h <action>\n" +
		       "java -jar NucleusJ_2-" + NJ_version + ".jar -help <action>";
	}
	
	
	/** @return list of options */
	public Options getOptions() {
		return this.m_options;
	}
	
	
	public CommandLine getCmd() {
		return this.m_cmd;
	}
	
}