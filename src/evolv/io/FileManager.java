package evolv.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class FileManager implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3970782842142170878L;

	EvolvioColor evolvioColor;
	double year;
	
	// Saving
	int[] fileSaveCounts;
	double[] fileSaveTimes;
	double imageSaveInterval = 32;
	double textSaveInterval = 32;
	String savePath;
	String[] modes = { "manualImgs", "autoImgs", "manual", "auto" };
	boolean[] isFileSavePrepared = {false, true, false, true}; // Don't let manualImgs and manualTexts save automatically
	
	public FileManager(EvolvioColor evolvioColor, String initialFileName){
		this.evolvioColor = evolvioColor;
		savePath = initialFileName;
		fileSaveCounts = new int[modes.length];
		fileSaveTimes = new double[modes.length];
		for (int i = 0; i < modes.length; i++) {
			fileSaveCounts[i] = 0;
		}
	}
	
	public Board fileLoad(String filename){
		Board evoBoard = null;
		try {
			FileInputStream fileInputStream = new FileInputStream(filename);
			ObjectInputStream reader = new ObjectInputStream(fileInputStream);
			evoBoard = (Board) reader.readObject();
			reader.close();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}		
		return evoBoard;
	}
	
	public String getNextFileName(int type, boolean fullPath) {
		String ending = ".png";
		if (type >= 2) {
			ending = ".evolve";
		}
		if(fullPath){
			return savePath + "/" + modes[type] + "/" + EvolvioColor.nf(fileSaveCounts[type], 5) + ending;
		} else {
			return evolvioColor.saveDir + "/" + modes[type] + "/" + EvolvioColor.nf(fileSaveCounts[type], 5) + ending;
		}
	}

	public String getNextFileName(int type) {
		return getNextFileName(type, true);
	}
	
	// Create the four folders under the new folder where all information will be saved
	public void setSaveDirectory(String filePath){
		savePath = evolvioColor.savePath;
		String fullPath;
		// Set folder
		File subFolder = new File(filePath);
		if (!subFolder.exists()){
			try{
				subFolder.mkdir();
			} catch(Exception e) {
			    e.printStackTrace();
			}
		}
		// Set four folders
		for (int i = 0; i < modes.length; i++) {
			fullPath = filePath + "/" + modes[i];	
			File f = new File(fullPath);
			if (!f.exists()){
				try{
					f.mkdir();
				} catch(Exception e) {
				    e.printStackTrace();
				}
			} 
		}		
	}
	
	void prepareForFileSave(int type) {
		isFileSavePrepared[type] = true;
	}

	// Save entire board or image of frame
	void fileSave(Board evoBoard) {		
		for (int i = 0; i < 4; i++) {
			if (isFileSavePrepared[i]) {
				fileSaveTimes[i] = year;
				isFileSavePrepared[i] = false;
				if (i < 2) {
					evoBoard.evolvioColor.saveFrame(getNextFileName(i));
				} else {
					try{
						String fileName = getNextFileName(i);
						ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));
						out.writeObject(evoBoard);
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				this.fileSaveCounts[i]++;
			}
		}
	}
}
