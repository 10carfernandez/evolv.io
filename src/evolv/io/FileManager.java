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
	String folder;
	String[] modes = { "manualImgs", "autoImgs", "manualTexts", "autoTexts" };
	
	public FileManager(EvolvioColor evolvioColor, String initialFileName){
		this.evolvioColor = evolvioColor;
		folder = initialFileName;
		fileSaveCounts = new int[4];
		fileSaveTimes = new double[4];
		for (int i = 0; i < 4; i++) {
			fileSaveCounts[i] = 0;
			fileSaveTimes[i] = -999;
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
	
	public String getNextFileName(int type) {		
		String ending = ".png";
		if (type >= 2) {
			ending = ".txt";
		}
		return folder + "/" + modes[type] + "/" + EvolvioColor.nf(fileSaveCounts[type], 5) + ending;
	}
	
	// Create the four folders under the new folder where all information will be saved
	public void setNewSaveDirectory(String filePath){		
		folder = evolvioColor.INITIAL_FILE_NAME;
		String fullPath;
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
		fileSaveTimes[type] = -999999;
	}
	
	// Save entire board or image of frame
	void fileSave(Board evoBoard) {		
		for (int i = 0; i < 4; i++) {
			if (fileSaveTimes[i] < -99999) {
				fileSaveTimes[i] = year;
				if (i < 2) {
					evoBoard.evolvioColor.saveFrame(getNextFileName(i));
				} else {
					try{
						ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(getNextFileName(i)));
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
