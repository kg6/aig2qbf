package at.jku.aig2qbf.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import at.jku.aig2qbf.aig2qbf;
import at.jku.aig2qbf.visualizer.TreeVisualizer;

public class TestMain {
	private final String TEMP_QDIMACS_FILE = "./output/temp.qbf";

	private Random random;

	@Before
	public void setUp() throws Exception {
		this.random = new Random();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		TreeVisualizer.CLOSE_ON_EXIT = true;

		List<ParameterPair> parameterList = new ArrayList<ParameterPair>();
		parameterList.add(new ParameterPair("-h"));
		parameterList.add(new ParameterPair("-k", "5"));
		parameterList.add(new ParameterPair("-i", "input/basic/and.aag"));
		parameterList.add(new ParameterPair("-it", "aag"));
		parameterList.add(new ParameterPair("-nr"));
		parameterList.add(new ParameterPair("-nu"));
		parameterList.add(new ParameterPair("-o", TEMP_QDIMACS_FILE));
		parameterList.add(new ParameterPair("-ot", "qbf"));
		parameterList.add(new ParameterPair("-v"));
		// parameterList.add(new ParameterPair("-vis"));
		parameterList.add(new ParameterPair("-ws"));

		final int max_iterations = 100;

		for (int i = 0; i < max_iterations; i++) {
			final int parameterCount = this.random.nextInt(parameterList.size());

			List<String> selectedParameterList = new ArrayList<String>();

			for (int j = 0; j < parameterCount; j++) {
				ParameterPair parameterPair = parameterList.get(this.random.nextInt(parameterList.size()));

				selectedParameterList.add(parameterPair.key);

				if (parameterPair.value != null) {
					selectedParameterList.add(parameterPair.value);
				}
			}

			String[] selectedParameters = new String[selectedParameterList.size()];
			selectedParameters = selectedParameterList.toArray(selectedParameters);

			printSelectedParameters(selectedParameters);

			aig2qbf.main(selectedParameters);
		}
	}

	private void printSelectedParameters(String[] parameters) {
		System.out.print("Selected parameters: ");

		for (int i = 0; i < parameters.length; i++) {
			System.out.print(parameters[i]);
			System.out.print(" ");
		}

		System.out.println();
	}

	private class ParameterPair {
		String key;
		String value;

		public ParameterPair(String key) {
			this.key = key;
			this.value = null;
		}

		public ParameterPair(String key, String value) {
			this.key = key;
			this.value = value;
		}
	}
}
