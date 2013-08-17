package at.jku.aig2qbf.test.reduction;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FilenameFilter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.jku.aig2qbf.Util;
import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.parser.Parser;
import at.jku.aig2qbf.reduction.SimplePathReduction;
import at.jku.aig2qbf.reduction.TreeReduction;
import at.jku.aig2qbf.test.BaseTest;

public class TestCompetition extends BaseTest {
	private final String INPUT_EXTENSION_CNF = ".cnf";
	private final String AIGER_FILE = "./output/aiger.aig";

	private final String INPUT_COMPETITION_DIRECTORY = "./input/competition/2009";
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
		Util.RemoveFile(OUTPUT_FILE);
		Util.RemoveFile(AIGER_FILE);
	}

	@Test
	public void testCompetition() {
		File[] benchmarkFiles = this.getBenchmarkInputFiles(INPUT_COMPETITION_DIRECTORY, new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(INPUT_EXTENSION_CNF);
			}
		});

		for (int k = 1; k <= MAX_K; k++) {
			System.out.println(String.format("Checking k=%s", k));

			for (File input : benchmarkFiles) {
				System.out.println(String.format("	Checking file %s", input.getName()));

				for (TreeReduction reduction : reductionMethods) {
					final long startTime = System.currentTimeMillis();

					System.out.println(String.format("		Applying reduction method %s", reduction.toString()));

					assertTrue(testCompetition(input, reduction, k));

					System.out.println(String.format("			runtime: %sms", System.currentTimeMillis() - startTime));
				}

				Component.Reset();
			}
		}
	}

	private boolean testCompetition(File input, TreeReduction reductionMethod, int k) {
		File aigerFile = new File(AIGER_FILE);

		if (!this.convertToAiger(input.getAbsolutePath(), aigerFile.getAbsolutePath()) || !aigerFile.exists()) {
			System.out.println("			Failed to convert CNF to AIG!");
			return false;
		}

		Parser parser = Util.GetParser(aigerFile);

		Tree tree = parser.parse(aigerFile.getAbsolutePath());

		Tree unrolledTree = tree.unroll(k);
		unrolledTree.mergeToOneOutput();

		Tree reducedTree = reductionMethod.reduceTree(unrolledTree, k);
		Tree tseitinTree = reducedTree.toTseitinCNF();

		final boolean currentSat = this.checkSatisfiablity(OUTPUT_FILE, tseitinTree);
		final boolean originalSat = this.checkOriginalSat(aigerFile, k);

		return currentSat == originalSat;
	}
}
