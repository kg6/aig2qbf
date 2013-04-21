package at.jku.aig2qbf.test.formatter;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.Tree;

public class TestAAG {

	@Before
	public void setUp() throws Exception {
		Component.Reset();
	}

	protected Tree loadFile(String filename) {
		return new at.jku.aig2qbf.parser.AAG().parse("input/basic/" + filename + ".aag");
	}

	protected boolean saveFile(Tree tree, String filename) {
		return new at.jku.aig2qbf.formatter.AAG().writeToFile(tree, "output/" + filename + ".aag");
	}

	protected Tree loadOutputFile(String filename) {
		return new at.jku.aig2qbf.parser.AAG().parse("output/" + filename + ".aag");
	}

	@Test
	public void straightLoadSaveLoad() {
		for (String filename : new String[] {
			"empty",
			"false",
			"true",
			"buffer",
			"inverter",
			"and",
			"flip",
			"with-comment",
			"and-with-all-nots",
			"and-with-true-and-false",
			"toggle",
			"toggle-re"
		}) {
			Tree t1 = loadFile(filename);
			assertTrue("load " + filename, t1 != null);

			assertTrue("save " + filename, saveFile(t1, filename));

			Tree t2 = loadOutputFile(filename);
			assertTrue("load saved " + filename, t2 != null);
		}
	}
}
