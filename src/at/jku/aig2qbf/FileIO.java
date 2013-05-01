package at.jku.aig2qbf;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import at.jku.aig2qbf.parser.Parser;

public class FileIO {
	public enum FileExtension {
		AIG, AAG, QDIMACS
	}
	
	public static FileExtension GetFileExtension(String filename) {
		int i = filename.lastIndexOf('.');

		if (i == -1) {
			return null;
		}

		String extension = filename.substring(i + 1).toLowerCase();

		if (extension.compareTo("aag") == 0) {
			return FileExtension.AAG;
		}
		else if (extension.compareTo("aig") == 0) {
			return FileExtension.AIG;
		}
		else if (extension.compareTo("qdimacs") == 0 || extension.compareTo("qbf") == 0) {
			return FileExtension.QDIMACS;
		}
		else {
			return null;
		}
	}
	
	public static Parser GetParserForFileExtension(FileExtension extension) {
		switch (extension) {
			case AAG:
				return new at.jku.aig2qbf.parser.AAG();
			case AIG:
				return new at.jku.aig2qbf.parser.AIG();
			default:
				throw new RuntimeException(String.format("Parser for extension \"%s\" not implemented", extension));
		}
	}
	
	public static String ReadFile(String inputFilePath) {
		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFilePath)));

			StringBuilder builder = new StringBuilder();

			String line = "";
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append("\n");
			}

			return builder.toString();
		}
		catch (Exception e) {
			fail(e.toString());
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (IOException e) {

				}
			}
		}

		return "";
	}
	
	public static List<byte[]> ReadBinaryFile(String inputFilePath) {
		File inputFile = new File(inputFilePath);
		
		DataInputStream inputStream = null;

		try {
			byte[] fileData = new byte[(int) inputFile.length()];

			inputStream = new DataInputStream(new FileInputStream(inputFile));
			inputStream.readFully(fileData);

			List<byte[]> resultList = new ArrayList<byte[]>();

			int startIndex = 0;
			for (int i = 0; i < fileData.length; i++) {
				if (fileData[i] == (int) '\n') {
					resultList.add(Arrays.copyOfRange(fileData, startIndex, i));
					startIndex = i + 1;
				}
			}

			if (startIndex < fileData.length) {
				resultList.add(Arrays.copyOfRange(fileData, startIndex, fileData.length));
			}

			return resultList;
		}
		catch (Exception e) {
			throw new RuntimeException("Unable to read the input file. Please check the file rights and try it again.");
		}
		finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static boolean WriteFile(String filepath, String content) {
		File outputFile = new File(filepath);

		if (outputFile.exists()) {
			outputFile.delete();
		}

		File parentDirectory = outputFile.getParentFile();

		if (parentDirectory != null && ! parentDirectory.exists()) {
			parentDirectory.mkdirs();
		}

		// Write to file
		BufferedWriter writer = null;

		try {
			writer = new BufferedWriter(new FileWriter(filepath));
			writer.write(content);

			return true;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (writer != null) {
				try {
					writer.close();
				}
				catch (IOException e) {

				}
			}
		}

		return false;
	}
	
	public static void RemoveFile(String outputFilePath) {
		File tempFile = new File(outputFilePath);

		if (tempFile.exists()) {
			tempFile.delete();
		}
	}
}
