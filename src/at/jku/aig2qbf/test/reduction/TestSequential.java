package at.jku.aig2qbf.test.reduction;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.parser.AAG;
import at.jku.aig2qbf.parser.AIG;
import at.jku.aig2qbf.parser.Parser;
import at.jku.aig2qbf.reduction.SimplePathReduction;
import at.jku.aig2qbf.reduction.TreeReduction;
import at.jku.aig2qbf.test.TestUtil;

public class TestSequential {
	private final String INPUT_EXTENSION_AIG = ".aig";
	private final String INPUT_EXTENSION_AAG = ".aag";

	private final String INPUT_SEQUENTIAL_DIRECTORY = "./input/sequential";
	private final String OUTPUT_FILE = "./output/temp.qbf";

	private final int MAX_K = 10;

	TreeReduction[] reductionMethods = new TreeReduction[] {
		new SimplePathReduction()
	};

	@Before
	public void setUp() throws Exception {
		Component.Reset();
	}

	@After
	public void tearDown() throws Exception {
		TestUtil.RemoveOutputFile(OUTPUT_FILE);
	}

	@Test
	public void test() {
		File[] benchmarkFiles = getSequentialBenchmarkFiles();

		List<String> blackList = new ArrayList<String>();

		for (int k = 1; k <= MAX_K; k++) {
			System.out.println(String.format("Checking k=%s", k));

			for (File input : benchmarkFiles) {
				System.out.println(String.format("	Checking file %s", input.getName()));

				if (blackList.contains(input.getName())) {
					System.out.println("		Skipped");
					continue;
				}

				for (TreeReduction reduction : reductionMethods) {
					final long startTime = System.currentTimeMillis();

					System.out.println(String.format("		Applying reduction method %s", reduction.toString()));

					assertTrue(testBenchmark(input, reduction, k));

					System.out.println(String.format("			runtime: %sms", System.currentTimeMillis() - startTime));
				}

				Component.Reset();
			}
		}
	}

	private boolean testBenchmark(File inputFile, TreeReduction reductionMethod, int k) {
		Parser parser = getParser(inputFile);

		Tree tree = parser.parse(inputFile.getAbsolutePath());

		Tree reducedTree = reductionMethod.reduceTree(tree, k);

		boolean currentSat = TestUtil.CheckSatisfiablity(reducedTree, OUTPUT_FILE);
		boolean originalSat = checkOriginalSat(inputFile, k);

		if (originalSat != currentSat) {
			File outputFile = new File(OUTPUT_FILE);

			if (! outputFile.exists()) {
				throw new RuntimeException("Unable to reduce the output file: File was not found.");
			}

			// Call the delta debugger
			minifyAigerFile(outputFile);

			return false;
		}

		return true;
	}

	private boolean checkOriginalSat(File inputFile, int k) {
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

	private void minifyAigerFile(File outputFile) {
		// TODO Run delta debugger...
	}

	private Parser getParser(File inputFile) {
		String fileName = inputFile.getName();
		String fileExtension = fileName.substring(fileName.lastIndexOf("."));

		if (fileName.endsWith(INPUT_EXTENSION_AAG)) {
			return new AAG();
		}
		else if (fileName.endsWith(INPUT_EXTENSION_AIG)) {
			return new AIG();
		}
		else {
			throw new RuntimeException(String.format("Unable to run sequential test: Unknown file extension '%s'", fileExtension));
		}
	}

	private File[] getSequentialBenchmarkFiles() {
		File sequentialDirectory = new File(INPUT_SEQUENTIAL_DIRECTORY);

		return sequentialDirectory.listFiles(new InputFileFilter());
	}

	private class InputFileFilter implements FilenameFilter {

		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(INPUT_EXTENSION_AAG) || name.endsWith(INPUT_EXTENSION_AIG);
		}

	}
}
