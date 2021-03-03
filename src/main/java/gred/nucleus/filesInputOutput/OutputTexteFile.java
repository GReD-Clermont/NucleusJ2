package gred.nucleus.filesInputOutput;

import ij.IJ;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;


public class OutputTexteFile extends FilesNames {
	
	public OutputTexteFile(String filePath) {
		super(filePath);
	}
	
	
	/**
	 * Method to save file with verification if file already exists
	 * <p> TODO(@DesTristus) ADD ERROR IN LOG FILE
	 */
	public void saveTextFile(String text) {
		try {
			BufferedWriter writer;
			writer = new BufferedWriter(new FileWriter(this._fullPathFile));
			writer.write(text);
			writer.close();

/*            if (!fileExist()) {
                BufferedWriter writer;
                writer = new BufferedWriter(new FileWriter(new File(this._fullPathFile)));
                writer.write(text);
                writer.close();
            }*/
		} catch (IOException e) {
			IJ.log("\n" + this._fullPathFile + " creation failed");
			e.printStackTrace();
		}
		IJ.log("\n" + this._fullPathFile + " created");
	}
	
}