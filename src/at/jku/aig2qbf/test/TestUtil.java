package at.jku.aig2qbf.test;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;

import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.formatter.QDIMACS;
import at.jku.aig2qbf.parser.AAG;
import at.jku.aig2qbf.parser.AIG;
import at.jku.aig2qbf.parser.Parser;

public class TestUtil {
	private static final String SAT_SOLVER_PATH = "./tools/depqbf";

	public static boolean CheckSatisfiablity(Tree tree, String outputFilePath) {
		QDIMACS q = new QDIMACS();

		if (! q.writeToFile(tree, outputFilePath)) {
			fail("Unable to write SAT temporary file");
			return false;
		}

		return CheckSatisfiablity(outputFilePath);
	}
	
	public static boolean CheckSatisfiablity(String filepath) {
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

	public static String ReadQDIMACSFile(String inputFilePath) {
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

	public static void RemoveOutputFile(String outputFilePath) {
		File tempFile = new File(outputFilePath);

		if (tempFile.exists()) {
			tempFile.delete();
		}
	}
	
	public static File[] GetBenchmarkInputFiles(String directoryPath, FilenameFilter fileNameFilter) {
		File sequentialDirectory = new File(directoryPath);

		return sequentialDirectory.listFiles(fileNameFilter);
	}
	
	public static boolean ConvertToAiger(String inputFilePath, String outputFilePath) {
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
	
	public static boolean CheckOriginalSat(File inputFile, int k) {
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
	
	public static Parser GetInputFileParser(File inputFile) {
		String fileName = inputFile.getName();
		String fileExtension = fileName.substring(fileName.lastIndexOf("."));

		if (fileName.endsWith(".aag")) {
			return new AAG();
		}
		else if (fileName.endsWith(".aig")) {
			return new AIG();
		}
		else {
			throw new RuntimeException(String.format("Unable to run sequential test: Unknown file extension '%s'", fileExtension));
		}
	}
}
