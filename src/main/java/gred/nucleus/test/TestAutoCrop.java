package gred.nucleus.test;

import gred.nucleus.mainsNucelusJ.AutoCropCalling;
import loci.formats.FormatException;

import java.io.IOException;
import java.util.ArrayList;


/**
 * Class dedicated to examples and test of methods in the package.
 * 
 * @author Remy Malgouyres, Tristan Dubos and Axel Poulet
 */
public class TestAutoCrop {
	
	/**
	 * Test for labeling connected components of a binarized image.
	 * Only connected components with no voxel on the image's boundary
	 * are kept in the filtering process.
	 * 
	 * Connected components with a volume below some threshold are
	 * also removed.
	 * 
	 * a constant random gray level is set on each connected component.
	 * 
	 * @param imageSourceFile the input image file on disk 
	 */
	
	static ArrayList <String> m_test;
	
	public static void testStupid(String imageSourceFile, String output) throws IOException, FormatException {
        AutoCropCalling autoCrop = new AutoCropCalling(imageSourceFile,output);
        autoCrop.run();
	}


	/**
	 * Main function of the package's tests.
	 * @param args
	 */
	public static void main(String[] args) throws IOException, FormatException {

	    System.err.println("start prog");
		long maxMemory = Runtime.getRuntime().maxMemory();
		/* Maximum amount of memory the JVM will attempt to use */
		System.out.println("Maximum memory (bytes): " +
				(maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory*1e-9));
		String inputOneImageAxel = "/home/plop/Bureau/image/wideField/Z_c1c4_cot11&12&13-_w11 DAPI SIM variable_s4.TIF";
        String inputDirAxel = "/home/plop/Bureau/image/wideField/";
        String outputAxel = "/home/plop/Bureau/image/wideField/test";

        String inputOneImageTristan = "/home/tridubos/Bureau/TestJar_autocrop/raw/";
        //String inputOneImageTristan = "/home/tridubos/Bureau/AUTOCROP_TEST/raw/Z_c1c4_cot11&12&13-_w11 DAPI SIM variable_s9.TIF";
        String inputDirTristan = "/home/tridubos/Bureau/Bille_4Micro_02-2019/AutocropDuSchnaps/";
        String outputTristan = "/home/tridubos/Bureau/TestJar_autocrop/out/";

        //testStupid(inputOneImageTristan, outputTristan);

		System.err.println("The program ended normally.");

		System.out.println("Total memory (bytes): " +
				Runtime.getRuntime().totalMemory()*1e-9);
	}

}
