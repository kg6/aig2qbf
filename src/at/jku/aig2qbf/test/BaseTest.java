package at.jku.aig2qbf.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;

import at.jku.aig2qbf.FileIO;
import at.jku.aig2qbf.FileIO.FileExtension;
import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.formatter.Formatter;
import at.jku.aig2qbf.formatter.QDIMACS;
import at.jku.aig2qbf.parser.Parser;

public class BaseTest {
	protected final String SAT_SOLVER_PATH = "./tools/depqbf";
	
	protected Tree loadTreeFromFile(String filePath) {
		Parser parser = BaseTest.GetInputFileParser(new File(filePath));
		
		return parser.parse(filePath);
	}

	protected boolean saveTreeTofile(String filePath, Tree tree) {
		Formatter formatter = BaseTest.GetOutputFileFormatter(new File(filePath));
		
		return FileIO.WriteFile(filePath, formatter.format(tree));
	}

	protected boolean checkSatisfiablity(String outputFilePath, Tree tree) {
		Formatter formatter = new QDIMACS();

		if(!FileIO.WriteFile(outputFilePath, formatter.format(tree))) {
			fail("Unable to write SAT temporary file");
			return false;
		}

		return checkSatisfiablity(outputFilePath);
	}
	
	protected boolean checkSatisfiablity(String filepath) {
		Process process = null;
		BufferedReader inputStream = null;
		File tempFile = null;

		try {
			process = Runtime.getRuntime().exec(SAT_SOLVER_PATH + " " + filepath);
			inputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String result = inputStream.readLine().toUpperCase();

			if (result.compareTo("SAT") == 0) {
				return true;
			}
			else if (result.compareTo("UNSAT") == 0) {
				return false;
			}

			throw new RuntimeException(String.format("SAT solver has returned an unexpected result: %s", result));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (tempFile != null) {
				tempFile.delete();
			}
		}

		return false;
	}
	
	protected File[] getBenchmarkInputFiles(String directoryPath, FilenameFilter fileNameFilter) {
		File sequentialDirectory = new File(directoryPath);

		return sequentialDirectory.listFiles(fileNameFilter);
	}
	
	protected boolean convertToAiger(String inputFilePath, String outputFilePath) {
		try {
			ProcessBuilder processBuilder = new ProcessBuilder("./tools/cnf2aig", inputFilePath, outputFilePath);
			processBuilder.directory(new File("").getAbsoluteFile());
			
			Process process = processBuilder.start();
			process.waitFor();
			
			return process.exitValue() == 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	protected boolean checkOriginalSat(File inputFile, int k) {
		BufferedReader inputReader = null;
		BufferedReader errorReader = null;

		try {
			ProcessBuilder processBuilder = new ProcessBuilder("./tools/mcaiger", "-r", Integer.toString(k), inputFile.getPath());
			processBuilder.directory(new File("").getAbsoluteFile());

			Process process = processBuilder.start();
			process.waitFor();

			inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String output = inputReader.readLine();

			errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			String error = errorReader.readLine();

			if (error != null && error.length() > 0) {
				throw new RuntimeException("MCAiger has returned an error: " + error);
			}

			if (output.compareTo("1") == 0) {
				return true;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (inputReader != null) {
				try {
					inputReader.close();
				}
				catch (IOException e) {

				}
			}

			if (errorReader != null) {
				try {
					errorReader.close();
				}
				catch (IOException e) {

				}
			}
		}

		return false;
	}
	
	protected void testComponent(Component c, String name, int inputSize, int outputSize, Component parent) {
		assertEquals(name, c.getName());
		assertEquals(inputSize, c.inputs.size());
		assertEquals(outputSize, c.outputs.size());

		if (parent != null) {
			assertEquals(parent, c.outputs.get(0));
		}
	}
	
	public static Parser GetInputFileParser(File inputFile) {
		String fileName = inputFile.getName();
		String fileExtension = fileName.substring(fileName.lastIndexOf("."));

		if (fileName.endsWith(".aag")) {
			return GetInputFileParser(FileExtension.AAG);
		}
		else if (fileName.endsWith(".aig")) {
			return GetInputFileParser(FileExtension.AIG);
		}
		else {
			throw new RuntimeException(String.format("Unable to create input file parser: File extension '%s' is unknown!", fileExtension));
		}
	}
	
	public static Parser GetInputFileParser(FileExtension extension) {
		switch(extension) {
			case AAG:
				return new at.jku.aig2qbf.parser.AAG();
			case AIG:
				return new at.jku.aig2qbf.parser.AIG();
			default:
				throw new RuntimeException(String.format("Unable to create input file parser: %s parser is not implemented", extension));
		}
	}
	
	public static Formatter GetOutputFileFormatter(File outputFile) {
		String fileName = outputFile.getName();
		String fileExtension = fileName.substring(fileName.lastIndexOf("."));

		if (fileName.endsWith(".qdimacs") || fileName.endsWith(".qbf")) {
			return GetOutputFileFormatter(FileExtension.QDIMACS);
		}
		else if (fileName.endsWith(".aag")) {
			return GetOutputFileFormatter(FileExtension.AAG);
		}
		else {
			throw new RuntimeException(String.format("Unable to create output file formatter: File extension '%s' is unknown!", fileExtension));
		}
	}
	
	public static Formatter GetOutputFileFormatter(FileExtension extension) {
		switch(extension) {
			case QDIMACS:
				new at.jku.aig2qbf.formatter.QDIMACS();
			case AAG:
				return new at.jku.aig2qbf.formatter.AAG();
			default:
				throw new RuntimeException(String.format("Unable to create output file formatter: %s formatter is not implemented", extension));
		}
	}
}
