package gred.nucleus.filesInputOutput;

import ij.IJ;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;


/** Class get to list directory and sub directory. */
public class Directory {
	
	/** Directory path */
	public File            m_directory;
	/** Directory path */
	public String          m_dirPath       = "";
	/** List of files in current folder + recursive folder */
	public ArrayList<File> m_fileList      = new ArrayList<>();
	/** Check if directory contain nd files */
	public Boolean         m_containNdFile = false;
	/** List of nd files */
	public ArrayList<File> m_fileListND    = new ArrayList<>();
	/** Path separator */
	public String          m_separator;
	
	
	/**
	 * Constructor
	 *
	 * @param Path of directory
	 */
	public Directory(String Path) {
		try {
			this.m_dirPath = Path;
			this.m_directory = new File(this.m_dirPath);
			this.m_separator = File.separator;
		} catch (Exception exp) {
			System.out.println(exp.getMessage());
			System.exit(1);
		}
	}
	
	
	/** Method to check if directory and create if doesn't */
	public void CheckAndCreateDir() {
		ChekSeparatorEndPath();
		CreateDir();
	}
	
	
	/** Check if separator exist */
	private void ChekSeparatorEndPath() {
		if (!(this.m_dirPath.endsWith(File.separator))) {
			this.m_dirPath += File.separator;
		}
	}
	
	
	/** Method creating folder if doesn't exist. */
	private void CreateDir() {
		File dir = new File(this.m_dirPath);
		if (!dir.exists()) {
			boolean isDirCreated = dir.mkdirs();
			if (isDirCreated) {
				IJ.log("New directory : " + this.m_dirPath);
			} else {
				IJ.error(this.m_dirPath + " : directory cannot be created");
				System.exit(-1);
			}
		}
	}
	
	
	/** @return path current directory */
	public String getDirPath() {
		return this.m_dirPath;
	}
	
	
	/**
	 * Method to recursively list files contains in folder and sub folder. (Argument needed because of recursive way)
	 *
	 * @param Path path of folder
	 */
	public void listImageFiles(String Path) {
		File   root = new File(Path);
		File[] list = root.listFiles();
		if (list == null) {
			IJ.error(Path + " does not contain files");
			System.exit(-1);
		}
		for (File f : list) {
			if (f.isDirectory()) {
				
				listImageFiles(f.getAbsolutePath());
			} else {
				if (!(FilenameUtils.getExtension(f.getName()).equals("txt"))) {
					this.m_fileList.add(f);
					if (FilenameUtils.getExtension(f.getName()).equals("nd")) {
						this.m_containNdFile = true;
						this.m_fileListND.add(f);
					}
				}
			}
		}
	}
	
	
	public void listAllFiles(String Path) {
		File   root = new File(Path);
		File[] list = root.listFiles();
		
		if (list != null) {
			for (File f : list) {
				this.m_fileList.add(f);
				if (f.isDirectory()) {
					listAllFiles(f.getAbsolutePath());
				}
			}
		}
	}
	
	
	/** Replace list files if ND files have been listed. */
	public void checkAndActualiseNDFiles() {
		if (this.m_containNdFile) {
			this.m_fileList = this.m_fileListND;
		}
	}
	
	
	/** check if input directory is empty */
	public void checkIfEmpty() {
		if (this.m_fileList.isEmpty()) {
			System.err.println("Folder " + this.m_dirPath + " is empty");
		}
	}
	
	
	/** @return list of files */
	public ArrayList<File> ListFiles() {
		return this.m_fileList;
	}
	
	
	/**
	 * @param index of file in list array
	 *
	 * @return File
	 */
	public File getFile(int index) {
		return this.m_fileList.get(index);
	}
	
	
	/** @return number of file listed */
	public int getNumberFiles() {
		return this.m_fileList.size();
	}
	
	
	public String getSeparator() {
		return this.m_separator;
	}
	
	
	/**
	 * Searches a file in a list file without extension. Used to compare 2 lists of files
	 */
	public File searchFileNameWithoutExtension(String fileName) {
		File fileToReturn = null;
		
		for (File f : this.m_fileList) {
			if (f.getName().substring(0, f.getName().lastIndexOf('.')).equals(fileName)) {
				fileToReturn = f;
			}
		}
		return fileToReturn;
	}
	
	
	public boolean checkIfFileExists(String fileName) {
		boolean fileExists = false;
		
		for (File f : this.m_fileList) {
			if ((f.getName().substring(0, f.getName().lastIndexOf('.')).equals(fileName))
			    || (f.getName().equals(fileName))) {
				fileExists = true;
			}
		}
		return fileExists;
	}
	
}