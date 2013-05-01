package at.jku.aig2qbf.test.formatter;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import at.jku.aig2qbf.component.Component;
import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.test.BaseTest;

public class TestAAG extends BaseTest {

	@Before
	public void setUp() throws Exception {
		Component.Reset();
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
			Tree t1 = this.loadTreeFromFile("input/basic/" + filename + ".aag");
			assertTrue("load " + filename, t1 != null);

			assertTrue("save " + filename, this.saveTreeTofile("output/" + filename + ".aag", t1));

			Tree t2 = this.loadTreeFromFile("output/" + filename + ".aag");
			assertTrue("load saved " + filename, t2 != null);
		}
	}
}
