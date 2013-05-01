package at.jku.aig2qbf.test.reduction;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.jku.aig2qbf.FileIO;
import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.Tree;
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
		FileIO.RemoveFile(OUTPUT_FILE);
	}

	@Test
	public void test() {
		File[] benchmarkFiles = TestUtil.GetBenchmarkInputFiles(INPUT_SEQUENTIAL_DIRECTORY, new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(INPUT_EXTENSION_AAG) || name.endsWith(INPUT_EXTENSION_AIG);
			}
		});

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
		Parser parser = TestUtil.GetInputFileParser(inputFile);

		Tree tree = parser.parse(inputFile.getAbsolutePath());
		
		tree = tree.unroll(k);
		tree.mergeToOneOutput();
		
		Tree unrolledTree = tree.unroll(k);
		unrolledTree.mergeToOneOutput();
		
		Tree reducedTree = reductionMethod.reduceTree(unrolledTree, k);
		
		Tree tseitinTree = reducedTree.toTseitinCNF();

		final boolean currentSat = TestUtil.CheckSatisfiablity(OUTPUT_FILE, tseitinTree);
		final boolean originalSat = TestUtil.CheckOriginalSat(inputFile, k);

		return currentSat == originalSat;
	}
}
