package at.jku.aig2qbf.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.jku.aig2qbf.aig2qbf;
import at.jku.aig2qbf.visualizer.TreeVisualizer;

public class TestMain {
	private final String TEMP_QDIMACS_FILE = "./output/temp.qbf";

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		TreeVisualizer.CLOSE_ON_EXIT = true;
		
		aig2qbf.main(new String[] { "-h" });
		
		aig2qbf.main(new String[] { "-k", "5", "-i", "input/basic/and.aag", "-it", "aag", "-nr", "-ns", "-nu", "-o", TEMP_QDIMACS_FILE, "-ot", "qbf", "-v" });
		
		aig2qbf.main(new String[] { "-k", "1", "-i", "input/basic/and.aag", "-nu", "-o", TEMP_QDIMACS_FILE, "-v", "-vis" });
		
		aig2qbf.main(new String[] { "-k", "9", "-i", "input/basic/and.aag", "-o", TEMP_QDIMACS_FILE, "-vis" });
	}

}
