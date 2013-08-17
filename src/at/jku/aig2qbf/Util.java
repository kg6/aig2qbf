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
import java.util.HashMap;
import java.util.List;

import at.jku.aig2qbf.formatter.Formatter;
import at.jku.aig2qbf.parser.Parser;

public class Util {
	public enum FileExtension {
		AIG, AAG, QDIMACS
	}

	@SuppressWarnings("serial")
	public static HashMap<String, FileExtension> INPUT_FORMAT_TYPES = new HashMap<String, FileExtension>() {
		{
			put(".aag", FileExtension.AAG);
			put(".aig", FileExtension.AIG);
		}
	};
	@SuppressWarnings("serial")
	public static HashMap<String, FileExtension> OUTPUT_FORMAT_TYPES = new HashMap<String, FileExtension>() {
		{
			put(".aag", FileExtension.AAG);
			put(".qbf", FileExtension.QDIMACS);
			put(".qdimacs", FileExtension.QDIMACS);
		}
	};

	public static FileExtension GetFileExtension(String filename) {
		int i = filename.lastIndexOf('.');

		if (i == -1) {
			return null;
		}

		String extension = filename.substring(i).toLowerCase();

		if (INPUT_FORMAT_TYPES.containsKey(extension)) {
			return INPUT_FORMAT_TYPES.get(extension);
		}
		else if (OUTPUT_FORMAT_TYPES.containsKey(extension)) {
			return OUTPUT_FORMAT_TYPES.get(extension);
		}
		else {
			return null;
		}
	}

	public static Formatter GetFormatter(File file) {
		return GetFormatter(file.getName());
	}

	public static Formatter GetFormatter(String fileName) {
		for (String s : OUTPUT_FORMAT_TYPES.keySet()) {
			if (fileName.endsWith(s)) {
				return GetFormatter(OUTPUT_FORMAT_TYPES.get(s));
			}
		}

		throw new RuntimeException(String.format("Unable to create output file formatter: File extension '%s' is unknown!", fileName.substring(fileName.lastIndexOf("."))));
	}

	public static Formatter GetFormatter(FileExtension extension) {
		switch (extension) {
			case AAG:
				return new at.jku.aig2qbf.formatter.AAG();
			case QDIMACS:
				return new at.jku.aig2qbf.formatter.QDIMACS();
			default:
				throw new RuntimeException(String.format("Unable to create output file formatter: %s formatter is not implemented", extension));
		}
	}

	public static Parser GetParser(File file) {
		return GetParser(file.getName());
	}

	public static Parser GetParser(String fileName) {
		for (String s : INPUT_FORMAT_TYPES.keySet()) {
			if (fileName.endsWith(s)) {
				return GetParser(INPUT_FORMAT_TYPES.get(s));
			}
		}

		throw new RuntimeException(String.format("Unable to create input file parser: File extension '%s' is unknown!", fileName.substring(fileName.lastIndexOf("."))));
	}

	public static Parser GetParser(FileExtension extension) {
		switch (extension) {
			case AAG:
				return new at.jku.aig2qbf.parser.AAG();
			case AIG:
				return new at.jku.aig2qbf.parser.AIG();
			default:
				throw new RuntimeException(String.format("Unable to create input file parser: %s parser is not implemented", extension));
		}
	}

	public static String Join(String delimiter, Object[] list) {
		StringBuilder s = new StringBuilder((String) list[0]);

		for (int i = 1; i < list.length; i++) {
			s.append(delimiter);
			s.append(list[i]);
		}

		return s.toString();
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

	public static void RemoveFile(String outputFilePath) {
		File tempFile = new File(outputFilePath);

		if (tempFile.exists()) {
			tempFile.delete();
		}
	}

	public static boolean WriteFile(String filepath, String content) {
		File outputFile = new File(filepath);

		if (outputFile.exists()) {
			outputFile.delete();
		}

		File parentDirectory = outputFile.getParentFile();

		if (parentDirectory != null && !parentDirectory.exists()) {
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
}
