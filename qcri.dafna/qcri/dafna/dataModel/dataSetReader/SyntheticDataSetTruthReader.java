package qcri.dafna.dataModel.dataSetReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;

import au.com.bytecode.opencsv.CSVReader;
import qcri.dafna.dataModel.data.Globals;
import qcri.dafna.dataModel.quality.dataQuality.DataItemMeasures;

public class SyntheticDataSetTruthReader extends TruthReader {
/*
 * File format
 * object Id \t propertyName \t value
 */
	public SyntheticDataSetTruthReader(HashMap<String, DataItemMeasures> dataItemMeasures) {
		super(dataItemMeasures);
	}
	public int readDirectoryFiles(String directory) {
		trueValueCount = 0;
		try {
			DirectoryStream<Path> directoryStream;

			directoryStream = Files.newDirectoryStream(Paths.get(directory));
			for (Path readFilePath : directoryStream) {
				processFileLines(readFilePath, "null");
			}
			directoryStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return trueValueCount;
	}
	private void processFileLines(Path filePath, String timeStamp) {
		
		try (Scanner scanner = new Scanner(filePath, Globals.Flight_DataSet_FILE_ENCODING.name())) {
			if (scanner.hasNextLine()) {
				if (scanner.nextLine().contains("\t")) {
					processFileLinesOldFormat(filePath, timeStamp);
				} else {
					readCSVFile(filePath.toString(), Globals.delimiterText.charAt(0));
				}
			}
			scanner.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readCSVFile(String filePath, char delim) {
		// line format:
		// object Id \t propertyName \t value
		try {
			CSVReader reader = new CSVReader(new FileReader(filePath), delim);

			//read line by line
			String[] record = null;

			while((record = reader.readNext()) != null){
				String objectID = record[0];
				String propertyName = record[1];
				String value = record[2];
				addTrueValue("", objectID, propertyName, value);
			    trueValueCount++;
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void processFileLinesOldFormat(Path filePath, String timeStamp) {
		try (Scanner scanner = new Scanner(filePath, Globals.Flight_DataSet_FILE_ENCODING.name())) {
			while (scanner.hasNextLine()) {
				processLine(scanner.nextLine(), timeStamp);
			}
			scanner.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void processLine(String fileLine, String timeStamp) {
		// line format:
		// object Id \t propertyName \t value
		Scanner scanner = new Scanner(fileLine);
		scanner.useDelimiter("\t");
		try {
			String objectID = scanner.next();
			String propertyName = scanner.next();
			String value = scanner.next();
			addTrueValue("", objectID, propertyName, value);
		} catch (NoSuchElementException e) {
			System.out.println("Wrong Line Format : " + fileLine + " . ");
		}
		scanner.close();
	}
}
